package eu.knowledge.engine.smartconnector.impl;

import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.smartconnector.api.AskPlan;
import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.BindingSet;

public class AskPlanImpl implements AskPlan {

	private SingleInteractionProcessor processor;

	public AskPlanImpl(SingleInteractionProcessor aProcessor) {
		this.processor = aProcessor;
	}

	@Override
	public CompletableFuture<AskResult> execute(BindingSet bindingSet) {
		if (bindingSet == null) {
			throw new IllegalArgumentException("the binding set should be non-null");
		}
		return this.processor.executeAskInteraction(bindingSet);
	}

	@Override
	public ReasonerPlan getReasonerPlan() {
		return (this.processor instanceof ReasonerProcessor ? ((ReasonerProcessor) this.processor).getReasonerPlan()
				: null);
	}

}
