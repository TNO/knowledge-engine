package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.KnowledgeInteractionInfo;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.KnowledgeInteractionInfo.Type;

public class HandleRequest {

	private CompletableFuture<BindingSet> future;
	private int handleRequestId;
	private List<Map<String, String>> bindingSet;
	private KnowledgeInteraction knowledgeInteraction;

	private KnowledgeInteractionInfo.Type type;
	private Type knowledgeInteractionType;

	public HandleRequest(int aHandleRequestId, KnowledgeInteraction aKI, KnowledgeInteractionInfo.Type type,
			List<Map<String, String>> aBindingSet, CompletableFuture<BindingSet> future) {
		this.future = future;
		this.handleRequestId = aHandleRequestId;
		this.knowledgeInteractionType = type;
		this.knowledgeInteraction = aKI;
		this.bindingSet = aBindingSet;
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

	public Type getKnowledgeInteractionType() {
		return knowledgeInteractionType;
	}

	@Override
	public String toString() {
		return "HandleRequest [future=" + future + ", handleRequestId=" + handleRequestId + ", bindingSet=" + bindingSet
				+ ", knowledgeInteraction=" + knowledgeInteraction + ", type=" + type + ", knowledgeInteractionType="
				+ knowledgeInteractionType + "]";
	}

}
