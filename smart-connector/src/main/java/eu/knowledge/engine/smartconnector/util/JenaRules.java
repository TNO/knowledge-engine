package eu.knowledge.engine.smartconnector.util;

import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.FmtUtils;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.util.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.util.Table;

public class JenaRules {

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
	public static Set<Rule> createFactRulesFromTurtle(Reader turtleSource) {

		var rules = new HashSet<Rule>();

		Model m = createJenaModelFromTurtle(turtleSource);

		StmtIterator iter = m.listStatements();

		assert m.size() < Integer.MAX_VALUE : "Cannot load more than " + Integer.MAX_VALUE + " statements.";
		int nrOfStatements = (int) m.size();
		String[] columns = new String[] { "s", "p", "o" };
		String[] rows = new String[nrOfStatements];

		SerializationContext context = new SerializationContext();
		context.setUsePlainLiterals(false);

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

		SerializationContext context = new SerializationContext();
		context.setUsePlainLiterals(false);

		while (iter.hasNext()) {
			Statement s = iter.next();
			sb.append("-> (").append(FmtUtils.stringForNode(s.getSubject().asNode(), context)).append(" ")
					.append(FmtUtils.stringForNode(s.getPredicate().asNode(), context)).append(" ")
					.append(FmtUtils.stringForNode(s.getObject().asNode(), context)).append(") .").append("\n");
		}

		return sb.toString();
	}
}
