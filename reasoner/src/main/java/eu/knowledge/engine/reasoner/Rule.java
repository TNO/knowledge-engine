package eu.knowledge.engine.reasoner;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.sparql.core.Var;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class Rule extends BaseRule {

	/**
	 * the bindingset handler used to go from the incoming antecedent bindingset to
	 * the outgoing consequent bindingset.
	 */
	public TransformBindingSetHandler forwardBindingSetHandler;

	/**
	 * the bindingset handler used to go from the incoming consequent bindingset to
	 * the outgoing antecedent bindingset.
	 * 
	 * this one is (for now) always the trivial one.
	 */
	public TransformBindingSetHandler backwardBindingSetHandler;

	/**
	 * the bindingset handler used to deal with an incoming antecedent bindingset
	 * without producing an outgoing antecedent bindingset.
	 */
	public SinkBindingSetHandler sinkBindingSetHandler;

	public static class AntecedentToConsequentBindingSetHandler implements TransformBindingSetHandler {

		private Set<TriplePattern> consequent;

		public AntecedentToConsequentBindingSetHandler(Set<TriplePattern> aTriplePattern) {
			this.consequent = aTriplePattern;
		}

		@Override
		public CompletableFuture<BindingSet> handle(BindingSet bs) {

			BindingSet newBS = new BindingSet();

			Binding newB;

			Set<Var> vars = BaseRule.getVars(this.consequent);
			for (Binding b : bs) {
				newB = new Binding();
				for (Var v : vars) {
					if (b.containsKey(v)) {
						newB.put(v, b.get(v));
					} else {
						throw new IllegalArgumentException(
								"Not all variables in the consequent are available in the antecedent of the rule. This type of rule should use a custom BindingHandler.");
					}
				}
				newBS.add(newB);
			}

			CompletableFuture<BindingSet> future = new CompletableFuture<>();
			future.complete(newBS);
			return future;
		}
	}

	public static class ConsequentToAntecedentBindingSetHandler implements TransformBindingSetHandler {

		private Set<TriplePattern> antecedent;

		public ConsequentToAntecedentBindingSetHandler(Set<TriplePattern> aTriplePattern) {
			this.antecedent = aTriplePattern;
		}

		@Override
		public CompletableFuture<BindingSet> handle(BindingSet bs) {

			BindingSet newBS = new BindingSet();

			Binding newB;

			Set<Var> vars = BaseRule.getVars(this.antecedent);
			for (Binding b : bs) {
				newB = new Binding();
				for (Var v : vars) {
					if (b.containsKey(v)) {
						newB.put(v, b.get(v));
					}
				}
				newBS.add(newB);
			}

			CompletableFuture<BindingSet> future = new CompletableFuture<>();
			future.complete(newBS);
			return future;
		}
	}

	public Rule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent,
			TransformBindingSetHandler aBindingSetHandler) {
		super(anAntecedent, aConsequent);

		if (aBindingSetHandler == null)
			throw new IllegalArgumentException("A rule should have a non-null bindingsethandler.");

		this.forwardBindingSetHandler = aBindingSetHandler;
		this.backwardBindingSetHandler = new ConsequentToAntecedentBindingSetHandler(anAntecedent);
	}

	public Rule(Set<TriplePattern> anAntecedent, SinkBindingSetHandler aSinkBindingSetHandler) {
		super(anAntecedent, new HashSet<>());
		this.sinkBindingSetHandler = aSinkBindingSetHandler;
	}

	/**
	 * Create a rule with a default bindingset handler.
	 * 
	 * @param anAntecedent
	 * @param aConsequent
	 */
	public Rule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent) {
		this(anAntecedent, aConsequent, new AntecedentToConsequentBindingSetHandler(aConsequent));
	}

	public TransformBindingSetHandler getForwardBindingSetHandler() {
		return this.forwardBindingSetHandler;
	}

	public TransformBindingSetHandler getBackwardBindingSetHandler() {
		return this.backwardBindingSetHandler;
	}

	public SinkBindingSetHandler getSinkBindingSetHandler() {
		return this.sinkBindingSetHandler;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((backwardBindingSetHandler == null) ? 0 : backwardBindingSetHandler.hashCode());
		result = prime * result + ((forwardBindingSetHandler == null) ? 0 : forwardBindingSetHandler.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rule other = (Rule) obj;
		if (backwardBindingSetHandler == null) {
			if (other.backwardBindingSetHandler != null)
				return false;
		} else if (!backwardBindingSetHandler.equals(other.backwardBindingSetHandler))
			return false;
		if (forwardBindingSetHandler == null) {
			if (other.forwardBindingSetHandler != null)
				return false;
		} else if (!forwardBindingSetHandler.equals(other.forwardBindingSetHandler))
			return false;
		return true;
	}

}
