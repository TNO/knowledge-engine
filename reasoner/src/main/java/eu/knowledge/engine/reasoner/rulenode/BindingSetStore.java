package eu.knowledge.engine.reasoner.rulenode;

import java.util.HashMap;
import java.util.HashSet;
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

	private final Set<RuleNode> neighbors;
	private final Map<RuleNode, TripleVarBindingSet> neighborBindingSet = new HashMap<>();
	private final Set<TriplePattern> graphPattern;

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

		TripleVarBindingSet previousBindingSet = this.neighborBindingSet.put(aNeighbor, aBindingSet);

		return previousBindingSet == null || !previousBindingSet.equals(aBindingSet);
	}

	public boolean haveAllNeighborsContributed() {
		return this.neighborBindingSet.keySet().containsAll(neighbors);
	}

	// TODO: It feels ugly to do this.
	public boolean haveAllNeighborsContributedExcept(Set<RuleNode> nodes) {
		var allNeighboursWithException = new HashSet<>();
		allNeighboursWithException.addAll(neighbors);
		allNeighboursWithException.removeAll(nodes);
		return this.neighborBindingSet.keySet().containsAll(allNeighboursWithException);
	}

	/**
	 * @return the bindingset with the combined bindingset of all neighbors.
	 */
	public TripleVarBindingSet get() {
		// TODO: Can a similar assertion be made? (Changed the class so that the
		// result is also gettable when all neighbours except a specific one
		// contributed)
		// assert haveAllNeighborsContributed();

		TripleVarBindingSet combinedBS = new TripleVarBindingSet(graphPattern);
		for (TripleVarBindingSet bs : this.neighborBindingSet.values()) {
			combinedBS = combinedBS.merge(bs);
		}

		// NOTE: we merge the bindings with themselves here (when the bindings
		// 'leave' the store), but it may be better to do it when they enter the
		// store, or when they get translated/matched.
		return combinedBS.merge(combinedBS);
	}
}
