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

		Model m = ModelFactory.createDefaultModel();
		m.read(turtleSource, null, "turtle");

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

}
