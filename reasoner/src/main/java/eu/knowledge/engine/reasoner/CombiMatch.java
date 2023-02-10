package eu.knowledge.engine.reasoner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulenode.RuleNode;

public class CombiMatch {
  final private Map<RuleNode, Match> matchesPerRuleNode;

  private CombiMatch(Map<RuleNode, Match> someMatchesPerRuleNode) {
    this.matchesPerRuleNode = someMatchesPerRuleNode;
  }

  public CombiMatch(RuleNode aRuleNode, Match aMatch) {
    this.matchesPerRuleNode = new HashMap<>();
    this.matchesPerRuleNode.put(aRuleNode, aMatch);
  }

  /**
   * Merge this CombiMatch with the given CombiMatch, and return the new
   * CombiMatch as a result; leaves this CombiMatch as is.
   *
   * If the other CombiMatch is incompatible/conflicting, this will return null.
   * 
   * @param other
   * @return The merged CombiMatch, or null if the merge is not possible.
   */
  public CombiMatch merge(CombiMatch other) {
    CombiMatch result = new CombiMatch(this.matchesPerRuleNode);
    
    throw new UnsupportedOperationException();
    // TODO: merge
    
    // return result;
  }

  /**
   * Returns true if this CombiMatch fully covers the given graph pattern, and
   * false otherwise.
   * @param graphPattern
   * @return
   */
  public boolean completelyCovers(Set<TriplePattern> graphPattern) {
    throw new UnsupportedOperationException();
  }
}
