package eu.knowledge.engine.reasoner2.reasoningnode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulenode.ActiveConsRuleNode;
import eu.knowledge.engine.reasoner.rulenode.BindingSetStore;
import eu.knowledge.engine.reasoner.rulenode.RuleNode;
import eu.knowledge.engine.reasoner2.api.Util;

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
    var changed = bss.add(neighbour, new BindingSet().toTripleVarBindingSet(graphPattern));
    assertTrue(changed);
    changed = bss.add(neighbour, new BindingSet().toTripleVarBindingSet(graphPattern));
    assertFalse(changed);
    assertTrue(bss.haveAllNeighborsContributed());
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
    var changed = bss.add(neighbour, bs.toTripleVarBindingSet(graphPattern));
    assertTrue(changed);
    bs = Util.toBindingSet("s=<bla>,o=7");
    changed = bss.add(neighbour, bs.toTripleVarBindingSet(graphPattern));
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
    var changed = bss.add(neighbour, bs.toTripleVarBindingSet(graphPattern));
    assertTrue(changed);
    bs = Util.toBindingSet("s=<bla>,o=8");
    changed = bss.add(neighbour, bs.toTripleVarBindingSet(graphPattern));
    assertTrue(changed);
    assertTrue(bss.haveAllNeighborsContributed());
  }
}
