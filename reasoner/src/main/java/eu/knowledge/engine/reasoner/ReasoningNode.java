package eu.knowledge.engine.reasoner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.ReasoningNode;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.TaskBoard;
import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TripleVar;
import eu.knowledge.engine.reasoner.api.TripleVarBinding;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern.Variable;

/**
 * Represents the application of a rule in the search tree. A rule can be
 * applied in a backward and forward chaining fashion and note that in some
 * scenario's (like loops with transitive relations), a reasoning node might be
 * used backward and forward simultaneously.
 * 
 * @author nouwtb
 *
 */
public class ReasoningNode {

	/**
	 * All non-loop backwardChildren.
	 */
	private Map<ReasoningNode, Set<Match>> backwardChildren;

	/**
	 * All loop backwardChildren (i.e. backwardChildren that eventually loop back to
	 * this particular node).
	 */
	private Map<ReasoningNode, Set<Match>> loopChildren;

	private List<Rule> allRules;

	/**
	 * Goals are represented as rules with an antecedent without a consequent.
	 */
	private Rule rule;

	/**
	 * The backward chaining (BC) status of this reasoning node. Is the consequent
	 * bindingset already available, is it requested or not requested.
	 */
	private static final int BC_BINDINGSET_NOT_REQUESTED = 0, BC_BINDINGSET_REQUESTED = 1, BC_BINDINGSET_AVAILABLE = 2;

	/**
	 * The forward chaining (FC) status of this reasoning node.
	 */
	private static final int FC_BINDINGSET_NOT_REQUESTED = 0, FC_BINDINGSET_REQUESTED = 1, FC_BINDINGSET_AVAILABLE = 2;

	/**
	 * This is the bindingset that has been retrieved and is now ready to be used to
	 * continue the reasoning process.
	 */
	private BindingSet resultingBackwardBindingSet;

	/**
	 * Thisis the bindingset that has been retrieved and is now ready to be used to
	 * continue the reasoning process.
	 */
	private BindingSet resultingForwardBindingSet;

	/**
	 * This bcState keeps track of whether the procedural code (if there is one) to
	 * for example remotely access other KBs has already been requested or already
	 * available in the backward chaining (bc) behavior of this node. Note that the
	 * rule might also just convert the incoming bindings to the outgoing bindings
	 * without calling remote services.
	 */
	private int bcState = BC_BINDINGSET_NOT_REQUESTED;

	/**
	 * This fcState keeps track of whether the procedural code is already requested
	 * in the forward chaining behavior of this node, etc.
	 */
	private int fcState = FC_BINDINGSET_NOT_REQUESTED;

	/**
	 * Whether this node only accepts backwardChildren reasoning nodes that full
	 * match its antecedent. Note that setting this to true effectively disables the
	 * reasoning functionality and reduces this algorithm to a matching algorithm.
	 */
	private MatchStrategy matchStrategy;

	private ReasoningNode parent;

	/**
	 * Whether the plan should be forward or backward chaining. This is important,
	 * because the rules are inverted between those two strategies. Note that in
	 * some scenario's (i.e. when loops are detected) a backward chaining plan might
	 * include some forward chaining. Is there also a scenario in which a forward
	 * chaining plan includes some backward chaining?
	 */
	private boolean shouldPlanBackward;

	private Map<ReasoningNode, Set<Match>> forwardChildren;

	private TaskBoard taskboard;

