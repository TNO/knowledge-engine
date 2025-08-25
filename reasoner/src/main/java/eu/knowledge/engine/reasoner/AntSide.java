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
public interface AntSide {

	public void setAntecedentCombiMatches(Set<CombiMatch> someCombiMatches);

	public Set<CombiMatch> getAntecedentCombiMatches();

	public void addAntecedentNeighbour(RuleNode neighbour, Set<Match> matches);

	public Map<RuleNode, Set<Match>> getAntecedentNeighbours();

	public boolean addResultBindingSetInput(RuleNode aNeighbor, Map<Match, TripleVarBindingSet> someBindingSets);

}
