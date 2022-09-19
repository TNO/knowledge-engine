package eu.knowledge.engine.reasoner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

public class RuleNode {

	/**
	 * The rule node from which this reasoner node has been derived.
	 */
	private BaseRule rule;

	/**
	 * All possible bindingsets. Not all of them are used in every situation.
	 */
	private TripleVarBindingSet incomingAntecedentBindingSet;
	private TripleVarBindingSet outgoingAntecedentBindingSet;
	private TripleVarBindingSet incomingConsequentBindingSet;
	private TripleVarBindingSet outgoingConsequentBindingSet;

	/**
	 * All relevant rules from the {@link RuleStore} whose consequents match this
	 * rule's antecedent either fully or partially.
	 */
	private Map<RuleNode, Set<Match>> antecedentNeighbors;

	/**
	 * All relevant rules from the {@link RuleStore} whose antecedents match this
	 * rule's consequent either fully or partially.
	 */
	private Map<RuleNode, Set<Match>> consequentNeighbors;

	/**
	 * Indicates whether this rule is currently being applied. It is necessary in
	 * case of using the taskboard to know our task is already on the taskboard, but
	 * the task iteself is not yet executed.
	 */
	private boolean waitingForTaskBoard = false;

	/**
	 * Create a new ReasonerNode for the rule of the given {@code aRule}.
	 * 
	 * @param aRule The rule of which to create a reasoner node.
	 */
	public RuleNode(BaseRule aRule) {
		this.antecedentNeighbors = new HashMap<>();
		this.consequentNeighbors = new HashMap<>();
		this.rule = aRule;
	}

	public BaseRule getRule() {
		return rule;
	}

	public TripleVarBindingSet getIncomingAntecedentBindingSet() {
		assert !this.getRule().getAntecedent().isEmpty();
		return incomingAntecedentBindingSet;
	}

	public boolean hasIncomingAntecedentBindingSet() {
		return incomingAntecedentBindingSet != null;
	}

	public void setIncomingAntecedentBindingSet(TripleVarBindingSet incomingAntecedentBindingSet) {
		assert !this.getRule().getAntecedent().isEmpty();
		this.incomingAntecedentBindingSet = incomingAntecedentBindingSet;
	}

	public TripleVarBindingSet getOutgoingAntecedentBindingSet() {
		assert !this.getRule().getAntecedent().isEmpty();
		return outgoingAntecedentBindingSet;
	}

	public boolean hasOutgoingAntecedentBindingSet() {
		return outgoingAntecedentBindingSet != null;
	}

	public void setOutgoingAntecedentBindingSet(TripleVarBindingSet outgoingAntecedentBindingSet) {
		assert !this.getRule().getAntecedent().isEmpty();
		this.outgoingAntecedentBindingSet = outgoingAntecedentBindingSet;
	}

	public TripleVarBindingSet getIncomingConsequentBindingSet() {
		assert !this.getRule().getConsequent().isEmpty();
		return incomingConsequentBindingSet;
	}

	public boolean hasIncomingConsequentBindingSet() {
		return incomingConsequentBindingSet != null;
	}

	public void setIncomingConsequentBindingSet(TripleVarBindingSet incomingConsequentBindingSet) {
		assert !this.getRule().getConsequent().isEmpty();
		this.incomingConsequentBindingSet = incomingConsequentBindingSet;
	}

	public TripleVarBindingSet getOutgoingConsequentBindingSet() {
		assert !this.getRule().getConsequent().isEmpty();
		return outgoingConsequentBindingSet;
	}

	public boolean hasOutgoingConsequentBindingSet() {
		return outgoingConsequentBindingSet != null;
	}

	public void setOutgoingConsequentBindingSet(TripleVarBindingSet outgoingConsequentBindingSet) {
		assert !this.getRule().getConsequent().isEmpty();
		this.outgoingConsequentBindingSet = outgoingConsequentBindingSet;
	}

