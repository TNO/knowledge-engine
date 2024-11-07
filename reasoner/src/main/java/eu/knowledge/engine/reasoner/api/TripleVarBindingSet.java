package eu.knowledge.engine.reasoner.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Match;

public class TripleVarBindingSet {

	private Set<TriplePattern> graphPattern;
	private Set<TripleVarBinding> bindings;
	private Set<TripleNode> tripleVarsCache;

	private static final Logger LOG = LoggerFactory.getLogger(TripleVarBindingSet.class);

	public TripleVarBindingSet(Set<TriplePattern> aGraphPattern) {

		this.graphPattern = aGraphPattern;
		bindings = ConcurrentHashMap.newKeySet();
	}

	public TripleVarBindingSet(Set<TriplePattern> aGraphPattern, BindingSet aBindingSet) {

		this(aGraphPattern);

		for (Binding b : aBindingSet) {
			this.add(new TripleVarBinding(this.graphPattern, b));
		}
	}

	public Set<TriplePattern> getGraphPattern() {
		return graphPattern;
	}

	public Set<TripleVarBinding> getBindings() {
		return bindings;
	}

	public BindingSet toBindingSet() {
		BindingSet bs = new BindingSet();
		for (TripleVarBinding tvb : this.bindings) {
			bs.add(tvb.toBinding());
		}
		return bs;
	}

	public void add(TripleVarBinding aTripleVarBinding) {
		// TODO check if the triple pattern in the triplevar is actually present in our
		// graph pattern.
		this.bindings.add(aTripleVarBinding);
	}

	public Set<TripleNode> getTripleVars() {
		if (tripleVarsCache == null) {
			tripleVarsCache = new HashSet<>();
			for (TriplePattern tp : graphPattern) {

				if (tp.getSubject().isVariable()) {
					tripleVarsCache.add(new TripleNode(tp, tp.getSubject(), 0));
				}
				if (tp.getPredicate().isVariable()) {
					tripleVarsCache.add(new TripleNode(tp, tp.getPredicate(), 1));
				}
				if (tp.getObject().isVariable()) {
					tripleVarsCache.add(new TripleNode(tp, tp.getObject(), 2));
				}
			}
		}
		return tripleVarsCache;
	}

	/**
	 * @return bindings in which not all variable instances are present.
	 */
	public TripleVarBindingSet getPartialBindingSet() {
		TripleVarBindingSet gbs = new TripleVarBindingSet(this.graphPattern);
		Set<TripleNode> vars = this.getTripleVars();
		int nrOfVars = vars.size();
		for (TripleVarBinding tvb : bindings) {
			if (tvb.keySet().size() < nrOfVars) {
				gbs.add(tvb);
			}
		}
		return gbs;
	}

