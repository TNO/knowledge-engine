package eu.knowledge.engine.reasonerprototype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasonerprototype.RuleAlt.Match;
import eu.knowledge.engine.reasonerprototype.RuleAlt.MatchStrategy;
import eu.knowledge.engine.reasonerprototype.api.Binding;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.GraphBindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Variable;
import eu.knowledge.engine.reasonerprototype.api.TripleVar;
import eu.knowledge.engine.reasonerprototype.api.TripleVarBinding;

public class NodeAlt {

	private Map<NodeAlt, Set<Match>> children;

	private List<RuleAlt> allRules;

	/**
	 * Goals are represented as rules with an antecedent without a consequent.
	 */
	private RuleAlt rule;

	private static final int BINDINGSET_NOT_REQUESTED = 0, BINDINGSET_REQUESTED = 1, BINDINGSET_AVAILABLE = 2;

	/**
	 * This is the bindingset that has been retrieved and is now ready to be used to
	 * continue the reason process.
	 */
	private BindingSet resultingBindingSet;

	/**
	 * This state keeps track of whether the procedural code (if there is one) to
	 * remotely access other KBs has already been requested or already available.
	 */
	private int state = BINDINGSET_NOT_REQUESTED;

	/**
	 * Whether this node only accepts children reasoning nodes that full match its
	 * antecedent. Note that setting this to true effectively disables the reasoning
	 * functionality and reduces this algorithm to a matching algorithm.
	 */
	private MatchStrategy fullMatchOnly;

	public NodeAlt(List<RuleAlt> someRules, NodeAlt aParent, RuleAlt aRule, MatchStrategy aMatchStrategy) {

		this.fullMatchOnly = aMatchStrategy;
		this.allRules = someRules;
		this.rule = aRule;
		this.children = new HashMap<NodeAlt, Set<Match>>();

		// generate children
		if (!this.rule.antecedent.isEmpty()) {
			Map<RuleAlt, Set<Match>> relevantRules = findRulesWithOverlappingConsequences(this.rule.antecedent,
					this.fullMatchOnly);

			NodeAlt child;
			for (Map.Entry<RuleAlt, Set<Match>> entry : relevantRules.entrySet()) {
				// create a childnode
				child = new NodeAlt(this.allRules, this, entry.getKey(), this.fullMatchOnly);
				this.children.put(child, entry.getValue());
			}
		}

		// TODO we need some ininite loop detection (see transitivity rule)
	}

	private Map<RuleAlt, Set<Match>> findRulesWithOverlappingConsequences(Set<TriplePattern> aPattern,
			MatchStrategy aMatchStrategy) {

		assert aPattern != null;
		assert !aPattern.isEmpty();

		Set<Match> possibleMatches;
		Map<RuleAlt, Set<Match>> overlappingRules = new HashMap<>();
		for (RuleAlt r : this.allRules) {

			if (!(possibleMatches = r.consequentMatches(aPattern, aMatchStrategy)).isEmpty()) {
				overlappingRules.put(r, possibleMatches);
			}
		}
		assert overlappingRules != null;
		return overlappingRules;
	}

