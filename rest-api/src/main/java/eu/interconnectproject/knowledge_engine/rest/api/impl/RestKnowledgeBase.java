package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.interconnectproject.knowledge_engine.rest.model.AskExchangeInfo;
import eu.interconnectproject.knowledge_engine.rest.model.AskResult;
import eu.interconnectproject.knowledge_engine.rest.model.KnowledgeInteractionBase;
import eu.interconnectproject.knowledge_engine.rest.model.KnowledgeInteractionWithId;
import eu.interconnectproject.knowledge_engine.rest.model.PostExchangeInfo;
import eu.interconnectproject.knowledge_engine.rest.model.PostResult;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerHandler;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AskKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.Binding;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.CommunicativeAct;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ExchangeInfo.Initiator;
import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.PostKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactHandler;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.KnowledgeInteractionInfo;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorBuilder;

public class RestKnowledgeBase implements KnowledgeBase {
	private static final Logger LOG = LoggerFactory.getLogger(RestKnowledgeBase.class);

	private String knowledgeBaseId;
	private String knowledgeBaseName;
	private String knowledgeBaseDescription;

	private AtomicInteger handleRequestId;

	/**
	 * Can be null, if no connection with the client is available.
	 */
	private AsyncResponse asyncResponse;

	/**
	 * The Smart connector of this KB asks us to handle a certain request
	 * (Ask,Post). These should be send to the asyncContext, but if it is not
	 * available, it will be placed on a queue.
	 */
	private Queue<HandleRequest> toBeProcessedHandleRequests;

	/**
	 * The client has received the handle request and it is currently being
	 * processed, once it has been processed and the results come in, we send the
	 * data to the smart connector.
	 */
	private Map<Integer, HandleRequest> beingProcessedHandleRequests;
	private ObjectMapper om = new ObjectMapper();
	private SmartConnector sc;
	private Map<URI, KnowledgeInteraction> knowledgeInteractions;

	private AnswerHandler answerHandler = new AnswerHandler() {

		@Override
		public CompletableFuture<BindingSet> answerAsync(AnswerKnowledgeInteraction anAKI, BindingSet aBindingSet) {

			CompletableFuture<BindingSet> future = new CompletableFuture<>();
			List<Map<String, String>> bindings = bindingSetToList(aBindingSet);

			int previous = handleRequestId.get();
			int myHandleRequestId = handleRequestId.incrementAndGet();
			int next = handleRequestId.get();
			assert previous + 1 == next;

			HandleRequest hr = new HandleRequest(myHandleRequestId, (KnowledgeInteraction) anAKI,
					KnowledgeInteractionInfo.Type.ANSWER, bindings, future);

			toBeProcessedByKnowledgeBase(hr);
			return future;
		}

		public BindingSet answer(AnswerKnowledgeInteraction anAKI, BindingSet aBindingSet) {
			throw new IllegalArgumentException("Should not be called.");
		}
	};

	private ReactHandler reactHandler = new ReactHandler() {

		@Override
		public CompletableFuture<BindingSet> reactAsync(ReactKnowledgeInteraction aRKI, BindingSet aBindingSet) {

			CompletableFuture<BindingSet> future = new CompletableFuture<>();
			List<Map<String, String>> bindings = bindingSetToList(aBindingSet);
			int myHandleRequestId = handleRequestId.incrementAndGet();
			HandleRequest hr = new HandleRequest(myHandleRequestId, (KnowledgeInteraction) aRKI,
					KnowledgeInteractionInfo.Type.REACT, bindings, future);

			toBeProcessedByKnowledgeBase(hr);
			return future;
		}

		public BindingSet react(ReactKnowledgeInteraction aRKI, BindingSet aBindingSet) {
			throw new IllegalArgumentException("Should not be called.");
		}
	};

