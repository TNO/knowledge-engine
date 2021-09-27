package eu.knowledge.engine.smartconnector.runtime.messaging;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.AnswerHandler;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostResult;
import eu.knowledge.engine.smartconnector.api.ReactHandler;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.RecipientSelector;
import eu.knowledge.engine.smartconnector.impl.RuntimeSmartConnector;
import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.messaging.ErrorMessage;
import eu.knowledge.engine.smartconnector.messaging.KnowledgeMessage;
import eu.knowledge.engine.smartconnector.messaging.MessageDispatcherEndpoint;
import eu.knowledge.engine.smartconnector.messaging.PostMessage;
import eu.knowledge.engine.smartconnector.messaging.ReactMessage;
import eu.knowledge.engine.smartconnector.messaging.SmartConnectorEndpoint;

public class MockSmartConnector implements RuntimeSmartConnector, SmartConnectorEndpoint {

	private static final Logger LOG = LoggerFactory.getLogger(MockSmartConnector.class);

	private final URI knowledgeBaseId;
	private MessageDispatcherEndpoint messageDispatcherEndpoint;
	private KnowledgeMessage lastMessage;

	public MockSmartConnector(URI knowledgeBaseId) {
		this.knowledgeBaseId = knowledgeBaseId;
	}

	@Override
	public URI register(AskKnowledgeInteraction anAskKI) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unregister(AskKnowledgeInteraction anAskKI) {
		// TODO Auto-generated method stub

	}

	@Override
	public URI register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler aAnswerHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unregister(AnswerKnowledgeInteraction anAnswerKI) {
		// TODO Auto-generated method stub

	}

	@Override
	public URI register(PostKnowledgeInteraction aPostKI) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unregister(PostKnowledgeInteraction aPostKI) {
		// TODO Auto-generated method stub

	}

	@Override
	public URI register(ReactKnowledgeInteraction anReactKI, ReactHandler aReactHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unregister(ReactKnowledgeInteraction anReactKI) {
		// TODO Auto-generated method stub

	}

	@Override
	public CompletableFuture<AskResult> ask(AskKnowledgeInteraction anAKI, RecipientSelector aSelector,
			BindingSet aBindingSet) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<AskResult> ask(AskKnowledgeInteraction ki, BindingSet bindings) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<PostResult> post(PostKnowledgeInteraction aPKI, RecipientSelector aSelector,
			BindingSet someArguments) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<PostResult> post(PostKnowledgeInteraction ki, BindingSet argument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public URI getKnowledgeBaseId() {
		return this.knowledgeBaseId;
	}

	@Override
	public SmartConnectorEndpoint getSmartConnectorEndpoint() {
		return this;
	}

	public KnowledgeMessage getLastMessage() {
		return this.lastMessage;
	}

	private void receiveMessage(KnowledgeMessage message) {
		LOG.info(this.knowledgeBaseId + ": Received " + message.getClass().getSimpleName() + " with ID "
				+ message.getMessageId());
		this.lastMessage = message;
	}

	@Override
	public void handleAskMessage(AskMessage message) {
		receiveMessage(message);
	}

	@Override
	public void handleAnswerMessage(AnswerMessage message) {
		receiveMessage(message);
	}

	@Override
	public void handlePostMessage(PostMessage message) {
		receiveMessage(message);
	}

	@Override
	public void handleReactMessage(ReactMessage message) {
		receiveMessage(message);
	}

	@Override
	public void handleErrorMessage(ErrorMessage message) {
		receiveMessage(message);
	}

	@Override
	public void setMessageDispatcher(MessageDispatcherEndpoint messageDispatcherEndpoint) {
		this.messageDispatcherEndpoint = messageDispatcherEndpoint;
	}

	@Override
	public void unsetMessageDispatcher() {
	}

	public void send(KnowledgeMessage message) throws IOException {
		LOG.info(this.knowledgeBaseId + ": Sending  " + message.getClass().getSimpleName() + " with ID "
				+ message.getMessageId());
		this.messageDispatcherEndpoint.send(message);
	}

}
