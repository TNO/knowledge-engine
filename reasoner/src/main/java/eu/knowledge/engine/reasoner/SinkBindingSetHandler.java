package eu.knowledge.engine.reasoner;

import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.reasoner.api.BindingSet;

public interface SinkBindingSetHandler extends BindingSetHandler {

	public CompletableFuture<Void> handle(BindingSet aBindingSet);

}