	public Map<RuleNode, Set<Match>> getAntecedentNeighbors() {
		return antecedentNeighbors;
	}

	public void addAntecedentNeighbor(RuleNode aReasonerNode, Set<Match> someMatches) {
		if (this.antecedentNeighbors.put(aReasonerNode, someMatches) == null) {
			aReasonerNode.addConsequentNeighbor(this, Match.invertAll(someMatches));
		}
	}

	public void addConsequentNeighbor(RuleNode aReasonerNode, Set<Match> someMatches) {
		if (this.consequentNeighbors.put(aReasonerNode, someMatches) == null) {
			aReasonerNode.addAntecedentNeighbor(this, Match.invertAll(someMatches));
		}

	}

	public Map<RuleNode, Set<Match>> getConsequentNeighbors() {
		return consequentNeighbors;
	}

	/**
	 * For now we do not make these 'inverse' bindingsethandlers via the taskboard,
	 * because we do not really want to use them yet. They are only here to maintain
	 * a consistent and elegant structure.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void applyBindingSetHandlerFromConsequentToAntecedent() throws InterruptedException, ExecutionException {
		BaseRule r = this.getRule();
		assert r instanceof Rule;
		Rule rr = (Rule) r;

		TransformBindingSetHandler bsh = rr.getInverseBindingSetHandler();

		BindingSet aBindingSet = bsh.handle(this.getIncomingConsequentBindingSet().toBindingSet()).get();
		this.setOutgoingAntecedentBindingSet(aBindingSet.toTripleVarBindingSet(this.getRule().getAntecedent()));
	}

	public void applyBindingSetHandlerFromAntecedentToConsequent(TaskBoard aTaskBoard)
			throws InterruptedException, ExecutionException {
		BaseRule r = this.getRule();
		assert r instanceof Rule;
		Rule rr = (Rule) r;
		TransformBindingSetHandler bsh = rr.getBindingSetHandler();

		TripleVarBindingSet aBindingSet = this.getIncomingAntecedentBindingSet();

		if (aTaskBoard != null) {
			aTaskBoard.addTask(this, aBindingSet.toBindingSet());
			this.waitingForTaskBoard = true;
		} else {
			BindingSet outgoingConsequentBindingSet = bsh.handle(aBindingSet.toBindingSet()).get();
			this.setOutgoingConsequentBindingSet(
					outgoingConsequentBindingSet.toTripleVarBindingSet(this.getRule().getConsequent()));
		}
	}

	public Set<RuleNode> prepareConsequentNeighbors(Map<RuleNode, Set<Match>> someConsequentNeighbors) {
		Set<RuleNode> neighbors = new HashSet<>();
		for (Map.Entry<RuleNode, Set<Match>> neighborEntry : someConsequentNeighbors.entrySet()) {
			RuleNode neighbor = neighborEntry.getKey();
			Set<Match> neighborMatches = neighborEntry.getValue();
			TripleVarBindingSet neighborBS = this.getOutgoingConsequentBindingSet()
					.translate(neighbor.getRule().getAntecedent(), Match.invertAll(neighborMatches));

			// TODO do we want the merge to happen inside the translate method (in some
			// cases)?
			neighborBS = neighborBS.merge(neighborBS);

			// TODO !neighbor.isWaitingForTaskBoard(true) is too coarse, it returns false
			// when no taskboard is being used at all.
			if (!neighbor.hasIncomingAntecedentBindingSet() || !neighbor.isWaitingForTaskBoard(true)) {
				neighbors.add(neighbor);
			}

		}
		return neighbors;
	}

	/**
	 * 
	 * @param aBindingSet
	 * @return a set of neighbors that still need to be processed.
	 */
	public Set<RuleNode> prepareAntecedentNeighbors(TripleVarBindingSet aBindingSet) {
		Set<RuleNode> neighbors = new HashSet<>();

		boolean anyNeighborWaitingForTaskboard = false;

		for (Map.Entry<RuleNode, Set<Match>> neighborEntry : this.getAntecedentNeighbors().entrySet()) {
			RuleNode neighbor = neighborEntry.getKey();
			Set<Match> neighborMatch = neighborEntry.getValue();

			if (!neighbor.hasOutgoingConsequentBindingSet() && !neighbor.isWaitingForTaskBoard(false)) {
				TripleVarBindingSet neighborBS = aBindingSet.translate(neighbor.getRule().getConsequent(),
						Match.invertAll(neighborMatch));

				if (!neighbor.hasIncomingConsequentBindingSet())
					neighbor.setIncomingConsequentBindingSet(neighborBS);

				neighbors.add(neighbor);
			}

			if (!neighbor.hasOutgoingConsequentBindingSet() && neighbor.isWaitingForTaskBoard(false)) {
				anyNeighborWaitingForTaskboard = true;
			} else {
				// skip this neighbor
			}
		}

		if (neighbors.isEmpty() && anyNeighborWaitingForTaskboard) {
			return null;
		} else {
			return neighbors;
		}
	}

