package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.rulesys.ClauseEntry;
import org.apache.jena.reasoner.rulesys.Node_RuleVariable;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.Util;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

/**
 * Based on:
 * https://github.com/apache/jena/blob/main/jena-core/src/test/java/org/apache/jena/reasoner/rulesys/test/TestBasicLP.java
 */
public class JenaRuleTest {

	// Useful constants
	Node p = NodeFactory.createURI("p");
	Node q = NodeFactory.createURI("q");
	Node r = NodeFactory.createURI("r");
	Node s = NodeFactory.createURI("s");
	Node t = NodeFactory.createURI("t");
	Node u = NodeFactory.createURI("u");
	Node a = NodeFactory.createURI("a");
	Node b = NodeFactory.createURI("b");
	Node c = NodeFactory.createURI("c");
	Node d = NodeFactory.createURI("d");
	Node e = NodeFactory.createURI("e");
	Node C1 = NodeFactory.createURI("C1");
	Node C2 = NodeFactory.createURI("C2");
	Node C3 = NodeFactory.createURI("C3");
	Node C4 = NodeFactory.createURI("C4");
	Node D1 = NodeFactory.createURI("D1");
	Node D2 = NodeFactory.createURI("D2");
	Node D3 = NodeFactory.createURI("D3");
	Node sP = RDFS.Nodes.subPropertyOf;
	Node sC = RDFS.Nodes.subClassOf;
	Node ty = RDF.Nodes.type;

	/**
	 * Test basic rule operations - lookup, no matching rules
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBaseRules1() throws InterruptedException, ExecutionException, ParseException {
		doBasicTest("[r1: (?x r c) <- (?x p b)]", new TriplePattern("?s <p> <b>"), "s=<a>");
	}

	/**
	 * Test basic rule operations - simple chain rule
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBaseRules2() throws InterruptedException, ExecutionException, ParseException {
		doBasicTest("[r1: (?x r c) <- (?x p b)]", new TriplePattern("?s <r> <c>"), "s=<a>");
	}

	/**
	 * Test basic rule operations - chain rule with head unification
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBaseRules3() throws InterruptedException, ExecutionException, ParseException {
		doBasicTest("[r1: (?x r ?x) <- (?x p b)]", new TriplePattern("?s <r> <a>"), "s=<a>");
	}

	/**
	 * Test basic rule operations - rule with head unification, non-temp var
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBaseRules4() throws InterruptedException, ExecutionException, ParseException {
		doBasicTest("[r1: (?x r ?x) <- (?y p b), (?x p b)]", new TriplePattern("?s <r> <a>"), "s=<a>");
	}

	/**
	 * Test basic rule operations - simple cascade
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBaseRules5() throws InterruptedException, ExecutionException, ParseException {
		doBasicTest("[r1: (?x q ?y) <- (?x r ?y)(?y s ?x)]" + "[r2: (?x r ?y) <- (?x p ?y)]"
				+ "[r3: (?x s ?y) <- (?y p ?x)]", new TriplePattern("?s <q> ?o"), "s=<a>,o=<b>");
	}

	/**
	 * Test basic rule operations - chain rule which will fail at head time
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBaseRules6() throws InterruptedException, ExecutionException, ParseException {
		doBasicTest("[r1: (?x r ?x) <- (?x p b)]", new TriplePattern("<a> <r> <b>"), "");
	}

	/**
	 * Test basic rule operations - chain rule which will fail in search
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBaseRules7() throws InterruptedException, ExecutionException, ParseException {
		doBasicTest("[r1: (?x r ?y) <- (?x p c)]", new TriplePattern("<a> <r> <b>"), "");
	}

	/**
	 * Test basic rule operations - simple chain
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBaseRules8() throws InterruptedException, ExecutionException, ParseException {
		doBasicTest("[r1: (?x q ?y) <- (?x r ?y)]" + "[r2: (?x r ?y) <- (?x p ?y)]", new TriplePattern("?s <q> ?o"),
				"s=<a>,o=<b>");
	}

	/**
	 * Test basic rule operations - simple chain
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBaseRules9() throws InterruptedException, ExecutionException, ParseException {
		doBasicTest("[r1: (?x q ?y) <- (?x r ?y)]" + "[r2: (?x r ?y) <- (?y p ?x)]", new TriplePattern("?s <q> ?o"),
				"s=<b>,o=<a>");
	}

	/**
	 * Generic base test operation on a graph with the single triple (a, p, b)
	 * 
	 * @param ruleSrc the source of the rules
	 * @param query   the Triple to search for
	 * @param results the array of expected results
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void doBasicTest(String ruleSrc, TriplePattern query, String results)
			throws InterruptedException, ExecutionException, ParseException {

		List<org.apache.jena.reasoner.rulesys.Rule> jenaRules = org.apache.jena.reasoner.rulesys.Rule
				.parseRules(ruleSrc);

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
			keRules.add(new Rule(antecedent, consequent));
		}

		doTest(keRules, "<a>,<p>,<b>", query, results);
	}

	private void doTest(Set<BaseRule> keRules, String data, TriplePattern query, String results)
			throws InterruptedException, ExecutionException, ParseException {

		RuleStore rs = new RuleStore();
		rs.addRules(keRules);

		// data rule
		DataBindingSetHandler aBindingSetHandler = new DataBindingSetHandler(new Table(new String[] {
				// @formatter:off
						"s", "p", "o"
						// @formatter:on
		}, new String[] {
				// @formatter:off
				data
						// @formatter:on
		}));
		Rule r = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern("?s ?p ?o"))),
				aBindingSetHandler);
		rs.addRule(r);

		ProactiveRule startRule = new ProactiveRule(new HashSet<>(Arrays.asList(query)), new HashSet<>());
		rs.addRule(startRule);

		ReasonerPlan rp = new ReasonerPlan(rs, startRule);

		rs.printGraphVizCode(rp);

		BindingSet bs = new BindingSet();
		Binding b = new Binding();
		bs.add(b);

		TaskBoard tb;
		while ((tb = rp.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		BindingSet results2 = rp.getResults();
		Model m = Util.generateModel(query, results2);

		StmtIterator iter = m.listStatements();

		while (iter.hasNext()) {
			Statement st = iter.next();
			System.out.println(st);
		}

		BindingSet bindset = new BindingSet();
		Binding b2 = Util.toBinding(results);
		if (!b2.isEmpty())
			bindset.add(b2);

		assertEquals(bindset, results2);
	}

	private TriplePattern convertToTriplePattern(org.apache.jena.reasoner.TriplePattern triple) {

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
