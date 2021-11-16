package eu.knowledge.engine.reasoner;

import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.reasoner.api.BindingSet;

public interface BindingSetHandler {
	public CompletableFuture<BindingSet> handle(BindingSet bs);
}
