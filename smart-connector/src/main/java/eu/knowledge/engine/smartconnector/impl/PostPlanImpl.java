package eu.knowledge.engine.smartconnector.impl;

import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.reasoner.ReasoningNode;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.PostPlan;
import eu.knowledge.engine.smartconnector.api.PostResult;

public class PostPlanImpl implements PostPlan {

	private SingleInteractionProcessor processor;

	public PostPlanImpl(SingleInteractionProcessor aProcessor) {
		this.processor = aProcessor;
	}

	@Override
	public CompletableFuture<PostResult> execute(BindingSet someArguments) {
		if (someArguments == null) {
			throw new IllegalArgumentException("the binding set should be non-null");
		}
		return this.processor.executePostInteraction(someArguments);
	}

	@Override
	public ReasoningNode getReasoningNode() {
		return (this.processor instanceof ReasonerProcessor ? ((ReasonerProcessor) this.processor).getReasoningNode()
				: null);
	}

}