	/**
	 * @return bindings in which all variable instances are present.
	 */
	public TripleVarBindingSet getFullBindingSet() {
		TripleVarBindingSet gbs = new TripleVarBindingSet(this.graphPattern);
		Set<TripleNode> vars = this.getTripleVars();
		int nrOfVars = vars.size();
		for (TripleVarBinding tvb : bindings) {
			if (tvb.keySet().size() == nrOfVars) {
				assert tvb.getTripleVars().equals(this.getTripleVars());
				gbs.add(tvb);
			}
		}
		return gbs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bindings == null) ? 0 : bindings.hashCode());
		result = prime * result + ((graphPattern == null) ? 0 : graphPattern.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TripleVarBindingSet)) {
			return false;
		}
		TripleVarBindingSet other = (TripleVarBindingSet) obj;
		if (bindings == null) {
			if (other.bindings != null) {
				return false;
			}
		} else if (!bindings.equals(other.bindings)) {
			return false;
		}
		if (graphPattern == null) {
			if (other.graphPattern != null) {
				return false;
			}
		} else if (!graphPattern.equals(other.graphPattern)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return bindings.toString();
	}

	/**
	 * Simply the union between the two bindingsets. Does nothing complex for now.
	 * 
	 * @param aGraphBindingSet
	 * @return
	 */
	public TripleVarBindingSet merge(TripleVarBindingSet aGraphBindingSet) {
		TripleVarBindingSet gbs = new TripleVarBindingSet(this.graphPattern);

		if (this.bindings.isEmpty()) {
			for (TripleVarBinding tvb2 : aGraphBindingSet.getBindings()) {
				gbs.add(tvb2);
			}
		} else {
			// Cartesian product is the base case
			gbs.addAll(aGraphBindingSet.getBindings());
			gbs.addAll(this.bindings);
			this.bindings.stream().parallel().forEach(tvb1 -> {

				for (TripleVarBinding otherB : aGraphBindingSet.getBindings()) {
					// always add a merged version of the two bindings, except when they conflict.
					if (!tvb1.isConflicting(otherB)) {
						gbs.add(tvb1.merge(otherB));
					}
				}
			});
		}

		return gbs;
	}

	public boolean isEmpty() {

		return this.bindings.isEmpty();

	}

	/**
	 * Translate this bindingset using the given match. The variablenames will be
	 * changed and variables not relevant in the match will be removed.
	 * 
	 * The format of the mapping is expected to be translate <from triple pattern>,
	 * <to triple pattern>.
	 * 
	 * It also filters bindings that are incompatible with the match.
	 * 
	 * @param match
	 * @return
	 */
	public TripleVarBindingSet translate(Set<TriplePattern> graphPattern, Set<Match> match) {
		LOG.trace("Translating binding set with '{}' bindings and '{}' matches.", this.bindings.size(), match.size());

		long start = System.currentTimeMillis();

		TripleVarBindingSet newOne = new TripleVarBindingSet(graphPattern);
		TripleVarBinding toB;
		for (TripleVarBinding fromB : this.bindings) {
			for (Match entry : match) {
				boolean skip = false;
				toB = new TripleVarBinding();
				for (Map.Entry<TriplePattern, TriplePattern> keyValue : entry.getMatchingPatterns().entrySet()) {
					TriplePattern fromTriple = keyValue.getKey();
					TriplePattern toTriple = keyValue.getValue();
					Map<TripleNode, TripleNode> mapping = fromTriple.findMatches(toTriple);
					for (Map.Entry<TripleNode, TripleNode> singleMap : mapping.entrySet()) {
						TripleNode toTNode = singleMap.getValue();
						TripleNode fromTNode = singleMap.getKey();

						// first consider all possible combinations of concrete and variable nodes.
						// note that there are slight variations in how we want to translate filter
						// and result bindingsets
						if (fromTNode.node instanceof Var && toTNode.node instanceof Var) {
							var fromTVar = new TripleNode(fromTriple, (Var) fromTNode.node, fromTNode.nodeIdx);
							var toTVar = new TripleNode(toTriple, (Var) toTNode.node, toTNode.nodeIdx);
							var toBVarValue = toB.getVarValue((Var) toTVar.node);
							if (fromB.containsKey(fromTVar) && !toB.containsKey(toTVar)
									&& (toBVarValue == null || toBVarValue.equals(fromB.get(fromTVar)))) {
								toB.put(toTVar, fromB.get(fromTVar));
							} else if (fromB.containsKey(fromTVar) && toB.containsVar((Var) toTVar.node)
									&& !fromB.get(fromTVar).equals(toBVarValue)) {
								skip = true; // conflict, so skip
							}
						} else if (fromTNode.node instanceof Var && toTNode.node instanceof Node) {
							var fromTVar = new TripleNode(fromTriple, (Var) fromTNode.node, fromTNode.nodeIdx);
							if (fromB.containsKey(fromTVar) && !fromB.get(fromTVar).equals(toTNode.node)) {
								skip = true; // conflict, so skip
							}
						} else if (fromTNode.node instanceof Node && toTNode.node instanceof Var) {
							var toTVar = new TripleNode(toTriple, (Var) toTNode.node, toTNode.nodeIdx);
							if (toB.containsVar((Var) toTVar.node)
									&& !toB.getVarValue((Var) toTVar.node).equals(fromTNode.node)) {
								skip = true;
							} else if (!toB.containsVar((Var) toTVar.node)) {
								toB.put(toTVar, (Node) fromTNode.node);
							}
						}
					}
				}
				if (!skip)
					newOne.add(toB);
			}
		}

		return newOne;

	}

	public void addAll(Set<TripleVarBinding> permutatedTVBs) {
		this.bindings.addAll(permutatedTVBs);
	}

	/**
	 * Only keep those bindings in {@code this} bindingset that are compatible with
	 * the bindings in the given {@code bindingSet}.
	 * 
	 * @param bindingSet
	 * @return
	 */
	public TripleVarBindingSet keepCompatible(TripleVarBindingSet bindingSet) {

		TripleVarBindingSet newBS = new TripleVarBindingSet(this.getGraphPattern());

		for (TripleVarBinding b : this.getBindings()) {

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

	/**
	 * Prints the debugging table of this triple Var binding set
	 */
	public void printDebuggingTable() {
		StringBuilder table = new StringBuilder();

		// header row
		int count = 0;
		table.append("| Graph Pattern |");
		count++;

		List<TripleVarBinding> tvbindings = new ArrayList<>(this.getBindings());

		for (int i = 0; i < tvbindings.size(); i++) {
			table.append("Binding-" + i).append(" | ");
			count++;
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

			for (TripleVarBinding tvb : tvbindings) {
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

					table.append(subject != null ? subject : formatNode(tp.getSubject())).append(" ");
					table.append(predicate != null ? predicate : formatNode(tp.getPredicate())).append(" ");
					table.append(object != null ? object : formatNode(tp.getObject())).append(" ");

				} else {
					table.append("");
				}

				table.append(" | ");
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
