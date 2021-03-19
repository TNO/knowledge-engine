package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.util.concurrent.CompletableFuture;

import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;

public class HandleRequest {
	
	private CompletableFuture<BindingSet> future;
	private String handleRequestId;
	

	static void toBeProcessedByKnowledgeBase(CompletableFuture<BindingSet> future) {

		future.complete(new BindingSet());
		
	}

}
