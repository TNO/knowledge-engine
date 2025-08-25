package eu.knowledge.engine.reasoner;

import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.BaseRule.CombiMatch;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulenode.RuleNode;

/**
 * @author nouwtb
 *
 */
public interface ConsSide {

	public void setConsequentCombiMatches(Set<CombiMatch> someMatches);

	public Set<CombiMatch> getConsequentCombiMatches();

	public void addConsequentNeighbour(RuleNode neighbour, Set<Match> matches);

	public Map<RuleNode, Set<Match>> getConsequentNeighbours();

	public boolean addFilterBindingSetInput(RuleNode aNeighbor, Map<Match, TripleVarBindingSet> someBindingSets);
}