	// TODO this fails with loops
	public boolean isWaitingForTaskBoard(boolean forward) {
		boolean isWaiting = false;

		isWaiting |= this.waitingForTaskBoard;
		Set<RuleNode> neighbors;
		if (!forward) {
			neighbors = this.getAntecedentNeighbors().keySet();
		} else {
			neighbors = this.getConsequentNeighbors().keySet();
		}

		for (RuleNode rn : neighbors) {
			isWaiting |= rn.isWaitingForTaskBoard(forward);
		}

		return isWaiting;
	}

	public void applyBindingSetHandlerFromConsequentToConsequent(TaskBoard aTaskBoard)
			throws InterruptedException, ExecutionException {
		BaseRule r = this.getRule();
		assert r instanceof Rule;
		Rule rr = (Rule) r;

		TransformBindingSetHandler bsh = rr.getBindingSetHandler();

		BindingSet bindingSet = this.getIncomingConsequentBindingSet().toBindingSet();
		if (aTaskBoard != null) {
			aTaskBoard.addTask(this, bindingSet);
			this.waitingForTaskBoard = true;
		} else {
			BindingSet aBindingSet = bsh.handle(bindingSet).get();
			this.setOutgoingConsequentBindingSet(aBindingSet.toTripleVarBindingSet(this.getRule().getConsequent()));
		}
	}

	public boolean contactAntecedentNeighbors(TripleVarBindingSet aBindingSet, boolean forward) {

		boolean allHaveContributed = true;
		// set neighbor's incoming consequent bindingset
		for (Map.Entry<RuleNode, Set<Match>> neighborEntry : this.getAntecedentNeighbors().entrySet()) {
			RuleNode neighbor = neighborEntry.getKey();
			Set<Match> neighborMatch = neighborEntry.getValue();

			if (!neighbor.hasOutgoingConsequentBindingSet()) {
				allHaveContributed = false;
				TripleVarBindingSet neighborBS = aBindingSet.translate(neighbor.getRule().getConsequent(),
						Match.invertAll(neighborMatch));
				neighbor.mergeIncomingConsequentBindingSet(neighborBS, forward);
			}
		}
		return allHaveContributed;
	}

	public void applyBindingSetHandlerToAntecedent(TaskBoard aTaskBoard) {
		BaseRule r = this.getRule();
		assert r instanceof Rule;
		Rule rr = (Rule) r;

		TripleVarBindingSet aBindingSet = this.getIncomingAntecedentBindingSet();
		if (!isEmpty(aBindingSet)) {
			SinkBindingSetHandler bsh = rr.getSinkBindingSetHandler();
			if (aTaskBoard != null) {
				aTaskBoard.addVoidTask(this, aBindingSet.toBindingSet());
				this.waitingForTaskBoard = true;
			} else {
				bsh.handle(aBindingSet.toBindingSet());

			}
		}
	}

