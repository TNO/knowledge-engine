package eu.knowledge.engine.reasoner.reasoningnode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.api.Util;
import eu.knowledge.engine.reasoner.rulenode.ActiveConsRuleNode;
import eu.knowledge.engine.reasoner.rulenode.BindingSetStore;
import eu.knowledge.engine.reasoner.rulenode.RuleNode;

public class TestBindingSetStore {
	@Test
	public void testAddingEmptyBindingSetTwice() {
		Set<TriplePattern> graphPattern = new HashSet<>();
		var tp = new TriplePattern("?s :prop ?o");
		graphPattern.add(tp);

		Set<RuleNode> ruleNodes = new HashSet<>();
		var neighbour = new ActiveConsRuleNode(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(tp))));
		ruleNodes.add(neighbour);

		BindingSetStore bss = new BindingSetStore(graphPattern, ruleNodes);

		assertFalse(bss.haveAllNeighborsContributed());
		TripleVarBindingSet tripleVarBindingSet = new BindingSet().toTripleVarBindingSet(graphPattern);

		var changed = bss.add(neighbour, convertToDummyMap(tripleVarBindingSet));
		assertTrue(changed);

		TripleVarBindingSet tripleVarBindingSet2 = new BindingSet().toTripleVarBindingSet(graphPattern);

		changed = bss.add(neighbour, convertToDummyMap(tripleVarBindingSet2));
		assertFalse(changed);

		assertTrue(bss.haveAllNeighborsContributed());
	}

	private Map<Match, TripleVarBindingSet> convertToDummyMap(TripleVarBindingSet aTVBS) {
		var map = new HashMap<Match, TripleVarBindingSet>();
		map.put(new Match(new TriplePattern("?s ?p ?o"), new TriplePattern("?a ?b ?c"), new HashMap<>()), aTVBS);
		return map;
	}

	@Test
	public void testAddingNonEmptyBindingSetTwice() {
		Set<TriplePattern> graphPattern = new HashSet<>();
		var tp = new TriplePattern("?s :prop ?o");
		graphPattern.add(tp);

		Set<RuleNode> ruleNodes = new HashSet<>();
		var neighbour = new ActiveConsRuleNode(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(tp))));
		ruleNodes.add(neighbour);

		BindingSetStore bss = new BindingSetStore(graphPattern, ruleNodes);

		assertFalse(bss.haveAllNeighborsContributed());
		var bs = Util.toBindingSet("s=<bla>,o=7");
		var changed = bss.add(neighbour, convertToDummyMap(bs.toTripleVarBindingSet(graphPattern)));
		assertTrue(changed);
		bs = Util.toBindingSet("s=<bla>,o=7");
		changed = bss.add(neighbour, convertToDummyMap(bs.toTripleVarBindingSet(graphPattern)));
		assertFalse(changed);
		assertTrue(bss.haveAllNeighborsContributed());
	}

	@Test
	public void testAddingDifferentNonEmptyBindingSets() {
		Set<TriplePattern> graphPattern = new HashSet<>();
		var tp = new TriplePattern("?s :prop ?o");
		graphPattern.add(tp);

		Set<RuleNode> ruleNodes = new HashSet<>();
		var neighbour = new ActiveConsRuleNode(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(tp))));
		ruleNodes.add(neighbour);

		BindingSetStore bss = new BindingSetStore(graphPattern, ruleNodes);

		assertFalse(bss.haveAllNeighborsContributed());
		var bs = Util.toBindingSet("s=<bla>,o=7");
		var changed = bss.add(neighbour, convertToDummyMap(bs.toTripleVarBindingSet(graphPattern)));
		assertTrue(changed);
		bs = Util.toBindingSet("s=<bla>,o=8");
		changed = bss.add(neighbour, convertToDummyMap(bs.toTripleVarBindingSet(graphPattern)));
		assertTrue(changed);
		assertTrue(bss.haveAllNeighborsContributed());
	}
}
