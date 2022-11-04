package eu.knowledge.engine.reasoner2.reasoningnode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

/**
 * 
 * A class that manages the bindingsets of a RuleNode. Keeps track of which
 * neighbors contributed which binding sets and whether they changed.
 * 
 * @author nouwtb
 *
 */
public class BindingSetStore {

	private Set<RuleNode> neighbors;
	private Map<RuleNode, TripleVarBindingSet> neighborBindingSet = new HashMap<>();
	private Set<TriplePattern> graphPattern;

	public BindingSetStore(Set<TriplePattern> aGraphPattern, Set<RuleNode> someNeighbors) {
		this.graphPattern = aGraphPattern;
		this.neighbors = someNeighbors;
	}

	/**
	 * Register the given {@code aBindingSet} to this BindingSetStore as coming from
	 * neighbor {@code aNeighbor}
	 * 
	 * @param aNeighbor   the neighbor who brought this bindingset.
	 * @param aBindingSet the bindingset brought by this neighbor.
	 * @return whether adding this bindingset changed anything. I.e. this particular
	 *         neighbor did not yet give a bindingset or this bindingset is
	 *         different from the previous one.
	 */
	public boolean add(RuleNode aNeighbor, TripleVarBindingSet aBindingSet) {
		assert aBindingSet != null;
		assert neighbors.contains(aNeighbor);

		TripleVarBindingSet previousBindingSet = neighborBindingSet.put(aNeighbor, aBindingSet);

		return previousBindingSet == null || !previousBindingSet.equals(aBindingSet);
	}

	public boolean haveAllNeighborsContributed() {
		return this.neighborBindingSet.keySet().containsAll(neighbors);
	}

	/**
	 * @return the bindingset with the combined bindingset of all neighbors.
	 */
	public TripleVarBindingSet get() {
		assert haveAllNeighborsContributed();

		TripleVarBindingSet combinedBS = new TripleVarBindingSet(graphPattern);
		for (TripleVarBindingSet bs : this.neighborBindingSet.values()) {
			combinedBS = combinedBS.merge(bs);
		}
		return combinedBS;
	}
}
