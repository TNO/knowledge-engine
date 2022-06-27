package eu.knowledge.engine.smartconnector.impl;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.ReasoningNode;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.PostPlan;
import eu.knowledge.engine.smartconnector.api.PostResult;

public class PostPlanImpl implements PostPlan {

	private static final Logger LOG = LoggerFactory.getLogger(PostPlanImpl.class);

	private SingleInteractionProcessor processor;

	public PostPlanImpl(SingleInteractionProcessor aProcessor) {
		this.processor = aProcessor;
	}

	@Override
	public CompletableFuture<PostResult> execute(BindingSet someArguments) {
		if (someArguments == null) {
			throw new IllegalArgumentException("the binding set should be non-null");
		}
		return this.processor.executePostInteraction(someArguments).handle((r, e) -> {

			if (r == null) {
				LOG.error("An exception has occured while executing Post Plan", e);
				return null;
			} else {
				return r;
			}
		});
	}

	@Override
	public ReasoningNode getReasoningNode() {
		return (this.processor instanceof ReasonerProcessor ? ((ReasonerProcessor) this.processor).getReasoningNode()
				: null);
	}

}