	public RestKnowledgeBase(eu.interconnectproject.knowledge_engine.rest.model.SmartConnector scModel) {
		this.knowledgeBaseId = scModel.getKnowledgeBaseId();
		this.knowledgeBaseName = scModel.getKnowledgeBaseName();
		this.knowledgeBaseDescription = scModel.getKnowledgeBaseDescription();
		this.knowledgeInteractions = new HashMap<>();
		this.toBeProcessedHandleRequests = new ArrayBlockingQueue<>(50);
		this.beingProcessedHandleRequests = Collections.synchronizedMap(new HashMap<Integer, HandleRequest>());
		this.handleRequestId = new AtomicInteger(0);

		this.sc = SmartConnectorBuilder.newSmartConnector(this).knowledgeBaseIsThreadSafe(true).create();
	}

	protected void toBeProcessedByKnowledgeBase(HandleRequest handleRequest) {

		if (asyncResponse != null) {
			this.beingProcessedHandleRequests.put(handleRequest.getHandleRequestId(), handleRequest);
			// immediately process
			// retrieve corresponding KnowledgeInteractionId
			if (this.knowledgeInteractions.containsValue(handleRequest.getKnowledgeInteraction())) {

				String knowledgeInteractionId = null;
				for (var entry : this.knowledgeInteractions.entrySet()) {
					if (entry.getValue().equals(handleRequest.getKnowledgeInteraction())) {
						knowledgeInteractionId = entry.getKey().toString();
					}
				}
				assert knowledgeInteractionId != null;

				eu.interconnectproject.knowledge_engine.rest.model.HandleRequest object = new eu.interconnectproject.knowledge_engine.rest.model.HandleRequest()
						.bindingSet(handleRequest.getBindingSet()).handleRequestId(handleRequest.getHandleRequestId())
						.knowledgeInteractionId(knowledgeInteractionId);

				this.asyncResponse.resume(Response.status(200).entity(object).build());
				this.asyncResponse = null;
			}

		} else {
			// add to queue
			this.toBeProcessedHandleRequests.add(handleRequest);
		}

	}

	public boolean hasAsyncResponse() {
		return this.asyncResponse != null;
	}

	public void resetAsyncResponse() {
		this.asyncResponse = null;
	}

	public void waitForHandleRequest(AsyncResponse asyncResponse) {

		this.asyncResponse = asyncResponse;
		HandleRequest hr = toBeProcessedHandleRequests.poll();
		if (hr != null) {
			beingProcessedHandleRequests.put(hr.getHandleRequestId(), hr);
			// there is a handle request waiting
			toBeProcessedByKnowledgeBase(hr);
		}
	}

	public boolean hasHandleRequestId(int handleRequestId) {
		return this.beingProcessedHandleRequests.containsKey(handleRequestId);
	}

	/**
	 * Called when the REST client sends us some bindings as an answer or reaction.
	 * 
	 * @param knowledgeInteractionId
	 * @param responseBody
	 */
	public void finishHandleRequest(String knowledgeInteractionId,
			eu.interconnectproject.knowledge_engine.rest.model.HandleResponse responseBody) {

		int handleRequestId = responseBody.getHandleRequestId();
		HandleRequest hr = this.beingProcessedHandleRequests.remove(handleRequestId);
		BindingSet bs = this.listToBindingSet(responseBody.getBindingSet());

		// TODO: Can this be moved to somewhere internal so that it can also be
		// caught in the Java developer api?
		// See https://gitlab.inesctec.pt/interconnect/knowledge-engine/-/issues/148
		try {
			hr.validateBindings(bs);
		} catch (IllegalArgumentException e) {
			hr.getFuture().completeExceptionally(e);
			throw e;
		}

		hr.getFuture().complete(bs);
	}

