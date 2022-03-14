package eu.knowledge.engine.reasoner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.jena.graph.Node_Concrete;
import org.apache.jena.sparql.core.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TripleVar;
import eu.knowledge.engine.reasoner.api.TripleVarBinding;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

/**
 * Represents the application of a rule in the search tree. A rule can be
 * applied in a backward and forward chaining fashion and note that in some
 * scenario's (like loops with transitive relations or forward chaining), a
 * reasoning node might be using backward and forward simultaneously.
 * 
 * Note also that tree is actually not a good term, because in some situations
 * it is a graph.
 * 
 * @author nouwtb
 *
 */
public class ReasoningNode {

	/**
	 * The log facility of this class.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ReasoningNode.class);

	/**
	 * All non-loop neighbors that have matching consequences to this node's
	 * antecedent.
	 */
	private Map<ReasoningNode, Set<Match>> antecedentNeighbors;

	/**
	 * All neighbors that have matching antecedents to this node's consequent.
	 */
	private Map<ReasoningNode, Set<Match>> consequentNeighbors;

	/**
	 * All loop antecedent neighbors (i.e. antecedent neighbors that eventually loop
	 * back to this particular node).
	 */
	private Map<ReasoningNode, Set<Match>> loopAntecedentNeighbors;

	/**
	 * A cached mapping from triple patterns in the antecedent of this
	 * ReasoningNode's rule to all the antecedent neighbors that cover that
	 * particular triple pattern. We can use this to deal with knowledge gaps.
	 */
	private Map<TriplePattern, Set<ReasoningNode>> antecedentCoverageCache;

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
	 * continue the backward reasoning process. It represents the consequent graph
	 * pattern.
	 */
	private BindingSet fromBindingSetHandlerBackward;

	/**
	 * This is the bindingset that has been retrieved and is now ready to be used to
	 * continue the forward reasoning process. It represents the consequent graph
	 * pattern.
	 */
	private BindingSet fromBindingSetHandlerForward;

	/**
	 * These contains the bindingsets that were given to the bindingset handler (if
	 * any).
	 */
	private BindingSet toBindingSetHandlerBackward;
	private BindingSet toBindingSetHandlerForward;

	/**
	 * The start and end time of the execution of the bindingsethandler
	 */
	private Instant startTime;
	private Instant endTime;

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

	/**
	 * The TaskBoard used to place new tasks on. Tasks are applications of rules to
	 * a particular bindingset. These applications can involve procedural
	 * programming code which (in case of the TKE) contact other Knowledge Bases
	 * possibly over the network. Therefore we might need to aggregate some of the
	 * calls, before actually sending data over the network to reduce the network
	 * traffic and increase the performance. If the TaskBoard is @{code null}, the
	 * rules are immediately applied.
	 */
	private TaskBoard taskboard = null;

	public ReasoningNode(List<Rule> someRules, ReasoningNode aParent, Rule aRule, MatchStrategy aMatchStrategy,
			boolean aShouldPlanBackward) {
		this(someRules, aParent, aRule, aMatchStrategy, aShouldPlanBackward, null);
	}

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
		this.antecedentNeighbors = new HashMap<ReasoningNode, Set<Match>>();
		this.consequentNeighbors = new HashMap<ReasoningNode, Set<Match>>();
		this.loopAntecedentNeighbors = new HashMap<ReasoningNode, Set<Match>>();
		this.shouldPlanBackward = aShouldPlanBackward;
		this.taskboard = aTaskboard;

		// determine whether our parent matches us partially
		boolean ourAntecedentFullyMatchesParentConsequent = false;
		if (!shouldPlanBackward && parent != null) {
			ourAntecedentFullyMatchesParentConsequent = antecedentFullyMatchesConsequent(this.rule.antecedent,
					this.parent.getRule().consequent, aMatchStrategy);
		}