	/**
	 * Collect the bindingsets from antecedent neighbors and make sure they are
	 * compatible with the incoming bindingset. This is either the outgoing
	 * antecedent bindingset (in case of backward chaining) or the outgoing
	 * consequent bindingset of our parent neighbor (in case of backward while
	 * forward chaining)
	 * 
	 * @param aBindingSet
	 */
	public void collectIncomingAntecedentBindingSet(TripleVarBindingSet aBindingSet) {

		TripleVarBindingSet combinedBindings = new TripleVarBindingSet(this.getRule().getAntecedent());
		for (Map.Entry<RuleNode, Set<Match>> neighborEntry : this.getAntecedentNeighbors().entrySet()) {
			RuleNode neighbor = neighborEntry.getKey();
			Set<Match> neighborMatches = neighborEntry.getValue();
			assert neighbor.hasOutgoingConsequentBindingSet();
			TripleVarBindingSet neighborOutgoingConsequentBindingSet = neighbor.getOutgoingConsequentBindingSet();

			combinedBindings = combinedBindings.merge(
					neighborOutgoingConsequentBindingSet.translate(this.getRule().getAntecedent(), neighborMatches));
		}

		combinedBindings = combinedBindings.merge(combinedBindings);

		TripleVarBindingSet compatiblePatternBindingsOnly = combinedBindings.keepCompatible(aBindingSet);

		this.setIncomingAntecedentBindingSet(compatiblePatternBindingsOnly.getFullBindingSet());

	}

	public Map<TriplePattern, Set<RuleNode>> findAntecedentCoverage(Map<RuleNode, Set<Match>> someAntecedentNeighbors) {
		var antecedentCoverageCache = new HashMap<TriplePattern, Set<RuleNode>>();

		// find the coverage
		Set<RuleNode> coveringNodes;
		for (TriplePattern tp : this.rule.getAntecedent()) {
			coveringNodes = new HashSet<>();
			antecedentCoverageCache.put(tp, coveringNodes);

			for (Entry<RuleNode, Set<Match>> entry : someAntecedentNeighbors.entrySet()) {
				for (Match m : entry.getValue()) {
					if (m.getMatchingPatterns().values().contains(tp)) {
						coveringNodes.add(entry.getKey());
						break; // where does this break from? The inner loop.
					}
				}
			}

		}
		return antecedentCoverageCache;
	}

	/**
	 * Check all children and remove the ones of which the antecedent is not fully
	 * covered.
	 */
	public Set<RuleNode> prune() {

		Set<RuleNode> removedNodes = new HashSet<>();
		Iterator<Entry<RuleNode, Set<Match>>> entryIter = this.getAntecedentNeighbors().entrySet().iterator();
		while (entryIter.hasNext()) {
			Entry<RuleNode, Set<Match>> entry = entryIter.next();
			removedNodes.addAll(entry.getKey().prune());
			if (!entry.getKey().isAntecedentFullyCovered()) {
				entryIter.remove();
				removedNodes.addAll(entry.getKey().getDirectAndIndirectNeighbors());
				removedNodes.add(entry.getKey());

			}
		}
		return removedNodes;
	}

	private Set<RuleNode> getDirectAndIndirectNeighbors() {
		Set<RuleNode> children = new HashSet<>(this.getAntecedentNeighbors().keySet());

		for (RuleNode rn : children) {
			children.addAll(rn.getDirectAndIndirectNeighbors());
		}

		return children;
	}

