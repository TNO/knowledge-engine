package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.junit.jupiter.api.Test;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.Util;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.reasoner.util.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.util.JenaRules;
import eu.knowledge.engine.reasoner.util.Table;

/**
 * Based on:
 * https://github.com/apache/jena/blob/main/jena-core/src/test/java/org/apache/jena/reasoner/rulesys/test/TestBasicLP.java
 */
public class JenaRuleTest {

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
		doBasicTest("[r1: (?x r ?x) <- (?x p b)]", new TriplePattern("<a> <r> <b>"), null);
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
		doBasicTest("[r1: (?x r ?y) <- (?x p c)]", new TriplePattern("<a> <r> <b>"), null);
	}

	/**
	 * Test basic rule operations - chain rule which will succeed in search
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBaseRules7_2() throws InterruptedException, ExecutionException, ParseException {
		doBasicTest("[r1: (?x r ?y) <- (?x p ?y)]", new TriplePattern("<a> <r> <b>"), "");
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
	 * Test backtracking - simple triple query.
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBacktrack1() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules("[r1: (?x r ?y) <- (?x p ?y)]"), "<a>,<p>,<b>|<a>,<p>,<c>|<a>,<p>,<d>",
				new TriplePattern("<a> <p> ?o"), "o=<b>|o=<c>|o=<d>");

	}

	/**
	 * Test backtracking - chain to simple triple query.
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBacktrack2() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules("[r1: (?x r ?y) <- (?x p ?y)]"), "<a>,<p>,<b>|<a>,<p>,<c>|<a>,<p>,<d>",
				new TriplePattern("<a> <r> ?o"), "o=<b>|o=<c>|o=<d>");
	}

	/**
	 * Test backtracking - simple choice point
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBacktrack3() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules(
				"[r1: (?x r C1) <- (?x p b)]" + "[r2: (?x r C2) <- (?x p b)]" + "[r3: (?x r C3) <- (?x p b)]"),
				"<a>,<p>,<b>", new TriplePattern("<a> <r> ?o"), "o=<C1>|o=<C2>|o=<C3>");
	}

	/**
	 * Test backtracking - nested choice point
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBacktrack4() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules("[r1: (?x r C1) <- (?x p b)]" + "[r2: (?x r C2) <- (?x p b)]"
				+ "[r3: (?x r C3) <- (?x p b)]" + "[r4: (?x s ?z) <- (?x p ?w), (?x r ?y) (?y p ?z)]"),
				"<a>,<p>,<b>|<C1>,<p>,<D1>|<C2>,<p>,<D2>|<C3>,<p>,<D3>", new TriplePattern("<a> <s> ?o"),
				"o=<D1>|o=<D2>|o=<D3>");
	}

	/**
	 * Test backtracking - nested choice point with multiple triple matches
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBacktrack5() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules(
				"[r1: (?x r C3) <- (C1 p ?x)]" + "[r2: (?x r C2) <- (C2 p ?x)]" + "[r4: (?x s ?y) <- (?x r ?y)]"),
				"<C1>,<p>,<D1>|<C1>,<p>,<a>|<C2>,<p>,<D2>|<C2>,<p>,<b>", new TriplePattern("?s <s> ?o"),
				"s=<D1>,o=<C3>|s=<a>,o=<C3>|s=<D2>,o=<C2>|s=<b>,o=<C2>");
	}

	/**
	 * Test backtracking - nested choice point with multiple triple matches, and
	 * checking temp v. permanent variable usage
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBacktrack6() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules(
				"[r1: (?x r C1) <- (?x p a)]" + "[r2: (?x r C2) <- (?x p b)]" + "[r3: (?x q C1) <- (?x p b)]"
						+ "[r4: (?x q C2) <- (?x p a)]" + "[r5: (?x s ?y) <- (?x r ?y) (?x q ?y)]"),
				"<D1>,<p>,<a>|<D2>,<p>,<a>|<D2>,<p>,<b>|<D3>,<p>,<b>", new TriplePattern("?s <s> ?o"),
				"s=<D2>,o=<C1>|s=<D2>,o=<C2>");
	}

	/**
	 * Test backtracking - nested choice point with simple triple matches
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBacktrack7() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules(
				"[r1: (?x r C1) <- (?x p b)]" + "[r2: (?x r C2) <- (?x p b)]" + "[r3: (?x r C3) <- (?x p b)]"
						+ "[r3: (?x r D1) <- (?x p b)]" + "[r4: (?x q C2) <- (?x p b)]" + "[r5: (?x q C3) <- (?x p b)]"
						+ "[r5: (?x q D1) <- (?x p b)]" + "[r6: (?x t C1) <- (?x p b)]" + "[r7: (?x t C2) <- (?x p b)]"
						+ "[r8: (?x t C3) <- (?x p b)]" + "[r9: (?x s ?y) <- (?x r ?y) (?x q ?y) (?x t ?y)]"),
				"<a>,<p>,<b>", new TriplePattern("?s <s> ?o"), "s=<a>,o=<C2>|s=<a>,o=<C3>");
	}

	/**
	 * Test backtracking - nested choice point with simple triple matches, permanent
	 * vars but used just once in body
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBacktrack8() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules(
				"[r1: (?x r C1) <- (?x p b)]" + "[r2: (?x r C2) <- (?x p b)]" + "[r3: (?x r C3) <- (?x p b)]"
						+ "[r3: (?x r D1) <- (?x p b)]" + "[r4: (?x q C2) <- (?x p b)]" + "[r5: (?x q C3) <- (?x p b)]"
						+ "[r5: (?x q D1) <- (?x p b)]" + "[r6: (?x t C1) <- (?x p b)]" + "[r7: (?x t C2) <- (?x p b)]"
						+ "[r8: (?x t C3) <- (?x p b)]" + "[r9: (?x s ?y) <- (?w r C1) (?x q ?y) (?w t C1)]"),
				"<a>,<p>,<b>", new TriplePattern("?s <s> ?o"), "s=<a>,o=<D1>|s=<a>,o=<C2>|s=<a>,o=<C3>");
	}

	/**
	 * Test backtracking - multiple triple matches
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testBacktrack9() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules("[r1: (?x s ?y) <- (?x r ?y) (?x q ?y)]"),
				"<a>,<r>,<D1>|<a>,<r>,<D2>|<a>,<r>,<D3>|<b>,<r>,<D2>|<a>,<q>,<D2>|<b>,<q>,<D2>|<b>,<q>,<D3>",
				new TriplePattern("?s <s> ?o"), "s=<a>,o=<D2>|s=<b>,o=<D2>");
	}

	/**
	 * Test backtracking - multiple triple matches
	 * 
	 * TODO: contains equals builtin, so does not work?
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
//	@Test
//	public void testBacktrack10() throws InterruptedException, ExecutionException, ParseException {
//		doTest(convertRules("[r1: (?x s ?y) <- (?x r ?y) (?x q ?z), equal(?y, ?z)(?x, p, ?y)]" + "[(a p D1) <- ]"
//				+ "[(a p D2) <- ]" + "[(b p D1) <- ]"),
//				"<a>,<r>,<D1>|<a>,<r>,<D2>|<a>,<r>,<D3>|<b>,<r>,<D2>|<a>,<q>,<D2>|<b>,<q>,<D2>|<b>,<q>,<D3>",
//				new TriplePattern("?s <s> ?o"), "s=<a>,o=<D2>");
//	}

	/**
	 * Test axioms work.
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testAxioms() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules(
				"[a1: -> (a r C1) ]" + "[a2: -> (a r C2) ]" + "[a3: (b r C1) <- ]" + "[r1: (?x s ?y) <- (?x r ?y)]"),
				"", new TriplePattern("?s <s> ?o"), "s=<a>,o=<C1>|s=<a>,o=<C2>|s=<b>,o=<C1>");
	}

	/**
	 * Test nested invocation of rules with permanent vars
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testNestedPvars() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules("[r1: (?x r ?y) <- (?x p ?z) (?z q ?y)]" + "[r1: (?y t ?x) <- (?x p ?z) (?z q ?y)]"
				+ "[r3: (?x s ?y) <- (?x r ?y) (?y t ?x)]"),
				"<a>,<p>,<C1>|<a>,<p>,<C2>|<a>,<p>,<C3>|<C2>,<q>,<b>|<C3>,<q>,<c>|<D1>,<q>,<D2>",
				new TriplePattern("?s <s> ?o"), "s=<a>,o=<b>|s=<a>,o=<c>");
	}

	/**
	 * Test wildcard predicate usage - simple triple search. Rules look odd because
	 * we have to hack around the recursive loops.
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testWildPredicate1() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules("[r1: (b r ?y) <- (a ?y ?v)]"), "<a>,<p>,<C1>|<a>,<q>,<C2>|<a>,<q>,<C3>",
				new TriplePattern("<b> <r> ?o"), "o=<p>|o=<q>");
	}

	/**
	 * Test wildcard predicate usage - combind triple search and multiclause
	 * matching. Rules look odd because we have to hack around the recursive loops.
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testWildPredicate2() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules("[r1: (a r ?y) <- (b ?y ?v)]" + "[r2: (?x q ?y) <- (?x p ?y)]"
				+ "[r3: (?x s C1) <- (?x p C1)]" + "[r4: (?x t C2) <- (?x p C2)]"),
				"<b>,<p>,<C1>|<b>,<q>,<C2>|<b>,<q>,<C3>|<a>,<p>,<C1>|<a>,<p>,<C2>|<c>,<p>,<C1>",
				new TriplePattern("<a> ?p ?o"),
				"p=<r>,o=<p>|p=<r>,o=<q>|p=<q>,o=<C1>|p=<q>,o=<C2>|p=<s>,o=<C1>|p=<t>,o=<C2>|p=<p>,o=<C1>|p=<p>,o=<C2>|p=<r>,o=<s>");
	}

	/**
	 * Test wildcard predicate usage - combined triple search and multiclause
	 * matching. Rules look odd because we have to hack around the recursive loops.
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testWildPredicate3() throws InterruptedException, ExecutionException, ParseException {
		String rules = "[r1: (a r ?y) <- (b ?y ?v)]" + "[r2: (?x q ?y) <- (?x p ?y)]" + "[r3: (?x s C1) <- (?x p C1)]"
				+ "[r4: (?x t ?y) <- (?x ?y C1)]";
		String data = "<b>,<p>,<C1>|<b>,<q>,<C2>|<b>,<q>,<C3>|<a>,<p>,<C1>|<a>,<p>,<C2>|<c>,<p>,<C1>";

		doTest(JenaRules.convertJenaToKeRules(rules), data, new TriplePattern("<a> ?p <C1>"), "p=<q>|p=<s>|p=<p>");
		doTest(JenaRules.convertJenaToKeRules(rules), data, new TriplePattern("<a> <t> ?o"), "o=<q>|o=<s>|o=<p>");
		doTest(JenaRules.convertJenaToKeRules(rules), data, new TriplePattern("?s <t> <q>"), "s=<a>|s=<b>|s=<c>");
	}

	/**
	 * Test wildcard predicate usage - wildcard in head as well
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testWildPredicate4() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules("[r1: (a ?p ?x) <- (b ?p ?x)]"), "<b>,<p>,<C1>|<b>,<q>,<C2>|<b>,<q>,<C3>|<c>,<q>,<d>",
				new TriplePattern("<a> ?p ?o"), "p=<p>,o=<C1>|p=<q>,o=<C2>|p=<q>,o=<C3>");
	}

	/**
	 * Test RDFS example.
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testRDFS1() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules("[ (?a <type> C1) <- (?a <type> C2) ]" + "[ (?a <type> C2) <- (?a <type> C3) ]"
				+ "[ (?a <type> C3) <- (?a <type> C4) ]"),
				"<a>,<type>,<C1>|<b>,<type>,<C2>|<c>,<type>,<C3>|<d>,<type>,<C4>", new TriplePattern("?s <type> <C1>"),
				"s=<a>|s=<b>|s=<c>|s=<d>");
	}

	/**
	 * Test RDFS example - branched version
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testRDFS2() throws InterruptedException, ExecutionException, ParseException {
		doTest(JenaRules.convertJenaToKeRules("[ (?a <type> C1) <- (?a <type> C2) ]" + "[ (?a <type> C1) <- (?a <type> C3) ]"
				+ "[ (?a <type> C1) <- (?a <type> C4) ]"),
				"<a>,<type>,<C1>|<b>,<type>,<C2>|<c>,<type>,<C3>|<d>,<type>,<C4>", new TriplePattern("?s <type> <C1>"),
				"s=<a>|s=<b>|s=<c>|s=<d>");
	}

	/**
	 * A problem from the original backchainer tests - tabled closure operation.
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testProblem2() throws InterruptedException, ExecutionException, ParseException {
		String ruleSrc = "[rdfs8:  (?a <subClass> ?c) <- (?a <subClass> ?b), (?b <subClass> ?c)]"
				+ "[rdfs7:  (?a <subClass> ?a) <- (?a <type> <class>)]";
		doTest(JenaRules.convertJenaToKeRules(ruleSrc),
				"<C1>,<subClass>,<C2>|<C2>,<subClass>,<C3>|<C1>,<type>,<class>|<C2>,<type>,<class>|<C3>,<type>,<class>",
				new TriplePattern("?s <subClass> ?o"),
				"s=<C1>,o=<C2>|s=<C1>,o=<C3>|s=<C1>,o=<C1>|s=<C2>,o=<C3>|s=<C2>,o=<C2>|s=<C3>,o=<C3>");
	}

	/**
	 * A problem from the original backchainer tests - head unification test
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testProblem4() throws InterruptedException, ExecutionException, ParseException {
		String rules = "[r1: (c r ?x) <- (?x p ?x)]" + "[r2: (?x p ?y) <- (a q ?x), (b q ?y)]";
		doTest(JenaRules.convertJenaToKeRules(rules), "<a>,<q>,<a>|<a>,<q>,<b>|<a>,<q>,<c>|<b>,<q>,<b>|<b>,<q>,<d>",
				new TriplePattern("<c> <r> ?o"), "o=<b>");
	}

	/**
	 * A problem from the original backchainer tests - RDFS example which threw an
	 * NPE
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testProblem5() throws InterruptedException, ExecutionException, ParseException {
		String ruleSrc = "[rdfs8:  (?a <subClass> ?c) <- (?a <subClass> ?b), (?b <subClass> ?c)]"
				+ "[rdfs9:   (?a <type> ?y) <- (?x <subClass> ?y), (?a <type> ?x)]" + "[(<type> <range> <class>) <-]"
				+ "[rdfs3:  (?y <type> ?c) <- (?x ?p ?y), (?p <range> ?c)]"
				+ "[rdfs7:  (?a <subClass> ?a) <- (?a <type> <class>)]";
		doTest(JenaRules.convertJenaToKeRules(ruleSrc),
				"<p>,<subProp>,<q>|<q>,<subProp>,<r>|<C1>,<subClass>,<C2>|<C2>,<subClass>,<C3>|<a>,<type>,<C1>",
				new TriplePattern("<a> <type> ?o"), "o=<C1>|o=<C2>|o=<C3>");

	}

	/**
	 * A suspect problem, originally derived from the OWL rules - risk of unbound
	 * variables escaping. Not managed to isolate or reproduce the problem yet.
	 * 
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Test
	public void testProblem9() throws InterruptedException, ExecutionException, ParseException {
		String ruleSrc = "[test:   (?x <sameAs> ?x) <- (?x <type> <thing>) ]"
				+ "[sameIndividualAs6: (?X <type> <thing>) <- (?X <sameAs> ?Y) ]"
				+ "[ans:    (?x <p> C1) <- (?y <sameAs> ?x)]";
		doTest(JenaRules.convertJenaToKeRules(ruleSrc), "<a>,<type>,<thing>|<b>,<sameAs>,<c>", new TriplePattern("?s <p> ?o"),
				"s=<a>,o=<C1>|s=<b>,o=<C1>|s=<c>,o=<C1>");

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

		Set<BaseRule> keRules = JenaRules.convertJenaToKeRules(ruleSrc);

		doTest(keRules, "<a>,<p>,<b>", query, results);
	}

	private void doTest(Set<BaseRule> keRules, String data, TriplePattern query, String results)
			throws InterruptedException, ExecutionException, ParseException {

		RuleStore rs = new RuleStore();
		rs.addRules(keRules);

		String[] dataSplitted = new String[0];
		if (!data.isEmpty()) {
			dataSplitted = data.split("\\|");
		}

		// data rule
		DataBindingSetHandler aBindingSetHandler = new DataBindingSetHandler(
				new Table(new String[] { "s", "p", "o" }, dataSplitted));
		Rule r = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern("?s ?p ?o"))),
				aBindingSetHandler);
		rs.addRule(r);

		ProactiveRule startRule = new ProactiveRule(new HashSet<>(Arrays.asList(query)), new HashSet<>());
		rs.addRule(startRule);

		ReasonerPlan rp = new ReasonerPlan(rs, startRule);
		rp.setUseTaskBoard(true);

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
		if (results != null)
			bindset = Util.toBindingSet(results);

		assertEquals(bindset, results2);
	}

}
