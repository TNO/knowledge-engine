package eu.knowledge.engine.reasoner.rulenode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.BaseRule.CombiMatch;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.api.TripleNode;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TripleVarBinding;
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
	private final Map<BaseRule, Map<Match, TripleVarBindingSet>> neighborBindingSet = new HashMap<>();

	/**
	 * This combi matches set need to be initialized <strong>after</strong> the
	 * constructor has been called.
	 */
	private Set<CombiMatch> combiMatches = null;
	private final Set<TriplePattern> graphPattern;

	/**
	 * Keep a cache and see if it improves performance.
	 */
	private TripleVarBindingSet cache;

	public BindingSetStore(Set<TriplePattern> aGraphPattern, Set<RuleNode> someNeighbors) {
		this.graphPattern = aGraphPattern;
		this.neighbors = someNeighbors;
	}

	/**
	 * Register the given {@code aBindingSet} to this BindingSetStore as coming from
	 * neighbor {@code aNeighbor}
	 * 
	 * @param aNeighbor       the neighbor who brought this bindingset.
	 * @param someBindingSets the bindingset brought by this neighbor.
	 * @return whether adding this bindingset changed anything. I.e. this particular
	 *         neighbor did not yet give a bindingset or this bindingset is
	 *         different from the previous one.
	 */
	public boolean add(RuleNode aNeighbor, Map<Match, TripleVarBindingSet> someBindingSets) {
		assert someBindingSets != null;
		assert neighbors.contains(aNeighbor);

		Map<Match, TripleVarBindingSet> previousBindingSet = this.neighborBindingSet.put(aNeighbor.getRule(),
				someBindingSets);

		boolean changed = previousBindingSet == null || !previousBindingSet.equals(someBindingSets);

		if (changed)
			this.cache = null;

		return changed;
	}

	public boolean haveAllNeighborsContributed() {
		return this.neighborBindingSet.keySet()
				.containsAll(neighbors.stream().map(x -> x.getRule()).collect(Collectors.toSet()));
	}

	// TODO: It feels ugly to do this.
	public boolean haveAllNeighborsContributedExcept(Set<RuleNode> nodes) {
		var allNeighboursWithException = new HashSet<BaseRule>();
		allNeighboursWithException.addAll(neighbors.stream().map(x -> x.getRule()).collect(Collectors.toSet()));
		allNeighboursWithException.removeAll(nodes.stream().map(x -> x.getRule()).collect(Collectors.toSet()));
		return this.neighborBindingSet.keySet().containsAll(allNeighboursWithException);
	}

	/**
	 * Translates the given TripleVarBindingSets from neighbors to a single
	 * TripleVarBindingSet using the given combi matches. Because the combi matches
	 * are very specific to how they should be formed, using this information should
	 * speed up the binding set merging process considerably.
	 * 
	 * @param aGraphPattern    The graph pattern of the binding set that is created.
	 * @param someCombiMatches The CombiMatches of the node.
	 * @param someNeighborBS   The already translated binding sets from all
	 *                         neighbors of a node per match.
	 * @return A TripleVarBindingSet that consists of only valid (according to the
	 *         combimatches) bindings.
	 */
	private TripleVarBindingSet translateWithCombiMatches(Set<TriplePattern> aGraphPattern,
			Set<CombiMatch> someCombiMatches, Map<BaseRule, Map<Match, TripleVarBindingSet>> someNeighborBS) {
		var combinedTVBS = new TripleVarBindingSet(aGraphPattern);

		for (CombiMatch cMatch : someCombiMatches) {
			// keep separate binding set per combi match
			var cMatchTVBS = new TripleVarBindingSet(aGraphPattern);

			for (Entry<BaseRule, Set<Match>> cEntry : cMatch.entrySet()) {

				BaseRule aNeighborRule = cEntry.getKey();

				Map<Match, TripleVarBindingSet> matchToBS = someNeighborBS.get(aNeighborRule);

				if (matchToBS != null) {
					for (Match cSingleMatch : cEntry.getValue()) {
						TripleVarBindingSet tvbs = matchToBS.get(cSingleMatch);

						if (tvbs != null)
							cMatchTVBS.addAll(cMatchTVBS.combine(tvbs).getBindings());
					}
				}
			}

			// addAll instead of merge, because different combi matches do not need to be
			// combined.
			combinedTVBS.addAll(cMatchTVBS.getBindings());
		}

		return combinedTVBS;

	}

	/**
	 * Sets the combi matches for this binding set store. Combi matches contain a
	 * combination of matches with neighboring rules that together form a single
	 * match for the rule node of which this bindingset store is parts.
	 * 
	 * @param someCombiMatches The combi matches for this store.
	 */
	public void setCombiMatches(Set<CombiMatch> someCombiMatches) {
		this.combiMatches = someCombiMatches;
	}

	/**
	 * @return the bindingset with the combined bindingset of all neighbors.
	 */
	public TripleVarBindingSet get() {

		if (this.cache != null) {
			return this.cache;
		}

		// unfortunately, due to the asymmetry introduced with combi matches (i.e. combi
		// matches are only available at the antecedent/consequent neighbours, but not
		// at the consequent/antecedent neighbors depending on backward/forward
		// reasoning) we use the older (slower) method when there are no combi matches.
		if (this.combiMatches != null)
			this.cache = this.translateWithCombiMatches(this.graphPattern, this.combiMatches, this.neighborBindingSet);
		else {
			TripleVarBindingSet combinedBS = new TripleVarBindingSet(graphPattern);

			for (TripleVarBindingSet bs : this.neighborBindingSet.values().stream().map(x -> x.values())
					.flatMap(x -> x.stream()).collect(Collectors.toSet())) {
				combinedBS = combinedBS.merge(bs);
			}

			// NOTE: we merge the bindings with themselves here (when the bindings
			// 'leave' the store), but it may be better to do it when they enter the
			// store, or when they get translated/matched.
			this.cache = combinedBS.merge(combinedBS);
		}

		return this.cache;
	}

	@Override
	public String toString() {
		return "BindingSetStore [neighborBindingSet=" + neighborBindingSet + "]";
	}

	private String getName(BaseRule r) {
		if (r.getName().isEmpty()) {
			return r.toString();
		} else {
			return r.getName();
		}
	}

	public void printDebuggingTable() {
		StringBuilder table = new StringBuilder();

		List<BaseRule> allNeighbors = new ArrayList<>(
				neighbors.stream().map(x -> x.getRule()).collect(Collectors.toSet()));

		// header row
		int count = 0;
		table.append("| Graph Pattern |");
		count++;
		for (BaseRule neighbor : allNeighbors) {

			if (this.neighborBindingSet.containsKey(neighbor)) {
				for (int i = 0; i < combine(this.neighborBindingSet.get(neighbor)).getBindings().size(); i++) {
					table.append(this.getName(neighbor) + "-" + i).append(" | ");
					count++;
				}
			} else {
				table.append(this.getName(neighbor)).append(" | ");
				count++;
			}
		}

		table.append("\n");

		// separator row
		table.append("|");
		for (int i = 0; i < count; i++) {
			table.append("-------|");
		}
		table.append("\n");

		// content rows
		for (TriplePattern tp : this.graphPattern) {
			// triple pattern
			table.append(" | ").append(tp.toString()).append(" | ");

			// bindings
			for (BaseRule neigh : allNeighbors) {

				TripleVarBindingSet tvbs = combine(this.neighborBindingSet.get(neigh));

				if (tvbs != null) {
					for (TripleVarBinding tvb : tvbs.getBindings()) {
						Set<TripleNode> nodes = tvb.getTripleNodes(tp);
						if (!nodes.isEmpty()) {

							Node subject = null;
							Node predicate = null;
							Node object = null;

							for (TripleNode tn : nodes) {
								if (tn.nodeIdx == 0) {
									subject = tvb.get(tn);
								} else if (tn.nodeIdx == 1)
									predicate = tvb.get(tn);
								else if (tn.nodeIdx == 2)
									object = tvb.get(tn);
							}

//							tp.getSubject().isVariable() ?

							table.append(subject != null ? subject : formatNode(tp.getSubject())).append(" ");
							table.append(predicate != null ? predicate : formatNode(tp.getPredicate())).append(" ");
							table.append(object != null ? object : formatNode(tp.getObject())).append(" ");

						} else {
							table.append("");
						}

						table.append(" | ");
					}
				} else {
					table.append("|");
				}
			}
			table.append("\n");
		}
		System.out.println(table.toString());
	}

	private String formatNode(Node n) {

		String before = "<span style=\"color:red\">";
		String after = "</span>";

		return n.isVariable() ? before + TriplePattern.trunc(n) + after : TriplePattern.trunc(n);
	}

	private TripleVarBindingSet combine(Map<Match, TripleVarBindingSet> someBindingSets) {
		var resultBS = new TripleVarBindingSet(this.graphPattern);
		for (TripleVarBindingSet bs : someBindingSets.values()) {
			resultBS.addAll(bs.getBindings());
		}
		return resultBS;
	}
}