	public String register(KnowledgeInteractionBase ki) {
		CommunicativeAct ca;
		if (ki.getCommunicativeAct() != null) {
			ca = new CommunicativeAct(toResources(ki.getCommunicativeAct().getRequiredPurposes()),
					toResources(ki.getCommunicativeAct().getSatisfiedPurposes()));
		} else {
			ca = new CommunicativeAct();
		}

		String type = ki.getKnowledgeInteractionType();
		URI kiId;
		if (type.equals("AskKnowledgeInteraction")) {

			eu.interconnectproject.knowledge_engine.rest.model.AskKnowledgeInteraction aki = (eu.interconnectproject.knowledge_engine.rest.model.AskKnowledgeInteraction) ki;

			if (aki.getGraphPattern() == null) {
				throw new IllegalArgumentException("graphPattern must be given for ASK knowledge interactions.");
			}
			var askKI = new AskKnowledgeInteraction(ca, new GraphPattern(aki.getGraphPattern()));
			kiId = this.sc.register(askKI);
			this.knowledgeInteractions.put(kiId, askKI);
		} else if (type.equals("AnswerKnowledgeInteraction")) {

			eu.interconnectproject.knowledge_engine.rest.model.AnswerKnowledgeInteraction aki = (eu.interconnectproject.knowledge_engine.rest.model.AnswerKnowledgeInteraction) ki;

			if (aki.getGraphPattern() == null) {
				throw new IllegalArgumentException("graphPattern must be given for ANSWER knowledge interactions.");
			}
			var answerKI = new AnswerKnowledgeInteraction(ca, new GraphPattern(aki.getGraphPattern()));

			kiId = this.sc.register(answerKI, this.answerHandler);

			this.knowledgeInteractions.put(kiId, answerKI);
		} else if (type.equals("PostKnowledgeInteraction")) {

			eu.interconnectproject.knowledge_engine.rest.model.PostKnowledgeInteraction pki = (eu.interconnectproject.knowledge_engine.rest.model.PostKnowledgeInteraction) ki;

			GraphPattern argGP = null;
			GraphPattern resGP = null;

			if (pki.getArgumentGraphPattern() != null) {
				argGP = new GraphPattern(pki.getArgumentGraphPattern());
			}
			if (pki.getResultGraphPattern() != null) {
				resGP = new GraphPattern(pki.getResultGraphPattern());
			}

			if (resGP == null && argGP == null) {
				throw new IllegalArgumentException(
						"At least one of argumentGraphPattern and resultGraphPattern must be given for POST knowledge interactions.");
			}

			var postKI = new PostKnowledgeInteraction(ca, argGP, resGP);
			kiId = this.sc.register(postKI);

			this.knowledgeInteractions.put(kiId, postKI);
		} else if (type.equals("ReactKnowledgeInteraction")) {

			eu.interconnectproject.knowledge_engine.rest.model.ReactKnowledgeInteraction rki = (eu.interconnectproject.knowledge_engine.rest.model.ReactKnowledgeInteraction) ki;

			GraphPattern argGP = null;
			GraphPattern resGP = null;

			if (rki.getArgumentGraphPattern() != null) {
				argGP = new GraphPattern(rki.getArgumentGraphPattern());
			}
			if (rki.getResultGraphPattern() != null) {
				resGP = new GraphPattern(rki.getResultGraphPattern());
			}

			if (resGP == null && argGP == null) {
				throw new IllegalArgumentException(
						"At least one of argumentGraphPattern and resultGraphPattern must be given for REACT knowledge interactions.");
			}

			var reactKI = new ReactKnowledgeInteraction(ca, argGP, resGP);
			kiId = this.sc.register(reactKI, this.reactHandler);

			this.knowledgeInteractions.put(kiId, reactKI);
		} else {
			throw new IllegalArgumentException(String.format(
					"Unexpected value for knowledgeInteractionType: %s. Must be one of: AskKnowledgeInteraction, AnswerKnowledgeInteraction, PostKnowledgeInteraction, ReactKnowledgeInteraction",
					type));
		}

		return kiId.toString();
	}

	private Set<Resource> toResources(List<String> strings) {
		return strings.stream().map((str) -> {
			return ResourceFactory.createProperty(str);
		}).collect(Collectors.toSet());
	}

	public KnowledgeInteractionWithId getKnowledgeInteraction(String knowledgeInteractionId) {

		URI kiId;
		try {
			kiId = new URI(knowledgeInteractionId);

			assert this.knowledgeInteractions.containsKey(kiId);

			return kiToModelKiWithId(kiId, this.knowledgeInteractions.get(kiId));

		} catch (URISyntaxException e) {
			assert false : "There should never occur an invalid URI here because it should have been checked in the service implementation.";
		}
		return null;

	}

