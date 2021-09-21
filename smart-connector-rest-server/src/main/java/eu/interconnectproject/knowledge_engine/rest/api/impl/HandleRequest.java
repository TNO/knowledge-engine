package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingValidator;
import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeInteraction;

public class HandleRequest {
	private static final Logger LOG = LoggerFactory.getLogger(HandleRequest.class);

	private CompletableFuture<BindingSet> future;
	private int handleRequestId;
	private List<Map<String, String>> bindingSet;
	private KnowledgeInteraction knowledgeInteraction;
	private final URI requestingKnowledgeBaseId;

	private KnowledgeInteractionType knowledgeInteractionType;

	public HandleRequest(int aHandleRequestId, KnowledgeInteraction aKI, KnowledgeInteractionType type,
			List<Map<String, String>> aBindingSet, URI aRequestingKnowledgeBaseId, CompletableFuture<BindingSet> future) {
		this.future = future;
		this.handleRequestId = aHandleRequestId;
		this.knowledgeInteractionType = type;
		this.knowledgeInteraction = aKI;
		this.bindingSet = aBindingSet;
		this.requestingKnowledgeBaseId = aRequestingKnowledgeBaseId;
	}

	public CompletableFuture<BindingSet> getFuture() {
		return future;
	}

	public void setFuture(CompletableFuture<BindingSet> future) {
		this.future = future;
	}

	public int getHandleRequestId() {
		return handleRequestId;
	}

	public List<Map<String, String>> getBindingSet() {
		return bindingSet;
	}

	public void setBindingSet(List<Map<String, String>> bindingSet) {
		this.bindingSet = bindingSet;
	}

	public KnowledgeInteraction getKnowledgeInteraction() {
		return knowledgeInteraction;
	}

	public void setKnowledgeInteraction(KnowledgeInteraction knowledgeInteraction) {
		this.knowledgeInteraction = knowledgeInteraction;
	}

	public KnowledgeInteractionType getKnowledgeInteractionType() {
		return knowledgeInteractionType;
	}

	public URI getRequestingKnowledgeBaseId() {
		return requestingKnowledgeBaseId;
	}

	public void validateBindings(BindingSet bindings) {
		GraphPattern graphPattern = null;
		switch (this.knowledgeInteractionType) {
		case ANSWER:
			graphPattern = ((AnswerKnowledgeInteraction) this.knowledgeInteraction).getPattern();
			break;
		case REACT:
			graphPattern = ((ReactKnowledgeInteraction) this.knowledgeInteraction).getResult();
			break;
		default:
			throw new RuntimeException(String.format(
					"Unexpected knowledge interaction type '%s' in HandleRequest. Should be either ANSWER or REACT.",
					this.knowledgeInteractionType));
		}
		var validator = new BindingValidator();
		validator.validateCompleteBindings(graphPattern, bindings);
	}

	@Override
	public String toString() {
		return "HandleRequest [future=" + future + ", handleRequestId=" + handleRequestId + ", bindingSet=" + bindingSet
				+ ", knowledgeInteraction=" + knowledgeInteraction + ", knowledgeInteractionType="
				+ knowledgeInteractionType + "]";
	}

}