	/**
	 * Construct a reasoning node and all backwardChildren.
	 * 
	 * Note that when this node detects a loop (i.e. the consequent of current rule
	 * matches the antecedent of the current rule, for example as is the case with
	 * the transitivity rule), it will create a special child to itself. This
	 * special child is used during backwards execution phase to start a forward
	 * execution phase because it is not as vulnerable for an infinite loop as the
	 * backward chaining method.
	 * 
	 * @param someRules
	 * @param aParent
	 * @param aRule
	 * @param aMatchStrategy
	 */
	public ReasoningNode(List<Rule> someRules, ReasoningNode aParent, Rule aRule, MatchStrategy aMatchStrategy,
			boolean aShouldPlanBackward, TaskBoard aTaskboard) {

		this.matchStrategy = aMatchStrategy;
		this.parent = aParent;
		this.allRules = someRules;
		this.rule = aRule;
		this.backwardChildren = new HashMap<ReasoningNode, Set<Match>>();
		this.forwardChildren = new HashMap<ReasoningNode, Set<Match>>();
		this.loopChildren = new HashMap<ReasoningNode, Set<Match>>();
		this.shouldPlanBackward = aShouldPlanBackward;
		this.taskboard = aTaskboard;

		if (shouldPlanBackward) {
			// generate backwardChildren
			if (!this.rule.antecedent.isEmpty()) {
				Map<Rule, Set<Match>> relevantRules = findRulesWithOverlappingConsequences(this.rule.antecedent,
						this.matchStrategy);
				ReasoningNode child;
				for (Map.Entry<Rule, Set<Match>> entry : relevantRules.entrySet()) {

					// some infinite loop detection (see transitivity test)
					ReasoningNode someNode;
					if ((someNode = this.ruleAlreadyOccurs(entry.getKey())) != null) {
						// loop detected: reuse the reasoning node.
						this.loopChildren.put(someNode, entry.getValue());
					} else {
						// create a new childnode
						child = new ReasoningNode(this.allRules, this, entry.getKey(), this.matchStrategy,
								this.shouldPlanBackward, this.taskboard);
						this.backwardChildren.put(child, entry.getValue());
					}
				}
			}

		} else if (!shouldPlanBackward) {
			// generate forward backwardChildren
			if (!this.rule.consequent.isEmpty()) {
				Map<Rule, Set<Match>> relevantRules = findRulesWithOverlappingAntecedents(this.rule.consequent,
						this.matchStrategy);
				ReasoningNode child;
				for (Map.Entry<Rule, Set<Match>> entry : relevantRules.entrySet()) {

					if ((child = this.ruleAlreadyOccurs(entry.getKey())) != null) {
						this.loopChildren.put(child, entry.getValue());
					} else {
						// create a new childnode
						child = new ReasoningNode(this.allRules, this, entry.getKey(), this.matchStrategy,
								this.shouldPlanBackward, this.taskboard);
						this.forwardChildren.put(child, entry.getValue());
					}
				}
			}

		}
	}

	/**
	 * Check recursively to the root whether the given rule already occurs. If it
	 * does, we create a loop to prevent an infinite loop.
	 * 
	 * @param aRule
	 * @return
	 */
	private ReasoningNode ruleAlreadyOccurs(Rule aRule) {

		if (this.rule.equals(aRule))
			return this;
		else if (this.parent != null)
			return this.parent.ruleAlreadyOccurs(aRule);
		else
			return null;
	}

	private Map<Rule, Set<Match>> findRulesWithOverlappingConsequences(Set<TriplePattern> aPattern,
			MatchStrategy aMatchStrategy) {

		assert aPattern != null;
		assert !aPattern.isEmpty();

		Set<Match> possibleMatches;
		Map<Rule, Set<Match>> overlappingRules = new HashMap<>();
		for (Rule r : this.allRules) {
			if (!(possibleMatches = r.consequentMatches(aPattern, aMatchStrategy)).isEmpty()) {
				overlappingRules.put(r, possibleMatches);
			}
		}
		assert overlappingRules != null;
		return overlappingRules;
	}

