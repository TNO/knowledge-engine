package eu.knowledge.engine.reasoner.api;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Concrete;
import org.apache.jena.sparql.core.Var;

import eu.knowledge.engine.reasoner.Match;

public class TripleVarBindingSet {

	private Set<TriplePattern> graphPattern;
	private Set<TripleVarBinding> bindings;

	public TripleVarBindingSet(Set<TriplePattern> aGraphPattern) {

		this.graphPattern = aGraphPattern;
		bindings = new HashSet<>();
	}

	public TripleVarBindingSet(Set<TriplePattern> aGraphPattern, BindingSet aBindingSet) {

		this.graphPattern = aGraphPattern;

		this.bindings = new HashSet<>();

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

	public Set<TripleVar> getTripleVars() {
		Set<TripleVar> vars = new HashSet<>();
		for (TriplePattern tp : graphPattern) {
			for (Var var : tp.getVariables()) {
				vars.add(new TripleVar(tp, var));
			}
		}
		return vars;
	}

	/**
	 * @return bindings in which not all variable instances are present.
	 */
	public TripleVarBindingSet getPartialBindingSet() {
		TripleVarBindingSet gbs = new TripleVarBindingSet(this.graphPattern);
		Set<TripleVar> vars = this.getTripleVars();
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
		Set<TripleVar> vars = this.getTripleVars();
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
	 * @param gbs
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
			boolean firstTime = true;
			for (TripleVarBinding tvb1 : this.bindings) {
				gbs.add(tvb1);

				for (TripleVarBinding otherB : aGraphBindingSet.getBindings()) {
					if (firstTime)
						gbs.add(otherB);

					// always add a merged version of the two bindings, except when they conflict.
					if (!tvb1.isConflicting(otherB)) {
						gbs.add(tvb1.merge(otherB));
					}
				}
				firstTime = false;
			}
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
		TripleVarBindingSet newOne = new TripleVarBindingSet(graphPattern);
		TripleVarBinding toB;

		if (this.bindings.isEmpty()) {
			// bindings coming through the match.
			for (Match entry : match) {
				toB = new TripleVarBinding();
				for (Map.Entry<TriplePattern, TriplePattern> keyValue : entry.getMatchingPatterns().entrySet()) {
					Map<Node, Node> mapping = keyValue.getKey().findMatches(keyValue.getValue());
					for (Map.Entry<Node, Node> singleMap : mapping.entrySet()) {
						if (singleMap.getValue() instanceof Var && singleMap.getKey() instanceof Node_Concrete) {
							// if the binding set is empty (and we are translating child results back to
							// current node results, we actually do not want to add the static literal.
							toB.put(new TripleVar(keyValue.getValue(), (Var) singleMap.getValue()),
									(Node_Concrete) singleMap.getKey());
						}
					}

				}
				newOne.add(toB);
			}

		} else {
			for (TripleVarBinding fromB : this.bindings) {
				for (Match entry : match) {
					boolean skip = false;
					toB = new TripleVarBinding();
					for (Map.Entry<TriplePattern, TriplePattern> keyValue : entry.getMatchingPatterns().entrySet()) {
						TriplePattern fromTriple = keyValue.getKey();
						if (fromB.containsTriplePattern(fromTriple)) {
							TriplePattern toTriple = keyValue.getValue();
							Map<Node, Node> mapping = fromTriple.findMatches(toTriple); // TODO get these from entry
							for (Map.Entry<Node, Node> singleMap : mapping.entrySet()) {
								Node toNode = singleMap.getValue();
								Node fromNode = singleMap.getKey();

								// first consider all possible combinations of concrete and variable nodes.
								// note that there are slight variations in how we want to translate filter
								// and result bindingsets
								if (fromNode instanceof Var && toNode instanceof Var) {
									var fromTVar = new TripleVar(fromTriple, (Var) fromNode);
									var toTVar = new TripleVar(toTriple, (Var) toNode);
									if (fromB.containsKey(fromTVar) && !toB.containsKey(toTVar)) {
										toB.put(toTVar, fromB.get(fromTVar));
									} else if (fromB.containsKey(fromTVar) && toB.containsKey(toTVar)
											&& !fromB.get(fromTVar).equals(toB.get(toTVar))) {
										skip = true; // conflict, so skip
									}
								} else if (fromNode instanceof Var && toNode instanceof Node_Concrete) {
									var fromTVar = new TripleVar(fromTriple, (Var) fromNode);
									if (fromB.containsKey(fromTVar) && !fromB.get(fromTVar).equals(toNode)) {
										skip = true; // conflict, so skip
									}
								} else if (fromNode instanceof Node_Concrete && toNode instanceof Var) {
									var toTVar = new TripleVar(toTriple, (Var) toNode);
									if (toB.containsKey(toTVar) && !toB.get(toTVar).equals(fromNode)) {
										skip = true;
									} else if (!toB.containsKey(toTVar)) {
										toB.put(toTVar, (Node_Concrete) fromNode);
									}
								} else if (fromNode instanceof Node_Concrete && toNode instanceof Node_Concrete) {
									assert fromNode.equals(toNode);
								}
							}
						}
					}
					if (!skip)
						newOne.add(toB);
				}
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
}
