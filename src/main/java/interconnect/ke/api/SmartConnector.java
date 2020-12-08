package interconnect.ke.api;

import java.util.Set;

import interconnect.ke.runtime.SmartConnectorRegistry;

public class SmartConnector {

	private KnowledgeBase knowledgeBase;

	public SmartConnector(KnowledgeBase aKnowledgeBase) {
		knowledgeBase = aKnowledgeBase;
		SmartConnectorRegistry.getInstance().register(this);
	}

	public void register(AskKnowledgeInteraction anAskKI) {

	}

	public void unregister(AskKnowledgeInteraction anAskKI) {

	}

	public void register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler aAnswerHandler) {

	}

	public void unregister(AnswerKnowledgeInteraction anAnswerKI) {

	}

	public void register(PostKnowledgeInteraction aPostKI) {

	}

	public void unregister(PostKnowledgeInteraction aPostKI) {

	}

	public void register(ReactKnowledgeInteraction anReactKI, ReactHandler aReactHandler) {

	}

	public void unregister(ReactKnowledgeInteraction anReactKI) {

	}

	public AskResult ask(AskKnowledgeInteraction ki, RecipientSelector r, Set<Bindings> bindings) {
		return null;
	}

	public AskResult ask(AskKnowledgeInteraction ki, Set<Bindings> bindings) {
		return ask(ki, null, bindings);
	}

	public PostResult post(PostKnowledgeInteraction ki, RecipientSelector r, Set<Bindings> arguments) {
		return null;
	}

	public PostResult post(PostKnowledgeInteraction ki, Set<Bindings> argument) {
		return post(ki, null, argument);
	}

	public void stop() {
		SmartConnectorRegistry.getInstance().unregister(this);
	}

}
