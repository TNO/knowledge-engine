package interconnect.ke.api;

import java.util.concurrent.CompletableFuture;

import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;
import interconnect.ke.messaging.SmartConnectorEndpoint;
import interconnect.ke.runtime.SmartConnectorRegistryImpl;

public class SmartConnector {

	private KnowledgeBase knowledgeBase;

	public SmartConnector(KnowledgeBase aKnowledgeBase) {
		knowledgeBase = aKnowledgeBase;
		SmartConnectorRegistryImpl.getInstance().register(this);
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

	public CompletableFuture<AskResult> ask(AskKnowledgeInteraction ki, RecipientSelector r, BindingSet bindings) {
		return CompletableFuture.supplyAsync(() -> null);
	}

	public CompletableFuture<AskResult> ask(AskKnowledgeInteraction ki, BindingSet bindings) {
		return ask(ki, null, bindings);
	}

	public CompletableFuture<PostResult> post(PostKnowledgeInteraction ki, RecipientSelector r, BindingSet arguments) {
		return CompletableFuture.supplyAsync(() -> null);
	}

	public CompletableFuture<PostResult> post(PostKnowledgeInteraction ki, BindingSet argument) {
		return post(ki, null, argument);
	}

	public void stop() {
		SmartConnectorRegistryImpl.getInstance().unregister(this);
	}

	public SmartConnectorEndpoint getEndpoint() {
		// TODO Auto-generated method stub
		return null;
	}

}
