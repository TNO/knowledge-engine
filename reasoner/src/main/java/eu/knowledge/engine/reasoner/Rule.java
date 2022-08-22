package eu.knowledge.engine.reasoner;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.sparql.core.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

/**
 * The base implementation of a rule. A rule typically has either an antecedent
 * or a consequent or both. A rule transforms data that comes in from the
 * antecedent to its consequent. This transformation happens via the so called
 * <i>BindingSetHandler<i>. There are several types of BindingSetHandlers, but
 * all of them deal with incoming BindingSets and most of them transform this
 * incoming BindingSet into an outgoing BindingSet although not all of them do.
 * This reasoning library allows customized BindingSetHandlers to be attached to
 * rules that replace the default BindingSetHandlers. Default BindingSetHandlers
 * take all variables that occur in both the antecedent and consequent of the
 * rule (if present) and copy their values from the one BindingSet to the other
 * BindingSet. Custom BindingSetHandlers, however, can do more complicated
 * things like do calculations, etc.<br/>
 * <br/>
 * Forward in the context of a rule means going from the antecedent to the
 * consequent, while backward means going from the consequent to the antecedent.
 * 
 * @author nouwtb
 *
 */
public class Rule extends BaseRule {

	/**
	 * the bindingset handler used to from the incoming consequent bindingset
	 * directly to the outgoing consequent binding set.
	 */
	public TransformBindingSetHandler backwardForwardBindingSetHandler;

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

	private static final Logger LOG = LoggerFactory.getLogger(Rule.class);

	/**
	 * This default bindingsethandler copies the values for any variables that occur
	 * both in the consequent and the antecedent to the antecedent bindingset. TODO
	 * In the future we could allow these to be customized as well which would allow
	 * us to for example transform degrees Celsius to degrees Fahrenheit when the
	 * actual rule transform Fahrenheit to Celsius. This means that this bindingset
	 * represents the inverse of applying this rule and this can be useful if
	 * incoming bindingsets contain degrees celsius and instead of ignoring these,
	 * we can transform them into degrees Fahrenheit.
	 * 
	 * @author nouwtb
	 *
	 */
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

		if (anAntecedent.isEmpty() && !aConsequent.isEmpty())
			this.backwardForwardBindingSetHandler = aBindingSetHandler;
		else if (!anAntecedent.isEmpty() && !aConsequent.isEmpty())
			this.forwardBindingSetHandler = aBindingSetHandler;
		else
			throw new IllegalArgumentException(
					"A rule with a TransformBindingSetHandler should have both an antecedent and a consequent or only a consequent and not antecedent '"
							+ anAntecedent + "' and consequent '" + aConsequent + "'");
		this.backwardBindingSetHandler = new ConsequentToAntecedentBindingSetHandler(anAntecedent);
	}

	public Rule(Set<TriplePattern> anAntecedent, SinkBindingSetHandler aSinkBindingSetHandler) {
		super(anAntecedent, new HashSet<>());
		this.sinkBindingSetHandler = aSinkBindingSetHandler;
	}

	public Rule(Set<TriplePattern> aConsequent, TransformBindingSetHandler aBindingSetHandler) {
		this(new HashSet<>(), aConsequent, aBindingSetHandler);
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

	public TransformBindingSetHandler getBindingSetHandler() {
		assert !this.getAntecedent().isEmpty() && !this.getConsequent().isEmpty()
				|| this.getAntecedent().isEmpty() && !this.getConsequent().isEmpty();

		if (this.getAntecedent().isEmpty())
			return this.backwardForwardBindingSetHandler;
		else
			return this.forwardBindingSetHandler;
	}

	public TransformBindingSetHandler getInverseBindingSetHandler() {
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
