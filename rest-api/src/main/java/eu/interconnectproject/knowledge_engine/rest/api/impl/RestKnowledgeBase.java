package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.servlet.AsyncContext;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.rest.model.AskResult;
import eu.interconnectproject.knowledge_engine.rest.model.PostResult;
import eu.interconnectproject.knowledge_engine.rest.model.Workaround;
import eu.interconnectproject.knowledge_engine.rest.model.WorkaroundWithId;
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
	private static final Logger LOG = LoggerFactory.getLogger(RestKnowledgeBase.class);

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

	public String register(Workaround workaround) {
		var ca = new CommunicativeAct(toResources(workaround.getCommunicativeAct().getRequiredPurposes()), toResources(workaround.getCommunicativeAct().getSatisfiedPurposes()));

		String type = workaround.getKnowledgeInteractionType();
		URI kiId;
		if (type.equals("AskKnowledgeInteraction")) {
			var askKI = new AskKnowledgeInteraction(ca, new GraphPattern(workaround.getGraphPattern()));
			kiId = this.sc.register(askKI);
			this.knowledgeInteractions.put(kiId, askKI);
		} else if (type.equals("AnswerKnowledgeInteraction")) {
			var answerKI = new AnswerKnowledgeInteraction(ca, new GraphPattern(workaround.getGraphPattern()));
			kiId = this.sc.register(answerKI, this.handler);
			this.knowledgeInteractions.put(kiId, answerKI);
		} else if (type.equals("PostKnowledgeInteraction")) {
			var postKI = new PostKnowledgeInteraction(ca, new GraphPattern(workaround.getArgumentGraphPattern()), new GraphPattern(workaround.getResultGraphPattern()));
			kiId = this.sc.register(postKI);
			this.knowledgeInteractions.put(kiId, postKI);
		} else if (type.equals("ReactKnowledgeInteraction")) {
			var reactKI = new ReactKnowledgeInteraction(ca, new GraphPattern(workaround.getArgumentGraphPattern()), new GraphPattern(workaround.getResultGraphPattern()));
			kiId = this.sc.register(reactKI, this.reactHandler);
			this.knowledgeInteractions.put(kiId, reactKI);
		} else {
			return null;
		}

		return kiId.toString();
	}

	private Set<Resource> toResources(List<String> strings) {
		return strings.stream().map((str) -> {
			return ResourceFactory.createProperty(str);
		}).collect(Collectors.toSet());
	}

	public WorkaroundWithId getKnowledgeInteraction(String knowledgeInteractionId) {
		throw new RuntimeException("TODO");
	}
	
	public Set<WorkaroundWithId> getKnowledgeInteractions() {
		return this.knowledgeInteractions.entrySet().stream()
			.map((Entry<URI, KnowledgeInteraction> e) -> this.kiToWorkAroundWithId(e.getKey(), e.getValue()))
			.collect(Collectors.toSet());
	}

	private WorkaroundWithId kiToWorkAroundWithId(URI kiId, KnowledgeInteraction ki) {
		var act = ki.getAct();
		var requirements = act.getRequirementPurposes().stream().map(r -> r.toString()).collect(Collectors.toList());
		var satisfactions = act.getSatisfactionPurposes().stream().map(r -> r.toString()).collect(Collectors.toList());
		var wwid = new WorkaroundWithId()
			.knowledgeInteractionId(kiId.toString())
			.communicativeAct(new eu.interconnectproject.knowledge_engine.rest.model.CommunicativeAct()
				.requiredPurposes(requirements)
				.satisfiedPurposes(satisfactions)
			);

		if (ki instanceof AskKnowledgeInteraction) {
			wwid.setKnowledgeInteractionType("AskKnowledgeInteraction");
			wwid.setGraphPattern(((AskKnowledgeInteraction) ki).getPattern().getPattern());
		} else if (ki instanceof AnswerKnowledgeInteraction) {
			wwid.setKnowledgeInteractionType("AnswerKnowledgeInteraction");
			wwid.setGraphPattern(((AnswerKnowledgeInteraction) ki).getPattern().getPattern());
		} else if (ki instanceof PostKnowledgeInteraction) {
			wwid.setKnowledgeInteractionType("PostKnowledgeInteraction");
			wwid.setArgumentGraphPattern(((PostKnowledgeInteraction) ki).getArgument().getPattern());
			wwid.setResultGraphPattern(((PostKnowledgeInteraction) ki).getResult().getPattern());
		} else if (ki instanceof ReactKnowledgeInteraction) {
			wwid.setKnowledgeInteractionType("ReactKnowledgeInteraction");
			wwid.setArgumentGraphPattern(((ReactKnowledgeInteraction) ki).getArgument().getPattern());
			wwid.setResultGraphPattern(((ReactKnowledgeInteraction) ki).getResult().getPattern());
		} else {
			assert false : "Encountered unknown knowledge interaction subclass.";
			LOG.error("Encountered a knowledge interaction instance ({}) of an unknown knowledge interaction subclass. Some properties are missing from the result!", ki);
		}

		return (WorkaroundWithId) wwid;
	}

	public boolean hasKnowledgeInteraction(String knowledgeInteractionId) {
		try {
			return this.knowledgeInteractions.containsKey(new URI(knowledgeInteractionId));
		} catch (URISyntaxException e) {
			return false;
		}
	}
	
	public void delete(String knowledgeInteractionId) {
		URI kiId;
		try {
			kiId = new URI(knowledgeInteractionId);
		} catch (URISyntaxException e) {
			LOG.warn("Tried to delete a knowledge interaction with an invalid URI '{}'. Ignored.", knowledgeInteractionId);
			return;
		}

		var ki = this.knowledgeInteractions.remove(kiId);

		if (ki == null) {
			LOG.warn("Tried to delete an unknown knowledge interaction '{}'. Ignored.", kiId);
			return;
		}

		if (ki instanceof AskKnowledgeInteraction) {
			this.sc.unregister((AskKnowledgeInteraction) ki);
		} else if (ki instanceof AnswerKnowledgeInteraction) {
			this.sc.unregister((AnswerKnowledgeInteraction) ki);
		} else if (ki instanceof PostKnowledgeInteraction) {
			this.sc.unregister((PostKnowledgeInteraction) ki);
		} else if (ki instanceof ReactKnowledgeInteraction) {
			this.sc.unregister((ReactKnowledgeInteraction) ki);
		} else {
			assert false : "Encountered unknown knowledge interaction subclass.";
			LOG.error("Encountered a knowledge interaction instance ({}) of an unknown knowledge interaction subclass. It has not been unregistered from the smart connector!", ki);
		}
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
