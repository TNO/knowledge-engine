package eu.knowledge.engine.reasoner.util;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.rulesys.ClauseEntry;
import org.apache.jena.reasoner.rulesys.Node_RuleVariable;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.FmtUtils;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class JenaRules {

	private static SerializationContext context = new SerializationContext(false);

	/**
	 * Create rules that return (part of) the turtle facts. Maybe in the future
	 * differentiate between:
	 * <ul>
	 * <li>{@code ?s rdf:type owl:Class}
	 * <li>{@code ?s rdf:type owl:Property}
	 * <li>{@code ...}</li>
	 * </ul>
	 * 
	 * @param turtleSource
	 * @return
	 */
	public static Set<BaseRule> createFactRulesFromTurtle(Reader turtleSource) {

		var rules = new HashSet<BaseRule>();

		Model m = createJenaModelFromTurtle(turtleSource);

		StmtIterator iter = m.listStatements();

		assert m.size() < Integer.MAX_VALUE : "Cannot load more than " + Integer.MAX_VALUE + " statements.";
		int nrOfStatements = (int) m.size();
		String[] columns = new String[] { "s", "p", "o" };
		String[] rows = new String[nrOfStatements];

		int idx = 0;
		while (iter.hasNext()) {
			Statement s = iter.next();
			rows[idx] = FmtUtils.stringForNode(s.getSubject().asNode(), context) + ","
					+ FmtUtils.stringForNode(s.getPredicate().asNode(), context) + ","
					+ FmtUtils.stringForNode(s.getObject().asNode(), context);
			idx++;
		}

		Table t = new Table(columns, rows);
		var r = new Rule(new HashSet<TriplePattern>(),
				new HashSet<TriplePattern>(Arrays.asList(new TriplePattern("?s ?p ?o"))), new DataBindingSetHandler(t));
		rules.add(r);

		return rules;
	}

	private static Model createJenaModelFromTurtle(Reader turtleSource) {
		Model m = ModelFactory.createDefaultModel();
		m.read(turtleSource, null, "turtle");
		return m;
	}

	/**
	 * Convert a given turtle string into a string that follows the
	 * <a href="https://jena.apache.org/documentation/inference/#RULEsyntax">Apache
	 * Jena Rules specification</a>. This syntax is used to load two kinds of domain
	 * knowledge into the knowledge engine: facts and rules. Note that the facts are
	 * actually rules without an antecedent and only a consequent
	 * ({@code (if/antecedent) -> (then/consequent) .}), which can be read as
	 * meaning "if no particular condition holds, then the following holds".
	 * 
	 * Every fact present in the turtle file will be converted to the following
	 * format:
	 * 
	 * <pre>
	 * {@code
	 * -> ( <subject> <predicate> <object> ) .
	 * }
	 * </pre>
	 * 
	 * Note that literals with language tags are not supported.
	 * 
	 * @param turtleSource The turtle string that contains the facts.
	 * @return A string of fact rules that follow the Apache Jena Rules syntax and
	 *         can be loaded into the knowledge engine as domain knowledge.
	 */
	public static String createApacheJenaRulesFromTurtle(Reader turtleSource) {
		StringBuilder sb = new StringBuilder();

		Model m = createJenaModelFromTurtle(turtleSource);
		StmtIterator iter = m.listStatements();

		while (iter.hasNext()) {
			Statement s = iter.next();
			sb.append("-> (").append(FmtUtils.stringForNode(s.getSubject().asNode(), context)).append(" ")
					.append(FmtUtils.stringForNode(s.getPredicate().asNode(), context)).append(" ")
					.append(FmtUtils.stringForNode(s.getObject().asNode(), context)).append(") .").append("\n");
		}

		return sb.toString();
	}

	/**
	 * Converts a string with Apache Jena Rules that can contain both facts and
	 * rules into KE rules that can be used in the reasoner.
	 * 
	 * Note that all fact rules (i.e. rules without an antecedent and only a
	 * consequent) are collected and created into a single data rule suitable for
	 * the reasoner.
	 * 
	 * @param someRules A string of rules in the Apache Jena Rules syntax.
	 * @return A set of rules that can be used with the KE reasoner.
	 */
	public static Set<BaseRule> convertJenaToKeRules(String someRules) {
		List<org.apache.jena.reasoner.rulesys.Rule> jenaRules = org.apache.jena.reasoner.rulesys.Rule
				.parseRules(someRules);

		List<String> stringRows = new ArrayList<>();

		Set<BaseRule> keRules = new HashSet<>();
		for (org.apache.jena.reasoner.rulesys.Rule r : jenaRules) {

			Set<TriplePattern> antecedent = new HashSet<>();
			for (ClauseEntry ce : r.getBody()) {
				antecedent.add(convertToTriplePattern(((org.apache.jena.reasoner.TriplePattern) ce)));
			}

			Set<TriplePattern> consequent = new HashSet<>();
			for (ClauseEntry ce : r.getHead()) {
				consequent.add(convertToTriplePattern(((org.apache.jena.reasoner.TriplePattern) ce)));
			}

			if (antecedent.isEmpty()) {
				// create KE data rule from consequent

				assert consequent.size() == 1;

				TriplePattern tp = consequent.iterator().next();

				String row = FmtUtils.stringForNode(tp.getSubject(), context) + ","
						+ FmtUtils.stringForNode(tp.getPredicate(), context) + ","
						+ FmtUtils.stringForNode(tp.getObject(), context);

				stringRows.add(row);

			} else if (consequent.isEmpty()) {
				throw new IllegalArgumentException("Rule '" + r.toShortString() + "' should have a consequent.");
			} else {
				// create normal rule
				keRules.add(new Rule(antecedent, consequent));
			}
		}

		String[] columns = new String[] { "s", "p", "o" };
		String[] rows = stringRows.toArray(new String[stringRows.size()]);

		Table t = new Table(columns, rows);

		keRules.add(new Rule(new HashSet<TriplePattern>(),
				new HashSet<TriplePattern>(Arrays.asList(new TriplePattern("?s ?p ?o"))),
				new DataBindingSetHandler(t)));

		return keRules;
	}

	private static TriplePattern convertToTriplePattern(org.apache.jena.reasoner.TriplePattern triple) {

		Node subject = triple.getSubject();
		Node predicate = triple.getPredicate();
		Node object = triple.getObject();

		if (subject instanceof Node_RuleVariable)
			subject = Var.alloc(subject.getName().substring(1));

		if (predicate instanceof Node_RuleVariable)
			predicate = Var.alloc(predicate.getName().substring(1));

		if (object instanceof Node_RuleVariable)
			object = Var.alloc(object.getName().substring(1));

		return new TriplePattern(subject, predicate, object);
	}
}