		if (!shouldPlanBackward) {
			// generate consequent neighbors
			if (!this.rule.consequent.isEmpty()) {
				Map<Rule, Set<Match>> relevantRules = findRulesWithOverlappingAntecedents(this.rule.consequent,
						this.matchStrategy);
				ReasoningNode child;
				for (Map.Entry<Rule, Set<Match>> entry : relevantRules.entrySet()) {

					if ((child = this.ruleAlreadyOccurs(entry.getKey())) != null) {
						this.loopAntecedentNeighbors.put(child, entry.getValue());
					} else {
						// create a new neighbor node
						child = new ReasoningNode(this.allRules, this, entry.getKey(), this.matchStrategy,
								this.shouldPlanBackward, this.taskboard);
						this.consequentNeighbors.put(child, entry.getValue());
					}
				}
			}
		}

		// generate antecedent neighbors
		// note that in some scenario's antecedent neighbors are also necessary in a
		// forward scenario
		if (!this.rule.antecedent.isEmpty()) {
			Map<Rule, Set<Match>> relevantRules = findRulesWithOverlappingConsequences(this.rule.antecedent,
					this.matchStrategy);
			ReasoningNode child;
			for (Map.Entry<Rule, Set<Match>> entry : relevantRules.entrySet()) {

				// some infinite loop detection (see transitivity test)
				ReasoningNode someNode;
				if (this.parent != null && this.parent.getRule().equals(entry.getKey())) {
					// skip our antecedent neighbor that triggered this node.
				} else if ((someNode = this.ruleAlreadyOccurs(entry.getKey())) != null) {
					// loop detected: reuse the reasoning node.
					this.loopAntecedentNeighbors.put(someNode, entry.getValue());
				} else if (shouldPlanBackward || !ourAntecedentFullyMatchesParentConsequent) {
					// create a new neihgbor node
					child = new ReasoningNode(this.allRules, this, entry.getKey(), this.matchStrategy, true,
							this.taskboard);
					this.antecedentNeighbors.put(child, entry.getValue());

				}
			}
		}
	}

	/**
	 * Checks whether the given antecedent fully matches the given consequent. Note
	 * that if the antecedent is a subset of the consequent this method also return
	 * true.
	 * 
	 * @param consequent
	 * @param antecedent
	 * @return
	 */
	private boolean antecedentFullyMatchesConsequent(Set<TriplePattern> antecedent, Set<TriplePattern> consequent,
			MatchStrategy aMatchStrategy) {

		assert !antecedent.isEmpty();
		assert !consequent.isEmpty();

		if (antecedent.size() > consequent.size())
			return false;

		Set<Match> matches = Rule.matches(antecedent, consequent, aMatchStrategy);

		for (Match m : matches) {
			// check if there is a match that is full
			boolean allFound = true;
			for (TriplePattern tp : antecedent) {
				boolean foundOne = false;
				for (Map.Entry<TriplePattern, TriplePattern> entry : m.getMatchingPatterns().entrySet()) {
					if (entry.getValue().findMatches(tp) != null) {
						foundOne = true;
					}
				}
				allFound &= foundOne;
			}

			if (allFound)
				return true;
		}

		return false;
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

	public BindingSet continueBackward(BindingSet bindingSet) {

		TripleVarBindingSet tvbs;
		if (this.rule.consequent.isEmpty())
			tvbs = continueBackward(bindingSet.toGraphBindingSet(this.rule.antecedent));
		else
			tvbs = continueBackward(bindingSet.toGraphBindingSet(this.rule.consequent));

		if (tvbs == null)
			return null;
		else
			return tvbs.toBindingSet();
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
	protected TripleVarBindingSet continueBackward(TripleVarBindingSet bindingSet) {

		// send the binding set down the tree and combine the resulting bindingsets to
		// the actual result.
		if (this.bcState != BC_BINDINGSET_AVAILABLE) {

			if (!this.hasKnowledgeGaps()) {

				boolean allChildBindingSetsAvailable = true;

				Set<ReasoningNode> someNeighbors = this.antecedentNeighbors.keySet();

				// store the results of the children separately and afterwards combine them.
				Set<TripleVarBindingSet> childResults = new HashSet<>();

				// TODO the order in which we iterate the backwardChildren is important. A
				// strategy like first processing the backwardChildren that have the incoming
				// variables in their consequent might be smart.

				Iterator<ReasoningNode> someIter = someNeighbors.iterator();
				while (someIter.hasNext()) {
					ReasoningNode neighbor = someIter.next();
					Set<Match> neighborMatches = this.antecedentNeighbors.get(neighbor);

					// we can combine all different matches of this child's consequent into a single
					// bindingset. We do not want to send bindings with variables that do not exist
					// for that child.

					// TODO do we need the following two lines? Do we want to send bindings received
					// from other children to this child? Or do we just send the bindings that we
					// received as an argument of this method.
//				TripleVarBindingSet preparedBindings1 = combinedBindings.getPartialBindingSet();
//				TripleVarBindingSet preparedBindings2 = preparedBindings1.merge(antecedentPredefinedBindings);

					TripleVarBindingSet neighborBindings = neighbor
							.continueBackward(bindingSet.translate(neighbor.rule.consequent, neighborMatches));
					if (neighborBindings == null) {
						allChildBindingSetsAvailable = false;
					} else {
						TripleVarBindingSet neighborGraphBindingSet = neighborBindings;

						// create powerset of graph pattern triples and use those to create additional
						// triplevarbindings.

						TripleVarBindingSet convertedNeighborTripleVarBindingSet = neighborGraphBindingSet
								.translate(this.rule.antecedent, invert(neighborMatches));

						// we do this expensive operation only on the overlapping part to improve
						// performance. If large graph patterns of our children only match with a single
						// triple on us, then we only generate additional bindings for this single
						// triple, which takes MUCH MUCH MUCH less time than generating them for the
						// full graph pattern.
						if (this.matchStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES))
							convertedNeighborTripleVarBindingSet = generateAdditionalTripleVarBindings(
									convertedNeighborTripleVarBindingSet);

						if (neighborMatches.size() > 1) {
							// the child matches in multiple ways, so we need to merge the bindingset with
							// itself to combine all ways that it matches.
							// TODO or do we want to translate per match and combine each translated
							// bindingset per match with each other?
							convertedNeighborTripleVarBindingSet = convertedNeighborTripleVarBindingSet
									.merge(convertedNeighborTripleVarBindingSet);
						}

						childResults.add(convertedNeighborTripleVarBindingSet);
					}
				}

				TripleVarBindingSet combinedBindings = new TripleVarBindingSet(this.rule.antecedent);
				if (allChildBindingSetsAvailable) {

					// combine the results from all the children.
					for (TripleVarBindingSet childBS : childResults) {
						combinedBindings = combinedBindings.merge(childBS);
					}

					boolean finished = true;
					// process the loop backwardChildren
					someIter = this.loopAntecedentNeighbors.keySet().iterator();
					while (someIter.hasNext()) {
						ReasoningNode child = someIter.next();
						// we use forward reasoning to prevent an infinite loop
						combinedBindings = keepOnlyFullGraphPatternBindings(this.rule.antecedent, combinedBindings);

						finished &= child.continueForward(combinedBindings);
					}

					if (finished) {

						// TODO this code is a bit shady. If we have an antecedent, we take the combined
						// bindings from all our antecedent neighbors and only keep those that are
						// compatible with the consequent bindings? What if the antecedent and
						// consequent graph patterns of this reasoningnode do not have any variables in
						// common?
						TripleVarBindingSet consequentAntecedentBindings;
						if (this.antecedentNeighbors.isEmpty()) {
							consequentAntecedentBindings = bindingSet; // send the consequent binding set
						} else {
							consequentAntecedentBindings = keepOnlyCompatiblePatternBindings(
									bindingSet.toBindingSet().toGraphBindingSet(this.rule.antecedent),
									combinedBindings);

							consequentAntecedentBindings = keepOnlyFullGraphPatternBindings(this.rule.antecedent,
									consequentAntecedentBindings);

						}

						this.toBindingSetHandlerBackward = consequentAntecedentBindings.toBindingSet();
						if (this.taskboard != null) {
							if (this.rule.antecedent.isEmpty() || !this.toBindingSetHandlerBackward.isEmpty()) {

								this.taskboard.addTask(this, this.toBindingSetHandlerBackward);
								this.bcState = BC_BINDINGSET_REQUESTED;
							} else {
								this.startTime = Instant.now();
								this.fromBindingSetHandlerBackward = new BindingSet();
								this.endTime = Instant.now();
								this.bcState = BC_BINDINGSET_AVAILABLE;
								return this.fromBindingSetHandlerBackward.toGraphBindingSet(this.rule.consequent);
							}
						} else {

							// call the handler directly because taskboard is null. This makes debugging
							// easier.
							if (this.rule.antecedent.isEmpty() || !this.toBindingSetHandlerBackward.isEmpty()) {

								try {
									startTime = Instant.now();
									this.fromBindingSetHandlerBackward = this.rule.getBindingSetHandler()
											.handle(this.toBindingSetHandlerBackward).get();
									endTime = Instant.now();
									this.bcState = BC_BINDINGSET_AVAILABLE;
								} catch (InterruptedException | ExecutionException e) {
									LOG.error("Handling a bindingset should not fail.", e);
								}
							} else {
								this.startTime = Instant.now();
								this.fromBindingSetHandlerBackward = new BindingSet();
								this.endTime = Instant.now();
								this.bcState = BC_BINDINGSET_AVAILABLE;
							}
							return this.fromBindingSetHandlerBackward.toGraphBindingSet(this.rule.consequent);

						}
					}
				}
			} else {
				// has knowledge gaps -> return empty
				startTime = Instant.now();
				endTime = Instant.now();
				this.fromBindingSetHandlerBackward = new BindingSet();
				this.bcState = BC_BINDINGSET_AVAILABLE;
				return this.fromBindingSetHandlerBackward.toGraphBindingSet(this.rule.consequent);
			}

			return null;
		} else {
			assert fromBindingSetHandlerBackward != null;

			// TODO internally the bindingset should contain bindings with variables where
			// instead of having a single entry per variable name, we need an entry per
			// variable occurrence. This is to make sure that different predicates going to
			// the same two variables are still correctly matched.
			// we start however, with the naive version that does not support this.
			if (this.rule.consequent.isEmpty())
				return this.fromBindingSetHandlerBackward.toGraphBindingSet(this.rule.antecedent);
			else
				return this.fromBindingSetHandlerBackward.toGraphBindingSet(this.rule.consequent);
		}
	}

	public boolean continueForward(BindingSet bindingSet) {
		// TODO this is not very elegant. This is caused by unclarity on the definition
		// of a bindingset handler and which binding it should receive.
		if (this.rule.antecedent.isEmpty())
			return continueForward(bindingSet.toGraphBindingSet(this.rule.consequent));
		else
			return continueForward(bindingSet.toGraphBindingSet(this.rule.antecedent));
	}

	/**
	 * Execute this rule in a forward manner using the given bindings.
	 * 
	 * @param someBindings
	 * @return
	 */
	protected boolean continueForward(TripleVarBindingSet bindingSet) {

		// the ordering is reversed, I think.
		// we first call the rule's bindingsethandler with the incoming bindingset (this
		// should go through the taskboard and requires an additional reasoning step)
		// the result should be compatible with the consequent of our rule
		// and we send it to the forward backwardChildren for further processing.

		boolean allFinished = true;
		BindingSet resultAntecedentBindings = new BindingSet();
		if (this.fcState != FC_BINDINGSET_AVAILABLE) {
			boolean allNeighborBindingSetsAvailable = true;
			if (!this.rule.antecedent.isEmpty()) {

				TripleVarBindingSet antecedentPredefinedBindings = bindingSet;

				// antecedent bindingset not yet available, we need to make sure it does.
				// process antecedent neighbors as well, since we might need them to get a
				// full bindingset.

				Set<ReasoningNode> someAntecedentNeighbors = this.antecedentNeighbors.keySet();
				Set<TripleVarBindingSet> neighborResults = new HashSet<>();
				for (ReasoningNode neighbor : someAntecedentNeighbors) {
					Set<Match> neighborMatches = this.antecedentNeighbors.get(neighbor);

					TripleVarBindingSet neighborBindings = neighbor
							.continueBackward(new TripleVarBindingSet(neighbor.rule.consequent)); // TODO include
																									// bindings
					if (neighborBindings == null) {
						allNeighborBindingSetsAvailable = false;
					} else {
						TripleVarBindingSet neighborTripleVarBindingSet = neighborBindings;

						// create powerset of graph pattern triples and use those to create additional
						// triplevarbindings.

						TripleVarBindingSet convertedNeighborTripleVarBindingSet = neighborTripleVarBindingSet
								.translate(this.rule.antecedent, invert(neighborMatches));

						// we do this expensive operation only on the overlapping part to improve
						// performance. If large graph patterns of our neighbors only match with a
						// single
						// triple on us, then we only generate additional bindings for this single
						// triple, which takes MUCH MUCH MUCH less time than generating them for the
						// full graph pattern.
						if (this.matchStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES))
							convertedNeighborTripleVarBindingSet = generateAdditionalTripleVarBindings(
									convertedNeighborTripleVarBindingSet);

						if (neighborMatches.size() > 1) {
							// the child matches in multiple ways, so we need to merge the bindingset with
							// itself to combine all ways that it matches or do we want to translate per
							// match and combine each translated bindingset per match with each other?
							convertedNeighborTripleVarBindingSet = convertedNeighborTripleVarBindingSet
									.merge(convertedNeighborTripleVarBindingSet);
						}

						neighborResults.add(convertedNeighborTripleVarBindingSet);
					}
				}

				if (allNeighborBindingSetsAvailable) {
					// combine all results from antecedent neighbors
					TripleVarBindingSet antecedentBindings = new TripleVarBindingSet(this.rule.antecedent);
					for (TripleVarBindingSet neighborBS : neighborResults) {
						antecedentBindings = antecedentBindings.merge(neighborBS);
					}

					TripleVarBindingSet mergedTripleVarBindings = antecedentBindings
							.merge(antecedentPredefinedBindings);

					// we moved the removal of incomplete bindings to just before adding them to the
					// taskboard.
					// previously it already happened in the parent node.
					resultAntecedentBindings = keepOnlyFullAndCompatibleGraphPatternBindings(
							mergedTripleVarBindings.getGraphPattern(), mergedTripleVarBindings,
							antecedentPredefinedBindings).toBindingSet();
				} else {
					allFinished = false;
				}
			} else {
				resultAntecedentBindings = bindingSet.toBindingSet();
			}

			if (allNeighborBindingSetsAvailable) {
				allFinished = false;

				this.toBindingSetHandlerForward = resultAntecedentBindings;

				if (this.taskboard != null) {
					this.taskboard.addTask(this, this.toBindingSetHandlerForward);
					this.fcState = FC_BINDINGSET_REQUESTED;
				} else {
					try {
						this.startTime = Instant.now();
						this.fromBindingSetHandlerForward = this.rule.getBindingSetHandler()
								.handle(this.toBindingSetHandlerForward).get();
						this.endTime = Instant.now();
					} catch (InterruptedException | ExecutionException e) {
						LOG.error("Handling a bindingset should not fail.", e);
					}
					this.fcState = FC_BINDINGSET_AVAILABLE;
				}
			}
		} else if (this.fcState == FC_BINDINGSET_AVAILABLE) {
			// bindingset available, so we send it to our consequent neighbors.
			Set<ReasoningNode> someNeighbors = this.consequentNeighbors.keySet();

			if (!this.fromBindingSetHandlerForward.isEmpty()) {
				TripleVarBindingSet consequentPredefinedBindings;
				consequentPredefinedBindings = new TripleVarBindingSet(this.rule.consequent);
				TripleVarBinding aTripleVarBinding;
				for (Binding b : this.fromBindingSetHandlerForward) {
					aTripleVarBinding = new TripleVarBinding(this.rule.consequent, b);
					consequentPredefinedBindings.add(aTripleVarBinding);
				}

				Iterator<ReasoningNode> someIter = someNeighbors.iterator();
				while (someIter.hasNext()) {
					ReasoningNode child = someIter.next();
					Set<Match> childMatch = this.consequentNeighbors.get(child);

					// we can combine all different matches of this neighbor's consequent into a
					// single
					// bindingset. We do not want to send bindings with variables that do not exist
					// for that child.

					TripleVarBindingSet translatedConsequentPredefinedBindings = consequentPredefinedBindings
							.translate(child.rule.antecedent, childMatch);

					TripleVarBindingSet mergedConsequentPredefinedBindings = translatedConsequentPredefinedBindings
							.merge(translatedConsequentPredefinedBindings);

					// TODO we probably need to keep track of what child has already finished,
					// because this child needs to be called again in the next call of this method.

//					TripleVarBindingSet resultConsequentPredefinedBindings = keepOnlyFullGraphPatternBindings(
//							mergedConsequentPredefinedBindings.getGraphPattern(), mergedConsequentPredefinedBindings);

					if (!mergedConsequentPredefinedBindings.isEmpty())
						allFinished &= child.continueForward(mergedConsequentPredefinedBindings);
				}

				// process the loop antecedent neighbors
				someIter = this.loopAntecedentNeighbors.keySet().iterator();
				while (someIter.hasNext()) {
					ReasoningNode child = someIter.next();

					Set<Match> childMatch = this.loopAntecedentNeighbors.get(child);

					TripleVarBindingSet existing = bindingSet;

					if (this.matchStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES))
						existing = generateAdditionalTripleVarBindings(existing);

					TripleVarBindingSet existing3 = existing
							.merge(this.fromBindingSetHandlerForward.toGraphBindingSet(this.rule.consequent)
									.translate(child.rule.antecedent, invert(childMatch)));

					// we use forward reasoning to prevent an infinite loop
					existing = keepOnlyFullGraphPatternBindings(this.rule.antecedent, existing3);

					this.fcState = FC_BINDINGSET_NOT_REQUESTED;

					allFinished = child.continueForward(existing.toBindingSet());
				}
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
				if (!newTVB.isEmpty())
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

	/**
	 * This method not only removes any partial bindings, but also removes full
	 * bindings that are incompatible with (i.e. do not incorporate) the incoming
	 * bindingsSet of the forwardChaining method. This prevents the reasoner from
	 * including all kind of data not coming from the rule that triggered the
	 * reasoning action to be included in the bindingset.
	 * 
	 * TODO merge this with
	 * {@link ReasoningNode#keepOnlyCompatiblePatternBindings(TripleVarBindingSet, TripleVarBindingSet)}?
	 * 
	 * @param graphPattern
	 * @param someBindings
	 * @return
	 */
	private TripleVarBindingSet keepOnlyFullAndCompatibleGraphPatternBindings(Set<TriplePattern> graphPattern,
			TripleVarBindingSet someBindings, TripleVarBindingSet someIncomingBindings) {

		TripleVarBindingSet bs = new TripleVarBindingSet(someBindings.getGraphPattern());
		for (TripleVarBinding b : someBindings.getBindings()) {
			if (isFullBinding(graphPattern, b) && isCompatible(b, someIncomingBindings)) {
				bs.add(new TripleVarBinding(b));
			}
		}
		return bs;
	}

	private boolean isCompatible(TripleVarBinding b, TripleVarBindingSet someIncomingBindings) {

		Node_Concrete value;
		for (TripleVarBinding tripleVarB : someIncomingBindings.getBindings()) {

			boolean allAreAvailable = true;

			for (TripleVar tv : tripleVarB.getTripleVars()) {
				allAreAvailable &= ((value = b.get(tv)) != null && value.equals(tripleVarB.get(tv)));
			}

			if (allAreAvailable)
				return true;
		}
		return false;
	}

	private boolean isFullBinding(Set<TriplePattern> graphPattern, TripleVarBinding b) {
		return b.keySet().containsAll(getTripleVars(graphPattern));
	}

	public Set<TripleVar> getTripleVars(Set<TriplePattern> aPattern) {
		Set<TripleVar> allTVs = new HashSet<>();
		for (TriplePattern tp : aPattern) {
			for (Var variable : tp.getVariables()) {
				allTVs.add(new TripleVar(tp, variable));
			}
		}
		return allTVs;
	}

	public Set<Var> getVars(Set<TriplePattern> aPattern) {
		Set<Var> vars = new HashSet<Var>();
		for (TriplePattern t : aPattern) {
			vars.addAll(t.getVariables());
		}
		return vars;
	}

	private int getBackwardState() {
		return this.bcState;
	}

	private int getForwardState() {
		return this.fcState;
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
	 * @param instant
	 * @param aStartTime
	 */
	public void setBindingSet(BindingSet bs, Instant aStartTime, Instant anEndTime) {

		this.startTime = aStartTime;
		this.endTime = anEndTime;
		if (bcState == BC_BINDINGSET_REQUESTED) {
			this.fromBindingSetHandlerBackward = bs;
			this.bcState = BC_BINDINGSET_AVAILABLE;
		} else if (fcState == FC_BINDINGSET_REQUESTED) {

			if (fromBindingSetHandlerForward != null) {
				this.fromBindingSetHandlerForward.addAll(bs);
			} else {
				this.fromBindingSetHandlerForward = bs;
			}
			this.fcState = FC_BINDINGSET_AVAILABLE;
		}
	}

	public String toString() {
		return this.toString(0);
	}

	public String toString(int tabIndex) {
		StringBuilder sb = new StringBuilder();

		if (this.getBackwardState() == BC_BINDINGSET_AVAILABLE) {
			sb.append(this.fromBindingSetHandlerBackward);
		} else if (this.getForwardState() == FC_BINDINGSET_AVAILABLE) {
			sb.append(this.fromBindingSetHandlerForward);
		} else {
			if (this.shouldPlanBackward) {
				String stateText = getStateText(this.bcState);
				sb.append(this.rule.consequent).append(" <-");
				sb.append(stateText).append("- ");
				sb.append(this.rule.antecedent);
			} else {
				String stateText = getStateText(this.fcState);
				sb.append(this.rule.antecedent);
				sb.append(" -").append(stateText).append("-> ");
				sb.append(this.rule.consequent);
			}
		}
		sb.append("\n");
		for (ReasoningNode neighbor : antecedentNeighbors.keySet()) {
			for (int i = 0; i < tabIndex + 1; i++)
				sb.append("\t");

			sb.append(neighbor.toString(tabIndex + 1));
		}

		for (ReasoningNode neighbor : consequentNeighbors.keySet()) {
			for (int i = 0; i < tabIndex + 1; i++)
				sb.append("\t");

			sb.append(neighbor.toString(tabIndex + 1));
		}

		for (ReasoningNode loopNeighbor : loopAntecedentNeighbors.keySet()) {
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

	/**
	 * @return The time the BindingSetHandler was started.
	 */
	public Instant getStartTime() {
		return this.startTime;
	}

	/**
	 * 
	 * @return The time the BindingSetHandler finished.
	 */
	public Instant getEndTime() {
		return this.endTime;
	}

	public BindingSet getBindingSetToHandler() {
		if (this.shouldPlanBackward) {
			return this.toBindingSetHandlerBackward;
		} else {
			return this.toBindingSetHandlerForward;
		}
	}

	public BindingSet getBindingSetFromHandler() {
		if (this.shouldPlanBackward) {
			return this.fromBindingSetHandlerBackward;
		} else {
			return this.fromBindingSetHandlerForward;
		}
	}

	public Map<ReasoningNode, Set<Match>> getAntecedentNeighbors() {
		return this.antecedentNeighbors;
	}

	public Map<ReasoningNode, Set<Match>> getConsequentNeighbors() {
		return this.consequentNeighbors;
	}

	public Map<TriplePattern, Set<ReasoningNode>> findAntecedentCoverage() {
		if (this.antecedentCoverageCache == null) {
			antecedentCoverageCache = new HashMap<>();
			// TODO find the coverage
			Set<ReasoningNode> coveringNodes;
			for (TriplePattern tp : this.rule.antecedent) {
				coveringNodes = new HashSet<>();
				antecedentCoverageCache.put(tp, coveringNodes);

				for (Entry<ReasoningNode, Set<Match>> entry : this.antecedentNeighbors.entrySet()) {
					for (Match m : entry.getValue()) {
						if (m.getMatchingPatterns().keySet().contains(tp)) {
							coveringNodes.add(entry.getKey());
							break; // where does this break from?
						}
					}
				}

			}
		}
		return this.antecedentCoverageCache;
	}

	private boolean hasKnowledgeGaps() {
		boolean hasGaps = false;
		Map<TriplePattern, Set<ReasoningNode>> nodeCoverage = this.findAntecedentCoverage();

		for (Entry<TriplePattern, Set<ReasoningNode>> entry : nodeCoverage.entrySet()) {
			if (entry.getValue().isEmpty()) {
				hasGaps = true;
				break;
			}
		}
		return hasGaps;

	}

}