	public boolean isAntecedentFullyCovered() {
		boolean isFullyCovered = true;

		Map<RuleNode, Set<Match>> someAntecedentNeighbors = new HashMap<>(this.getAntecedentNeighbors());

//		if (!this.shouldPlanBackward && this.parent != null) {
//			Set<Match> parentMatches = this.rule.antecedentMatches(this.parent.rule.getConsequent(),
//					this.matchStrategy);
//
//			Set<Match> inversedParentMatches = new HashSet<>();
//			for (Match m : parentMatches) {
//				inversedParentMatches.add(m.inverse());
//			}
//
//			someAntecedentNeighbors.put(this.parent, inversedParentMatches);
//		}

		Map<TriplePattern, Set<RuleNode>> nodeCoverage = this.findAntecedentCoverage(someAntecedentNeighbors);

		for (Entry<TriplePattern, Set<RuleNode>> entry : nodeCoverage.entrySet()) {
			if (entry.getValue().isEmpty()) {
				isFullyCovered = false;
				break;
			}
		}
		return isFullyCovered;

	}

	private boolean isEmpty(TripleVarBindingSet aBindingSet) {
		return aBindingSet.isEmpty()
				|| (aBindingSet.getBindings().size() == 1 && !aBindingSet.getBindings().iterator().hasNext());
	}