	/**
	 * Continues with the reasoning process. Each time this method is called, the
	 * reasoning process is try to continue. Note that this method expects the
	 * pending tasks to be executing between subsequent tasks to this method. This
	 * method will schedule additional tasks that need to be executed again before
	 * it can continue with the reasoning process.
	 * 
	 * @param bindingSet the incoming binding set coming from the consequence.
	 * @return complete bindingset when finished, otherwise null.
	 */
	public BindingSet continueReasoning(BindingSet bindingSet) {

		// send the binding set down the tree and combine the resulting bindingsets to
		// the actual result.
		if (this.state != BINDINGSET_AVAILABLE) {

			boolean allBindingSetsAvailable = true;

			Set<NodeAlt> someChildren = this.children.keySet();

			// we transfer the incomingBindingSet (from the consequent) to the antecedent
			// bindingset (if it exists). This latter one is send to the different children.
			GraphBindingSet antecedentPredefinedBindings;
			antecedentPredefinedBindings = new GraphBindingSet(this.rule.antecedent);
			TripleVarBinding aTripleVarBinding;
			for (Binding b : bindingSet) {
				aTripleVarBinding = new TripleVarBinding(this.rule.antecedent, b);
				antecedentPredefinedBindings.add(aTripleVarBinding);
			}

			// include the allBindingSetsAvailable into the while condition, to process a
			// single node of the tree a time. This makes sure that the results from the
			// first child are send to the second child to check. Otherwise, they get
			// combined and end up in the result without anyone ever checking them.

			// TODO the order in which we iterate the children is important. A strategy like
			// first processing the children that have the incoming variables in their
			// consequent might be smart.
			GraphBindingSet combinedBindings = new GraphBindingSet(this.rule.antecedent);

			Iterator<NodeAlt> someIter = someChildren.iterator();
			while (/* allBindingSetsAvailable && */someIter.hasNext()) {
				NodeAlt child = someIter.next();
				Set<Match> childMatch = this.children.get(child);

				// we can combine all different matches of this child's consequent into a single
				// bindingset. We do not want to send bindings with variables that do not exist
				// for that child.

				GraphBindingSet preparedBindings1 = combinedBindings.getPartialBindingSet();
				GraphBindingSet preparedBindings2 = preparedBindings1.merge(antecedentPredefinedBindings);

				BindingSet childBindings = child.continueReasoning(
						preparedBindings2.translate(child.rule.consequent, childMatch).toBindingSet());
				if (childBindings == null) {
					allBindingSetsAvailable = false;
				} else {
					GraphBindingSet childGraphBindingSet = childBindings.toGraphBindingSet(child.rule.consequent);

					// create powerset of graph pattern triples and use those to create additional
					// triplevarbindings.

					if (this.fullMatchOnly.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES))
						childGraphBindingSet = generateAdditionalTripleVarBindings(childGraphBindingSet);

					GraphBindingSet convertedChildGraphBindingSet = childGraphBindingSet
							.translate(combinedBindings.getGraphPattern(), invert(childMatch));
					combinedBindings = combinedBindings.merge(convertedChildGraphBindingSet);
				}
			}

			if (allBindingSetsAvailable) {

				GraphBindingSet consequentAntecedentBindings;
				if (this.children.isEmpty()) {
					consequentAntecedentBindings = bindingSet.toGraphBindingSet(this.rule.consequent);
				} else {
					consequentAntecedentBindings = keepOnlyCompatiblePatternBindings(
							bindingSet.toGraphBindingSet(this.rule.antecedent), combinedBindings);

					consequentAntecedentBindings = keepOnlyFullGraphPatternBindings(this.rule.antecedent,
							consequentAntecedentBindings);

				}

				if (true) {
					if (this.rule.antecedent.isEmpty() || !consequentAntecedentBindings.isEmpty()) {

						TaskBoard.instance().addTask(this, consequentAntecedentBindings.toBindingSet());
						this.state = BINDINGSET_REQUESTED;
					} else {
						this.resultingBindingSet = new BindingSet();
						this.state = BINDINGSET_AVAILABLE;
						return this.resultingBindingSet;
					}
				} else {

					// call the handler directly, to make debugging easier.
					if (this.rule.antecedent.isEmpty() || !consequentAntecedentBindings.isEmpty()) {

						this.resultingBindingSet = this.rule.getBindingSetHandler()
								.handle(consequentAntecedentBindings.toBindingSet());
						this.state = BINDINGSET_AVAILABLE;
					} else {
						this.resultingBindingSet = new BindingSet();
						this.state = BINDINGSET_AVAILABLE;
					}
					return this.resultingBindingSet;

				}
			}

			return null;
		} else {
			assert resultingBindingSet != null;

			// TODO internally the bindingset should contain bindings with variables where
			// instead of having a single entry per variable name, we need an entry per
			// variable occurrence. This is to make sure that different predicates going to
			// the same two variables are still correctly matched.
			// we start however, with the naive version that does not support this.
			return this.resultingBindingSet;
		}
	}

	private GraphBindingSet generateAdditionalTripleVarBindings(GraphBindingSet childGraphBindingSet) {

		// create powerset
		Set<TriplePattern> graphPattern = childGraphBindingSet.getGraphPattern();
		Set<List<Boolean>> binarySubsets = createBinarySubsets(graphPattern.size());
		Set<Set<TriplePattern>> gpPowerSet = createPowerSet(new ArrayList<TriplePattern>(graphPattern), binarySubsets);

		// create additional triplevarbindings
		GraphBindingSet newGraphBindingSet = new GraphBindingSet(graphPattern);
		Set<TripleVarBinding> permutatedTVBs;
		for (TripleVarBinding tvb : childGraphBindingSet.getBindings()) {
			permutatedTVBs = permutateTripleVarBinding(tvb, gpPowerSet);
			newGraphBindingSet.addAll(permutatedTVBs);
		}

		return newGraphBindingSet;
	}

	/**
	 * Returns the permutation (except for the empty set).
	 * 
	 * @param tvb
	 * @param powerSets
	 * @return
	 */
	private Set<TripleVarBinding> permutateTripleVarBinding(TripleVarBinding tvb, Set<Set<TriplePattern>> powerSets) {

		Set<TripleVarBinding> permutated = new HashSet<>();
		for (Set<TriplePattern> set : powerSets) {
			if (!set.isEmpty()) {
				TripleVarBinding newTVB = new TripleVarBinding();
				for (TriplePattern tp : set) {
					for (TripleVar tv2 : tvb.getTripleVars()) {
						if (tv2.tp.equals(tp)) {
							newTVB.put(tv2, tvb.get(tv2));
						}
					}
				}
				permutated.add(newTVB);
			}
		}

		return permutated;
	}

	/**
	 * Only keep those bindings in bindingSet2 that are compatible with the bindings
	 * in bindingSet.
	 * 
	 * @param bindingSet
	 * @param bindingSet2
	 * @return
	 */
	private GraphBindingSet keepOnlyCompatiblePatternBindings(GraphBindingSet bindingSet, GraphBindingSet bindingSet2) {

		GraphBindingSet newBS = new GraphBindingSet(bindingSet2.getGraphPattern());

		for (TripleVarBinding b : bindingSet2.getBindings()) {

			if (!bindingSet.isEmpty()) {
				for (TripleVarBinding b2 : bindingSet.getBindings()) {

					if (!b2.isEmpty()) {

						if (b.isOverlapping(b2) && !b.isConflicting(b2)) {
							newBS.add(b);
						}
					} else {
						newBS.add(b);
					}
				}
			} else {
				newBS.add(b);
			}
		}

		return newBS;
	}

	private GraphBindingSet keepOnlyFullGraphPatternBindings(Set<TriplePattern> graphPattern,
			GraphBindingSet someBindings) {
		GraphBindingSet bs = new GraphBindingSet(someBindings.getGraphPattern());
		for (TripleVarBinding b : someBindings.getBindings()) {
			if (isFullBinding(graphPattern, b)) {
				bs.add(new TripleVarBinding(b));
			}
		}
		return bs;
	}

	private boolean isFullBinding(Set<TriplePattern> graphPattern, TripleVarBinding b) {
		return b.keySet().containsAll(getTripleVars(graphPattern));
	}

	public Set<TripleVar> getTripleVars(Set<TriplePattern> aPattern) {
		Set<TripleVar> allTVs = new HashSet<>();
		for (TriplePattern tp : aPattern) {
			for (Variable var : tp.getVars()) {
				allTVs.add(new TripleVar(tp, var));
			}
		}
		return allTVs;
	}

	public Set<Variable> getVars(Set<TriplePattern> aPattern) {
		Set<Variable> vars = new HashSet<Variable>();
		for (TriplePattern t : aPattern) {
			vars.addAll(t.getVars());
		}
		return vars;
	}

	private int getState() {
		return this.state;
	}

	public Set<Match> invert(Set<Match> set) {
		Set<Match> inverted = new HashSet<>();
		for (Match map : set) {
			inverted.add(map.inverse());
		}
		return inverted;
	}

	/**
	 * This method should be used by the taskboard whenever a bindingset has been
	 * produced.
	 * 
	 * @param bs
	 */
	public void setBindingSet(BindingSet bs) {

		// make sure the combinedBindings does not contain partial bindings.

		this.resultingBindingSet = bs;

		this.state = BINDINGSET_AVAILABLE;
	}

	public String toString() {
		return this.toString(0);
	}

	public String toString(int tabIndex) {
		StringBuilder sb = new StringBuilder();
		String stateText = getStateText(this.state);

		if (this.getState() == BINDINGSET_AVAILABLE) {
			sb.append(this.resultingBindingSet);
		} else {
			sb.append(this.rule.consequent).append(" <-");
			sb.append(stateText).append("- ");
			sb.append(this.rule.antecedent);
		}
		sb.append("\n");
		for (NodeAlt child : children.keySet()) {
			for (int i = 0; i < tabIndex + 1; i++)
				sb.append("\t");

			sb.append(child.toString(tabIndex + 1));
		}
		return sb.toString();
	}

	private String getStateText(int state2) {

		if (state2 == BINDINGSET_NOT_REQUESTED)
			return "x";
		if (state2 == BINDINGSET_REQUESTED)
			return "=";
		if (state2 == BINDINGSET_AVAILABLE)
			return "-";
		return null;
	}

	public RuleAlt getRule() {
		return this.rule;
	}

	private static Set<Set<TriplePattern>> createPowerSet(List<TriplePattern> graphPattern,
			Set<List<Boolean>> binarySubsets) {

		Set<Set<TriplePattern>> powerSet = new HashSet<>();
		Set<TriplePattern> subSet;
		for (List<Boolean> binaryList : binarySubsets) {
			subSet = new HashSet<>();
			for (int i = 0; i < binaryList.size(); i++) {
				Boolean b = binaryList.get(i);
				if (b) {
					subSet.add(graphPattern.get(i));
				}
			}
			powerSet.add(subSet);
		}
		return powerSet;
	}

	private static Set<List<Boolean>> createBinarySubsets(int setSize) {

		// what is minimum int that you can represent {@code setSize} bits? 0
		// what is maximum int that you can represent {@code setSize} bits? 2^setSize

		int start = 0;
		int end = 1 << setSize;
		Set<List<Boolean>> set = new HashSet<>(end);
		String endBin = Integer.toBinaryString(end);

		for (int i = start; i < end; i++) {
			String bin = toBinary(i, endBin.length() - 1);
			List<Boolean> bools = fromCharToBoolean(bin);
			set.add(bools);
		}

		return set;
	}

	private static String toBinary(int val, int len) {
		return Integer.toBinaryString((1 << len) | val).substring(1);

	}

	private static List<Boolean> fromCharToBoolean(String in) {
		List<Boolean> list = new ArrayList<>();

		for (int i = 0; i < in.length(); i++) {
			char charAt = in.charAt(i);
			if (charAt == '0') {
				list.add(Boolean.valueOf(false));
			} else if (charAt == '1') {
				list.add(Boolean.valueOf(true));
			} else {
				throw new IllegalArgumentException("The input string should only contains 0s or 1s and not: " + charAt);
			}
		}

		return list;
	}

}