	public Set<KnowledgeInteractionWithId> getKnowledgeInteractions() {
		return this.knowledgeInteractions.entrySet().stream().map(e -> this.kiToModelKiWithId(e.getKey(), e.getValue()))
				.collect(Collectors.toSet());
	}

	private KnowledgeInteractionWithId kiToModelKiWithId(URI kiId, KnowledgeInteraction ki) {
		var act = ki.getAct();
		var requirements = act.getRequirementPurposes().stream().map(r -> r.toString()).collect(Collectors.toList());
		var satisfactions = act.getSatisfactionPurposes().stream().map(r -> r.toString()).collect(Collectors.toList());
		var kiwid = new KnowledgeInteractionWithId().knowledgeInteractionId(kiId.toString())
				.communicativeAct(new eu.interconnectproject.knowledge_engine.rest.model.CommunicativeAct()
						.requiredPurposes(requirements).satisfiedPurposes(satisfactions));

		if (ki instanceof AskKnowledgeInteraction) {
			kiwid.setKnowledgeInteractionType("AskKnowledgeInteraction");
			kiwid.setGraphPattern(((AskKnowledgeInteraction) ki).getPattern().getPattern());
		} else if (ki instanceof AnswerKnowledgeInteraction) {
			kiwid.setKnowledgeInteractionType("AnswerKnowledgeInteraction");
			kiwid.setGraphPattern(((AnswerKnowledgeInteraction) ki).getPattern().getPattern());
		} else if (ki instanceof PostKnowledgeInteraction) {
			kiwid.setKnowledgeInteractionType("PostKnowledgeInteraction");
			var pKi = (PostKnowledgeInteraction) ki;
			kiwid.setArgumentGraphPattern(pKi.getArgument().getPattern());
			if (pKi.getResult() != null) {
				kiwid.setResultGraphPattern(pKi.getResult().getPattern());
			}
		} else if (ki instanceof ReactKnowledgeInteraction) {
			kiwid.setKnowledgeInteractionType("ReactKnowledgeInteraction");
			var rKi = (ReactKnowledgeInteraction) ki;
			kiwid.setArgumentGraphPattern(rKi.getArgument().getPattern());
			if (rKi.getResult() != null) {
				kiwid.setResultGraphPattern(rKi.getResult().getPattern());
			}
		} else {
			assert false : "Encountered unknown knowledge interaction subclass.";
			LOG.error(
					"Encountered a knowledge interaction instance ({}) of an unknown knowledge interaction subclass. Some properties are missing from the result!",
					ki);
		}

		return (KnowledgeInteractionWithId) kiwid;
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
			LOG.warn("Tried to delete a knowledge interaction with an invalid URI '{}'. Ignored.",
					knowledgeInteractionId);
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
			LOG.error(
					"Encountered a knowledge interaction instance ({}) of an unknown knowledge interaction subclass. It has not been unregistered from the smart connector!",
					ki);
		}
	}

	public AskResult ask(String kiId, List<Map<String, String>> bindings)
			throws URISyntaxException, InterruptedException, ExecutionException {
		KnowledgeInteraction ki;
		try {
			ki = this.knowledgeInteractions.get(new URI(kiId));
		} catch (URISyntaxException e1) {
			assert false : "Encountered invalid URI for knowledge interaction.: " + kiId;
			throw e1;
		}

		if (!(ki instanceof AskKnowledgeInteraction)) {
			throw new IllegalArgumentException(String.format(
					"Knowledge interaction '%s' is not an ASK knowledge interaction, but the request tried to use it as one.",
					kiId));
		}
		// ASK the bindings to the smart connector and wait for a response. If
		// anything misbehaves, this will throw and it's up to the caller of this
		// method to handle it.
		var askResult = this.sc.ask((AskKnowledgeInteraction) ki, listToBindingSet(bindings)).get();

		return new AskResult().bindingSet(this.bindingSetToList(askResult.getBindings())).exchangeInfo(askResult
				.getExchangeInfoPerKnowledgeBase().stream()
				.map(aei -> new AskExchangeInfo().bindingSet(this.bindingSetToList(aei.getBindings()))
						.knowledgeBaseId(aei.getKnowledgeBaseId().toString())
						.knowledgeInteractionId(aei.getKnowledgeInteractionId().toString())
						.initiator(toInitiatorEnumAsk(aei.getInitiator()))
						.exchangeStart(Date.from(aei.getExchangeStart())).exchangeEnd(Date.from(aei.getExchangeEnd()))
						.status(aei.getStatus().toString()).failedMessage(aei.getFailedMessage()))
				.collect(Collectors.toList()));
	}

	public PostResult post(String kiId, List<Map<String, String>> bindings)
			throws URISyntaxException, InterruptedException, ExecutionException {
		KnowledgeInteraction ki;
		try {
			ki = this.knowledgeInteractions.get(new URI(kiId));
		} catch (URISyntaxException e1) {
			assert false : "Encountered invalid URI for knowledge interaction.: " + kiId;
			throw e1;
		}

		if (!(ki instanceof PostKnowledgeInteraction)) {
			throw new IllegalArgumentException(String.format(
					"Knowledge interaction '%s' is not a POST knowledge interaction, but the request tried to use it as one.",
					kiId));
		}

		// POST the bindings to the smart connector and wait for a response. If
		// anything misbehaves, this will throw and it's up to the caller of this
		// method to handle it.
		var postResult = this.sc.post((PostKnowledgeInteraction) ki, listToBindingSet(bindings)).get();

		return new PostResult().resultBindingSet(this.bindingSetToList(postResult.getBindings()))
				.exchangeInfo(postResult.getExchangeInfoPerKnowledgeBase().stream()
						.map(pei -> new PostExchangeInfo().argumentBindingSet(this.bindingSetToList(pei.getArgument()))
								.resultBindingSet(this.bindingSetToList(pei.getResult()))
								.knowledgeBaseId(pei.getKnowledgeBaseId().toString())
								.knowledgeInteractionId(pei.getKnowledgeInteractionId().toString())
								.initiator(toInitiatorEnumPost(pei.getInitiator()))
								.exchangeStart(Date.from(pei.getExchangeStart()))
								.exchangeEnd(Date.from(pei.getExchangeEnd())).status(pei.getStatus().toString())
								.failedMessage(pei.getFailedMessage()))
						.collect(Collectors.toList()));
	}

	private PostExchangeInfo.InitiatorEnum toInitiatorEnumPost(Initiator initiator) {
		switch (initiator) {
		case KNOWLEDGEBASE:
			return PostExchangeInfo.InitiatorEnum.KNOWLEDGEBASE;
		case REASONER:
			return PostExchangeInfo.InitiatorEnum.REASONER;
		default:
			assert false;
			return null;
		}
	}

	private AskExchangeInfo.InitiatorEnum toInitiatorEnumAsk(Initiator initiator) {
		switch (initiator) {
		case KNOWLEDGEBASE:
			return AskExchangeInfo.InitiatorEnum.KNOWLEDGEBASE;
		case REASONER:
			return AskExchangeInfo.InitiatorEnum.REASONER;
		default:
			assert false;
			return null;
		}
	}

	private BindingSet listToBindingSet(List<Map<String, String>> listBindings) {
		var bindings = new BindingSet();
		listBindings.forEach((listBinding) -> {
			var binding = new Binding();
			listBinding.forEach((k, v) -> {
				binding.put(k, v);
			});
			bindings.add(binding);
		});
		return bindings;
	}

	private List<Map<String, String>> bindingSetToList(BindingSet bindings) {
		var listBindings = new ArrayList<Map<String, String>>(bindings.size());
		bindings.forEach((binding) -> {
			Map<String, String> listBinding = new HashMap<>();
			binding.forEach((k, v) -> {
				listBinding.put(k, v);
			});
			listBindings.add(listBinding);
		});
		return listBindings;
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

	public void stop() {
		this.sc.stop();
	}
}