	/**
	 * This methods sets the given bindingset (from the TaskBoard) in the correct
	 * TripleVarBindingSet of this RuleNode. Can the RuleNode determine solely based
	 * on the structure where it needs to be put? Probably not, certainly because
	 * there are inverse bindingsethandlers as well, but we'll ignore those for now.
	 * 
	 * This method also changes the state of this RuleNode to no longer waiting for
	 * the taskboard.
	 * 
	 * @param aBindingSet Either the resulting bindingset or {@code null} if there
	 *                    is not resulting bindingset.
	 */
	public void setBindingSet(BindingSet aBindingSet) {

		// TODO in the future we might need to make this method more complex.
		if (aBindingSet != null)
			this.outgoingConsequentBindingSet = aBindingSet.toTripleVarBindingSet(this.getRule().getConsequent());
		this.setWaitingForTaskBoard(false);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rule == null) ? 0 : rule.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RuleNode other = (RuleNode) obj;
		if (rule == null) {
			if (other.rule != null)
				return false;
		} else if (!rule.equals(other.rule))
			return false;
		return true;
	}

	public boolean hasAntecedent() {
		return !this.rule.getAntecedent().isEmpty();
	}

	public boolean hasConsequent() {
		return !this.rule.getConsequent().isEmpty();
	}

	@Override
	public String toString() {
		if (this.getRule() instanceof ProactiveRule) {
			if (this.hasAntecedent()) {
				return this.toString(0, false);
			} else if (this.hasConsequent()) {
				return this.toString(0, true);
			} else
				assert false;

		} else
			return this.toString(0, false);
		return this.getRule().toString();
	}

	public String toString(int tabIndex, boolean isForward) {

		StringBuilder sb = new StringBuilder();

		// print yourself
		if (isForward ? this.hasAntecedent() : this.hasConsequent()) {
			if (isForward ? this.hasIncomingAntecedentBindingSet() : this.hasOutgoingConsequentBindingSet())
				sb.append(isForward ? this.getIncomingAntecedentBindingSet().toBindingSet()
						: this.getOutgoingConsequentBindingSet().toBindingSet());
			else
				sb.append(isForward ? this.rule.getAntecedent() : this.rule.getConsequent());
		}

		sb.append(isForward ? "->" : "<-");

		if (isForward ? this.hasConsequent() : this.hasAntecedent()) {

			if (isForward ? this.hasOutgoingConsequentBindingSet() : this.hasIncomingAntecedentBindingSet())
				sb.append(isForward ? this.getOutgoingConsequentBindingSet().toBindingSet()
						: this.getIncomingAntecedentBindingSet().toBindingSet());
			else
				sb.append(isForward ? this.rule.getConsequent() : this.rule.getAntecedent());

		}

		sb.append("\n");
		if (!isForward)
			for (RuleNode neighbor : this.getAntecedentNeighbors().keySet()) {
				for (int i = 0; i < tabIndex + 1; i++)
					sb.append("\t");

				sb.append(neighbor.toString(tabIndex + 1, isForward));
			}
		else
			for (RuleNode neighbor : this.getConsequentNeighbors().keySet()) {
				for (int i = 0; i < tabIndex + 1; i++)
					sb.append("\t");

				sb.append(neighbor.toString(tabIndex + 1, isForward));
			}

		return sb.toString();
	}

	public void setWaitingForTaskBoard(boolean aWaitingForTaskBoard) {
		waitingForTaskBoard = aWaitingForTaskBoard;

	}

	public TripleVarBindingSet collectIncomingAntecedentBindingSet2(TripleVarBindingSet aBindingSet) {
		TripleVarBindingSet combinedBindings = null;
		for (Map.Entry<RuleNode, Set<Match>> neighborEntry : this.getAntecedentNeighbors().entrySet()) {
			RuleNode neighbor = neighborEntry.getKey();
			Set<Match> neighborMatch = neighborEntry.getValue();

			if (!neighbor.hasOutgoingConsequentBindingSet()) {
				TripleVarBindingSet neighborBS = aBindingSet.translate(neighbor.getRule().getConsequent(),
						Match.invertAll(neighborMatch));

				if (!neighbor.hasIncomingConsequentBindingSet())
					neighbor.setIncomingConsequentBindingSet(neighborBS);

			} else {
				combinedBindings = new TripleVarBindingSet(this.getRule().getAntecedent());
				TripleVarBindingSet neighborOutgoingConsequentBindingSet = neighbor.getOutgoingConsequentBindingSet();

				combinedBindings = combinedBindings.merge(
						neighborOutgoingConsequentBindingSet.translate(this.getRule().getAntecedent(), neighborMatch));
			}
		}
		return combinedBindings;
	}

	public void pushOutgoingConsequentBindingSet(TripleVarBindingSet outgoingConsequentBS) {
		for (Map.Entry<RuleNode, Set<Match>> neighborEntry : this.getConsequentNeighbors().entrySet()) {
			RuleNode neighbor = neighborEntry.getKey();
			Set<Match> neighborMatches = neighborEntry.getValue();

			TripleVarBindingSet neighborBS = outgoingConsequentBS.translate(neighbor.getRule().getAntecedent(),
					Match.invertAll(neighborMatches));
			neighbor.setIncomingAntecedentBindingSet(neighborBS);
		}

	}

	/**
	 * @param otherOne
	 * @return {@code true} if merge actually changed something, {@code false}
	 *         otherwise.
	 */
	public boolean mergeIncomingConsequentBindingSet(TripleVarBindingSet otherOne, boolean onlyKeepCompatible) {
		var bs = mergeBindingSets(this.getIncomingConsequentBindingSet(), otherOne);
		if (bs != null) {
			if (onlyKeepCompatible)
				bs = bs.keepCompatible(this.getIncomingConsequentBindingSet());
			this.setIncomingConsequentBindingSet(bs);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param otherOne
	 * @return {@code true} if merge actually changed something, {@code false}
	 *         otherwise.
	 */
	public boolean mergeIncomingAntecedentBindingSet(TripleVarBindingSet otherOne, boolean onlyKeepCompatible) {
		var bs = mergeBindingSets(this.incomingConsequentBindingSet, otherOne);
		if (bs != null) {
			if (onlyKeepCompatible)
				bs = bs.keepCompatible(this.getIncomingConsequentBindingSet());
			this.setIncomingAntecedentBindingSet(bs);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Merges two bindingsets and returns {@code null} if nothing has changed.
	 * 
	 * @param existingOne
	 * @param otherOne
	 * @return
	 */
	public TripleVarBindingSet mergeBindingSets(TripleVarBindingSet existingOne, TripleVarBindingSet otherOne) {
		TripleVarBindingSet newOne = existingOne.merge(otherOne);
		if (existingOne.equals(newOne)) {
			return null;
		} else
			return newOne;
	}
}
