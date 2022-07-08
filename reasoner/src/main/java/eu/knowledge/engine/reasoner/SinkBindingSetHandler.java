package eu.knowledge.engine.reasoner;

import eu.knowledge.engine.reasoner.api.BindingSet;

public interface SinkBindingSetHandler extends BindingSetHandler {

	void handle(BindingSet aBindingSet);

}
