package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.servlet.AsyncContext;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import eu.interconnectproject.knowledge_engine.rest.model.AskResult;
import eu.interconnectproject.knowledge_engine.rest.model.PostResult;
import eu.interconnectproject.knowledge_engine.rest.model.Workaround;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerHandler;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AskKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.CommunicativeAct;
import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.PostKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactHandler;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorBuilder;

public class RestKnowledgeBase implements KnowledgeBase {

	private String knowledgeBaseId;
	private String knowledgeBaseName;
	private String knowledgeBaseDescription;

	private AsyncContext asyncContext;
	private Queue<HandleRequest> toBeProcessedHandleRequests;
	private Map<String, HandleRequest> beingProcessedHandleRequests;
	private ReactHandler reactHandler;
	private SmartConnector sc;
	private Map<URI, KnowledgeInteraction> knowledgeInteractions = new HashMap<>();

	public RestKnowledgeBase(eu.interconnectproject.knowledge_engine.rest.model.SmartConnector scModel) {
		this.knowledgeBaseId = scModel.getKnowledgeBaseId();
		this.knowledgeBaseName = scModel.getKnowledgeBaseName();
		this.knowledgeBaseDescription = scModel.getKnowledgeBaseDescription();
		this.sc = SmartConnectorBuilder.newSmartConnector(this).create();
	}

	private AnswerHandler handler = new AnswerHandler() {

		@Override
		public BindingSet answer(AnswerKnowledgeInteraction anAKI, BindingSet aBindingSet) {

//			CompletableFuture future = doSomeThing();

			CompletableFuture<BindingSet> future2 = new CompletableFuture<>();
			// toBeProcessedByKnowledgeBase(future2);

			try {
				return future2.get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

	};

	public boolean register(Workaround workaround) {
		var ca = new CommunicativeAct(toResources(workaround.getCommunicativeAct().getRequiredPurposes()), toResources(workaround.getCommunicativeAct().getSatisfiedPurposes()));

		String type = workaround.getKnowledgeInteractionType();
		if (type.equals("AskKnowledgeInteraction")) {
			var askKI = new AskKnowledgeInteraction(ca, new GraphPattern(workaround.getGraphPattern()));
			URI kiId = this.sc.register(askKI);
			this.knowledgeInteractions.put(kiId, askKI);
		} else if (type.equals("AnswerKnowledgeInteraction")) {
			var answerKI = new AnswerKnowledgeInteraction(ca, new GraphPattern(workaround.getGraphPattern()));
			URI kiId = this.sc.register(answerKI, this.handler);
			this.knowledgeInteractions.put(kiId, answerKI);
		} else if (type.equals("PostKnowledgeInteraction")) {
			var postKI = new PostKnowledgeInteraction(ca, new GraphPattern(workaround.getArgumentGraphPattern()), new GraphPattern(workaround.getResultGraphPattern()));
			URI kiId = this.sc.register(postKI);
			this.knowledgeInteractions.put(kiId, postKI);
		} else if (type.equals("ReactKnowledgeInteraction")) {
			var reactKI = new ReactKnowledgeInteraction(ca, new GraphPattern(workaround.getArgumentGraphPattern()), new GraphPattern(workaround.getResultGraphPattern()));
			URI kiId = this.sc.register(reactKI, this.reactHandler);
			this.knowledgeInteractions.put(kiId, reactKI);
		} else {
			return false;
		}

		return true;
	}

	private Set<Resource> toResources(List<String> strings) {
		return strings.stream().map((str) -> {
			return ResourceFactory.createProperty(str);
		}).collect(Collectors.toSet());
	}

	public Workaround getKnowledgeInteraction(String knowledgeInteractionId) {
		throw new RuntimeException("TODO");
	}
	
	public Set<Workaround> getKnowledgeInteractions() {
		throw new RuntimeException("TODO");
	}
	
	public boolean delete(String knowledgeInteractionId) {
		throw new RuntimeException("TODO");
	}
	
	public AskResult ask(String kiId, List<Map<String, String>> bindings) {
		throw new RuntimeException("TODO");
		// this.sc.ask(new URI(kiId), bindings);
	}
	
	public PostResult post(String kiId, List<Map<String, String>> bindings) {
		throw new RuntimeException("TODO");
		// this.sc.post(ki, bindings)
	}

	@Override
	public URI getKnowledgeBaseId() {
		try {
			return new URI(this.knowledgeBaseId);
		} catch (URISyntaxException e) {
			assert false;
		}
		return null;
	}

	@Override
	public String getKnowledgeBaseName() {
		return this.knowledgeBaseName;
	}

	@Override
	public String getKnowledgeBaseDescription() {
		return this.knowledgeBaseDescription;
	}

	@Override
	public void smartConnectorReady(SmartConnector aSC) {
		// Do nothing. The REST API doesn't provide these signals (yet).
	}

	@Override
	public void smartConnectorConnectionLost(SmartConnector aSC) {
		// Do nothing. The REST API doesn't provide these signals (yet).
	}

	@Override
	public void smartConnectorConnectionRestored(SmartConnector aSC) {
		// Do nothing. The REST API doesn't provide these signals (yet).
	}

	@Override
	public void smartConnectorStopped(SmartConnector aSC) {
		// Do nothing. The REST API doesn't provide these signals (yet).
	}

}