	private Map<Rule, Set<Match>> findRulesWithOverlappingAntecedents(Set<TriplePattern> aConsequentPattern,
			MatchStrategy aMatchStrategy) {
		assert aConsequentPattern != null;
		assert !aConsequentPattern.isEmpty();

		Set<Match> possibleMatches;
		Map<Rule, Set<Match>> overlappingRules = new HashMap<>();
		for (Rule r : this.allRules) {

			if (!(possibleMatches = r.antecedentMatches(aConsequentPattern, aMatchStrategy)).isEmpty()) {
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
	public BindingSet continueBackward(BindingSet bindingSet) {

		// send the binding set down the tree and combine the resulting bindingsets to
		// the actual result.
		if (this.bcState != BC_BINDINGSET_AVAILABLE) {

			boolean allChildBindingSetsAvailable = true;

			Set<ReasoningNode> someChildren = this.backwardChildren.keySet();

			// we transfer the incomingBindingSet (from the consequent) to the antecedent
			// bindingset (if it exists). This latter one is send to the different
			// backwardChildren.
			TripleVarBindingSet antecedentPredefinedBindings;
			antecedentPredefinedBindings = new TripleVarBindingSet(this.rule.antecedent);
			TripleVarBinding aTripleVarBinding;
			for (Binding b : bindingSet) {
				aTripleVarBinding = new TripleVarBinding(this.rule.antecedent, b);
				antecedentPredefinedBindings.add(aTripleVarBinding);
			}

			// include the allBindingSetsAvailable into the while condition, to process a
			// single node of the tree a time. This makes sure that the results from the
			// first child are send to the second child to check. Otherwise, they get
			// combined and end up in the result without anyone ever checking them.

			// TODO the order in which we iterate the backwardChildren is important. A
			// strategy like
			// first processing the backwardChildren that have the incoming variables in
			// their
			// consequent might be smart.
			TripleVarBindingSet combinedBindings = new TripleVarBindingSet(this.rule.antecedent);

			Iterator<ReasoningNode> someIter = someChildren.iterator();
			while (/* allBindingSetsAvailable && */someIter.hasNext()) {
				ReasoningNode child = someIter.next();
				Set<Match> childMatch = this.backwardChildren.get(child);

				// we can combine all different matches of this child's consequent into a single
				// bindingset. We do not want to send bindings with variables that do not exist
				// for that child.

				// TODO do we need the following two lines? Do we want to send bindings received
				// from other children to this child? Or do we just send the bindings that we
				// received as an argument of this method.
				TripleVarBindingSet preparedBindings1 = combinedBindings.getPartialBindingSet();
				TripleVarBindingSet preparedBindings2 = preparedBindings1.merge(antecedentPredefinedBindings);

				BindingSet childBindings = child.continueBackward(
						preparedBindings2.translate(child.rule.consequent, childMatch).toBindingSet());
				if (childBindings == null) {
					allChildBindingSetsAvailable = false;
				} else {
					TripleVarBindingSet childGraphBindingSet = childBindings.toGraphBindingSet(child.rule.consequent);

					// create powerset of graph pattern triples and use those to create additional
					// triplevarbindings.

					TripleVarBindingSet convertedChildGraphBindingSet = childGraphBindingSet
							.translate(this.rule.antecedent, invert(childMatch));

					// we do this expensive operation only on the overlapping part to improve
					// performance. If large graph patterns of our children only match with a single
					// triple on us, then we only generate additional bindings for this single
					// triple, which takes MUCH MUCH MUCH less time than generating them for the
					// full graph pattern.
					if (this.matchStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES))
						convertedChildGraphBindingSet = generateAdditionalTripleVarBindings(
								convertedChildGraphBindingSet);

					if (childMatch.size() > 1) {
						// the child matches in multiple ways, so we need to merge the bindingset with
						// itself to combine all ways that it matches.
						// TODO or do we want to translate per match and combine each translated
						// bindingset per match with each other?
						convertedChildGraphBindingSet = convertedChildGraphBindingSet
								.merge(convertedChildGraphBindingSet);
					}

					combinedBindings = combinedBindings.merge(convertedChildGraphBindingSet);
				}
			}

			if (allChildBindingSetsAvailable) {

				boolean finished = true;
				// process the loop backwardChildren
				someIter = this.loopChildren.keySet().iterator();
				while (someIter.hasNext()) {
					ReasoningNode child = someIter.next();
					// we use forward reasoning to prevent an infinite loop
					combinedBindings = keepOnlyFullGraphPatternBindings(this.rule.antecedent, combinedBindings);

					finished &= child.continueForward(combinedBindings.toBindingSet());
				}

				if (finished) {

					TripleVarBindingSet consequentAntecedentBindings;
					if (this.backwardChildren.isEmpty()) {
						consequentAntecedentBindings = bindingSet.toGraphBindingSet(this.rule.consequent);
					} else {
						consequentAntecedentBindings = keepOnlyCompatiblePatternBindings(
								bindingSet.toGraphBindingSet(this.rule.antecedent), combinedBindings);

						consequentAntecedentBindings = keepOnlyFullGraphPatternBindings(this.rule.antecedent,
								consequentAntecedentBindings);

					}

					if (true) {
						if (this.rule.antecedent.isEmpty() || !consequentAntecedentBindings.isEmpty()) {

							this.taskboard.addTask(this, consequentAntecedentBindings.toBindingSet());
							this.bcState = BC_BINDINGSET_REQUESTED;
						} else {
							this.resultingBackwardBindingSet = new BindingSet();
							this.bcState = BC_BINDINGSET_AVAILABLE;
							return this.resultingBackwardBindingSet;
						}
					} else {

						// call the handler directly, to make debugging easier.
						if (this.rule.antecedent.isEmpty() || !consequentAntecedentBindings.isEmpty()) {

							this.resultingBackwardBindingSet = this.rule.getBindingSetHandler()
									.handle(consequentAntecedentBindings.toBindingSet());
							this.bcState = BC_BINDINGSET_AVAILABLE;
						} else {
							this.resultingBackwardBindingSet = new BindingSet();
							this.bcState = BC_BINDINGSET_AVAILABLE;
						}
						return this.resultingBackwardBindingSet;

					}
				}
			}

			return null;
		} else {
			assert resultingBackwardBindingSet != null;

			// TODO internally the bindingset should contain bindings with variables where
			// instead of having a single entry per variable name, we need an entry per
			// variable occurrence. This is to make sure that different predicates going to
			// the same two variables are still correctly matched.
			// we start however, with the naive version that does not support this.
			return this.resultingBackwardBindingSet;
		}
	}

	/**
	 * Execute this rule in a forward manner using the given bindings.
	 * 
	 * @param someBindings
	 * @return
	 */
	public boolean continueForward(BindingSet bindingSet) {

		// the ordering is reversed, I think.
		// we first call the rule's bindingsethandler with the incoming bindingset (this
		// should go through the taskboard and requires an additional reasoning step)
		// the result should be compatible with the consequent of our rule
		// and we send it to the forward backwardChildren for further processing.

		boolean allFinished = true;
		if (this.fcState == FC_BINDINGSET_AVAILABLE) {
			// bindingset available, so we send it to our backwardChildren.
			Set<ReasoningNode> someChildren = this.forwardChildren.keySet();

			if (!this.resultingForwardBindingSet.isEmpty()) {
				TripleVarBindingSet consequentPredefinedBindings;
				consequentPredefinedBindings = new TripleVarBindingSet(this.rule.consequent);
				TripleVarBinding aTripleVarBinding;
				for (Binding b : this.resultingForwardBindingSet) {
					aTripleVarBinding = new TripleVarBinding(this.rule.consequent, b);
					consequentPredefinedBindings.add(aTripleVarBinding);
				}

				Iterator<ReasoningNode> someIter = someChildren.iterator();
				while (someIter.hasNext()) {
					ReasoningNode child = someIter.next();
					Set<Match> childMatch = this.forwardChildren.get(child);

					// we can combine all different matches of this child's consequent into a single
					// bindingset. We do not want to send bindings with variables that do not exist
					// for that child.

					TripleVarBindingSet translatedConsequentPredefinedBindings = consequentPredefinedBindings
							.translate(child.rule.antecedent, childMatch);

					TripleVarBindingSet mergedConsequentPredefinedBindings = translatedConsequentPredefinedBindings
							.merge(translatedConsequentPredefinedBindings);

					// TODO we probably need to keep track of what child has already finished,
					// because this child needs to be called again in the next call of this method.

					TripleVarBindingSet resultConsequentPredefinedBindings = keepOnlyFullGraphPatternBindings(
							mergedConsequentPredefinedBindings.getGraphPattern(), mergedConsequentPredefinedBindings);

					if (!resultConsequentPredefinedBindings.isEmpty())
						allFinished &= child.continueForward(resultConsequentPredefinedBindings.toBindingSet());
				}

				// process the loop backwardChildren
				someIter = this.loopChildren.keySet().iterator();
				while (someIter.hasNext()) {
					ReasoningNode child = someIter.next();

					Set<Match> childMatch = this.loopChildren.get(child);

					TripleVarBindingSet existing = bindingSet.toGraphBindingSet(this.rule.antecedent);

					if (this.matchStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES))
						existing = generateAdditionalTripleVarBindings(existing);

					TripleVarBindingSet existing3 = existing
							.merge(this.resultingForwardBindingSet.toGraphBindingSet(this.rule.consequent)
									.translate(child.rule.antecedent, invert(childMatch)));

					// we use forward reasoning to prevent an infinite loop
					existing = keepOnlyFullGraphPatternBindings(this.rule.antecedent, existing3);

					this.fcState = FC_BINDINGSET_NOT_REQUESTED;

					child.continueForward(existing.toBindingSet());
				}
			}
		} else {
			// bindingset not yet available, we need to make sure it does.
			allFinished = false;
			if (true) {
				this.taskboard.addTask(this, bindingSet);
				this.fcState = FC_BINDINGSET_REQUESTED;
			} else {
				this.resultingForwardBindingSet = this.rule.getBindingSetHandler().handle(bindingSet);
				this.fcState = FC_BINDINGSET_AVAILABLE;
			}
		}

		return allFinished;
	}

	private TripleVarBindingSet generateAdditionalTripleVarBindings(TripleVarBindingSet childGraphBindingSet) {

		// create powerset
		Set<TriplePattern> graphPattern = childGraphBindingSet.getGraphPattern();
		List<List<Boolean>> binarySubsets = createBinarySubsets(graphPattern.size());
		List<Set<TriplePattern>> gpPowerSet = createPowerSet(new ArrayList<TriplePattern>(graphPattern), binarySubsets);

		// create additional triplevarbindings
		TripleVarBindingSet newGraphBindingSet = new TripleVarBindingSet(graphPattern);
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
	private Set<TripleVarBinding> permutateTripleVarBinding(TripleVarBinding tvb, List<Set<TriplePattern>> powerSets) {

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
	private TripleVarBindingSet keepOnlyCompatiblePatternBindings(TripleVarBindingSet bindingSet,
			TripleVarBindingSet bindingSet2) {

		TripleVarBindingSet newBS = new TripleVarBindingSet(bindingSet2.getGraphPattern());

		for (TripleVarBinding b : bindingSet2.getBindings()) {

			if (!bindingSet.isEmpty()) {
				for (TripleVarBinding b2 : bindingSet.getBindings()) {

					if (!b2.isEmpty()) {

						if (!b.isConflicting(b2)) {
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

	private TripleVarBindingSet keepOnlyFullGraphPatternBindings(Set<TriplePattern> graphPattern,
			TripleVarBindingSet someBindings) {
		TripleVarBindingSet bs = new TripleVarBindingSet(someBindings.getGraphPattern());
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
			for (Variable variable : tp.getVariables()) {
				allTVs.add(new TripleVar(tp, variable));
			}
		}
		return allTVs;
	}

	public Set<Variable> getVars(Set<TriplePattern> aPattern) {
		Set<Variable> vars = new HashSet<Variable>();
		for (TriplePattern t : aPattern) {
			vars.addAll(t.getVariables());
		}
		return vars;
	}

	private int getState() {
		return this.bcState;
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

		if (bcState == BC_BINDINGSET_REQUESTED) {
			this.resultingBackwardBindingSet = bs;
			this.bcState = BC_BINDINGSET_AVAILABLE;
		} else if (fcState == FC_BINDINGSET_REQUESTED) {

			if (resultingForwardBindingSet != null) {
				this.resultingForwardBindingSet.addAll(bs);
			} else {
				this.resultingForwardBindingSet = bs;
			}
			this.fcState = FC_BINDINGSET_AVAILABLE;
		}
	}

	public String toString() {
		return this.toString(0);
	}

	public String toString(int tabIndex) {
		StringBuilder sb = new StringBuilder();
		String stateText = getStateText(this.bcState);

		if (this.getState() == BC_BINDINGSET_AVAILABLE) {
			sb.append(this.resultingBackwardBindingSet);
		} else {
			sb.append(this.rule.consequent).append(" <-");
			sb.append(stateText).append("- ");
			sb.append(this.rule.antecedent);
		}
		sb.append("\n");
		for (ReasoningNode child : backwardChildren.keySet()) {
			for (int i = 0; i < tabIndex + 1; i++)
				sb.append("\t");

			sb.append(child.toString(tabIndex + 1));
		}

		for (ReasoningNode child : forwardChildren.keySet()) {
			for (int i = 0; i < tabIndex + 1; i++)
				sb.append("\t");

			sb.append(child.toString(tabIndex + 1));
		}

		for (ReasoningNode loopChild : loopChildren.keySet()) {
			for (int i = 0; i < tabIndex + 1; i++)
				sb.append("\t");
			sb.append("loop").append("\n");
		}

		return sb.toString();
	}

	private String getStateText(int state2) {

		if (state2 == BC_BINDINGSET_NOT_REQUESTED)
			return "x";
		if (state2 == BC_BINDINGSET_REQUESTED)
			return "=";
		if (state2 == BC_BINDINGSET_AVAILABLE)
			return "-";
		return null;
	}

	public Rule getRule() {
		return this.rule;
	}

	private static List<Set<TriplePattern>> createPowerSet(List<TriplePattern> graphPattern,
			List<List<Boolean>> binarySubsets) {

		List<Set<TriplePattern>> powerSet = new ArrayList<>();
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

	private static List<List<Boolean>> createBinarySubsets(int setSize) {

		// what is minimum int that you can represent {@code setSize} bits? 0
		// what is maximum int that you can represent {@code setSize} bits? 2^setSize

		int start = 0;
		int end = 1 << setSize;
		List<List<Boolean>> set = new ArrayList<>(end);
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
