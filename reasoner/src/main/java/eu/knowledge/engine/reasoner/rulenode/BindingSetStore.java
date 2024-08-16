package eu.knowledge.engine.reasoner.rulenode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Concrete;

import eu.knowledge.engine.reasoner.BaseRule;
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
	private final Map<RuleNode, TripleVarBindingSet> neighborBindingSet = new HashMap<>();
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

		boolean changed = previousBindingSet == null || !previousBindingSet.equals(aBindingSet);

		if (changed)
			this.cache = null;

		return changed;
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

		if (this.cache != null) {
			return this.cache;
		}
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
		this.cache = combinedBS.merge(combinedBS);
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

		List<RuleNode> allNeighbors = new ArrayList<>(neighbors);

		// header row
		int count = 0;
		table.append("| Graph Pattern |");
		count++;
		for (RuleNode neighbor : allNeighbors) {

			if (this.neighborBindingSet.containsKey(neighbor)) {
				for (int i = 0; i < this.neighborBindingSet.get(neighbor).getBindings().size(); i++) {
					table.append(this.getName(neighbor.getRule()) + "-" + i).append(" | ");
					count++;
				}
			} else {
				table.append(this.getName(neighbor.getRule())).append(" | ");
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
			for (RuleNode neigh : allNeighbors) {

				TripleVarBindingSet tvbs = this.neighborBindingSet.get(neigh);

				if (tvbs != null) {
					for (TripleVarBinding tvb : tvbs.getBindings()) {
						Set<TripleNode> nodes = tvb.getTripleNodes(tp);
						if (!nodes.isEmpty()) {

							Node_Concrete subject = null;
							Node_Concrete predicate = null;
							Node_Concrete object = null;

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

}
