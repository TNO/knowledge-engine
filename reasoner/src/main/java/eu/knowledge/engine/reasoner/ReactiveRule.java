package eu.knowledge.engine.reasoner;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.sparql.core.Var;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class ReactiveRule extends Rule {

	/**
	 * the bindingset handler used to go from the incoming antecedent bindingset to
	 * the outgoing consequent bindingset.
	 */
	public BindingSetHandler forwardBindingSetHandler;

	/**
	 * the bindingset handler used to go from the incoming consequent bindingset to
	 * the outgoing antecedent bindingset.
	 * 
	 * this one is (for now) always the trivial one.
	 */
	public BindingSetHandler backwardBindingSetHandler;

	public static class TrivialAntecedentBindingSetHandler implements BindingSetHandler {

		private Set<TriplePattern> consequent;

		public TrivialAntecedentBindingSetHandler(Set<TriplePattern> aTriplePattern) {
			this.consequent = aTriplePattern;
		}

		@Override
		public CompletableFuture<BindingSet> handle(BindingSet bs) {

			BindingSet newBS = new BindingSet();

			Binding newB;

			Set<Var> vars = Rule.getVars(this.consequent);
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

	public static class TrivialConsequentBindingSetHandler implements BindingSetHandler {

		private Set<TriplePattern> antecedent;

		public TrivialConsequentBindingSetHandler(Set<TriplePattern> aTriplePattern) {
			this.antecedent = aTriplePattern;
		}

		@Override
		public CompletableFuture<BindingSet> handle(BindingSet bs) {

			BindingSet newBS = new BindingSet();

			Binding newB;

			Set<Var> vars = Rule.getVars(this.antecedent);
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

	public ReactiveRule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent,
			BindingSetHandler aBindingSetHandler) {
		super(anAntecedent, aConsequent);

		if (aBindingSetHandler == null)
			throw new IllegalArgumentException("A rule should have a non-null bindingsethandler.");

		this.forwardBindingSetHandler = aBindingSetHandler;
		this.backwardBindingSetHandler = new TrivialConsequentBindingSetHandler(anAntecedent);
	}

	/**
	 * Create a rule with a default bindingset handler.
	 * 
	 * @param anAntecedent
	 * @param aConsequent
	 */
	public ReactiveRule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent) {
		this(anAntecedent, aConsequent, new TrivialAntecedentBindingSetHandler(aConsequent));
	}

	public BindingSetHandler getForwardBindingSetHandler() {
		return this.forwardBindingSetHandler;
	}

	public BindingSetHandler getBackwardBindingSetHandler() {
		return this.backwardBindingSetHandler;
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
		ReactiveRule other = (ReactiveRule) obj;
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
