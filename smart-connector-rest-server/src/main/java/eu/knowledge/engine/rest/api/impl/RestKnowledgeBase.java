package eu.knowledge.engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.reasoner.rulesys.Rule.ParserException;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.util.JenaRules;
import eu.knowledge.engine.rest.model.KnowledgeInteractionBase;
import eu.knowledge.engine.rest.model.KnowledgeInteractionWithId;
import eu.knowledge.engine.rest.model.ResponseMessage;
import eu.knowledge.engine.rest.model.SmartConnectorLease;
import eu.knowledge.engine.smartconnector.api.AnswerExchangeInfo;
import eu.knowledge.engine.smartconnector.api.AnswerHandler;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeBase;
import eu.knowledge.engine.smartconnector.api.KnowledgeEngineException;
import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.MatchStrategy;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.ReactExchangeInfo;
import eu.knowledge.engine.smartconnector.api.ReactHandler;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.RecipientSelector;
import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.api.SmartConnectorProvider;
import eu.knowledge.engine.smartconnector.api.SmartConnectorSPI;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.Response;

public class RestKnowledgeBase implements KnowledgeBase {
	private static final Logger LOG = LoggerFactory.getLogger(RestKnowledgeBase.class);

	public static int INACTIVITY_TIMEOUT_SECONDS = 60;

	/**
	 * A way to allow the RestServer to use different versions of the Smart
	 * Connector (typically v1 and v2). We assume there is only a single provider on
	 * the classpath.
	 */
	private static SmartConnectorProvider smartConnectorProvider = null;

	static {
		Iterator<SmartConnectorProvider> iter = SmartConnectorSPI.providers(true);
		if (iter.hasNext()) {
			smartConnectorProvider = iter.next();
		} else {
			LOG.error(
					"SmartConnectorProvider not initialized. Make sure there is a SmartConnectorProvider implementation registered on the classpath.");
		}
	}

	private String knowledgeBaseId;
	private String knowledgeBaseName;
	private String knowledgeBaseDescription;

	private AtomicInteger handleRequestId;

	private final Object asyncResponseLock = new Object();

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
	private final Map<Integer, HandleRequest> beingProcessedHandleRequests;
	private SmartConnector sc;
	private Map<URI, KnowledgeInteraction> knowledgeInteractions;

	private static int QUEUE_SIZE = 50;

	private AnswerHandler answerHandler = new AnswerHandler() {

		/**
		 * Creates a future for the response of this knowledge base, and passes it to
		 * the knowledge base to be processed.
		 */
		@Override
		public CompletableFuture<BindingSet> answerAsync(AnswerKnowledgeInteraction anAKI,
				AnswerExchangeInfo anAnswerExchangeInfo) {
			var aBindingSet = anAnswerExchangeInfo.getIncomingBindings();
			CompletableFuture<BindingSet> future = new CompletableFuture<>();
			List<Map<String, String>> bindings = bindingSetToList(aBindingSet);

			int previous = handleRequestId.get();
			int myHandleRequestId = handleRequestId.incrementAndGet();
			int next = handleRequestId.get();
			assert previous + 1 == next;

			HandleRequest hr = new HandleRequest(myHandleRequestId, (KnowledgeInteraction) anAKI,
					KnowledgeInteractionType.ANSWER, bindings, anAnswerExchangeInfo.getAskingKnowledgeBaseId(), future);

			tryProcessHandleRequestElseEnqueue(hr);
			return future;
		}

		/**
		 * By overriding the {@link AnswerHandler#answerAsync} method, this method
		 * should no longer be called. But we still have to implement it, which is
		 * unfortunate.
		 */
		public BindingSet answer(AnswerKnowledgeInteraction anAKI, AnswerExchangeInfo anAnswerExchangeInfo) {
			throw new IllegalArgumentException("Should not be called.");
		}
	};

	private ReactHandler reactHandler = new ReactHandler() {

		/**
		 * Creates a future for the response of this knowledge base, and passes it to
		 * the knowledge base to be processed.
		 */
		@Override
		public CompletableFuture<BindingSet> reactAsync(ReactKnowledgeInteraction aRKI,
				ReactExchangeInfo aReactExchangeInfo) {

			CompletableFuture<BindingSet> future = new CompletableFuture<>();
			List<Map<String, String>> bindings = bindingSetToList(aReactExchangeInfo.getArgumentBindings());
			int myHandleRequestId = handleRequestId.incrementAndGet();
			HandleRequest hr = new HandleRequest(myHandleRequestId, (KnowledgeInteraction) aRKI,
					KnowledgeInteractionType.REACT, bindings, aReactExchangeInfo.getPostingKnowledgeBaseId(), future);

			tryProcessHandleRequestElseEnqueue(hr);
			return future;
		}

		/**
		 * By overriding the {@link ReactHandler#reactAsync} method, this method should
		 * no longer be called. But we still have to implement it, which is unfortunate.
		 */
		public BindingSet react(ReactKnowledgeInteraction aRKI, ReactExchangeInfo aReactExchangeInfo) {
			throw new IllegalArgumentException("Should not be called.");
		}
	};

	/**
	 * Runnable to run when smart connector is ready. This currently triggers the
	 * response to a successful /sc POST request.
	 */
	private final Runnable onReady;

	/**
	 * The lease that must be periodically renewed to keep using the smart
	 * connector. If this is `null`, the lease is permanent.
	 */
	private final SmartConnectorLease lease;

	/**
	 * The renewal time of the lease.
	 */
	private final Integer leaseRenewalTime;

	private Timer inactivityTimer;

	private boolean suspended = false;

	public RestKnowledgeBase(eu.knowledge.engine.rest.model.SmartConnector scModel, final Runnable onReady) {
		this.knowledgeBaseId = scModel.getKnowledgeBaseId();
		this.knowledgeBaseName = scModel.getKnowledgeBaseName();
		this.knowledgeBaseDescription = scModel.getKnowledgeBaseDescription();
		this.knowledgeInteractions = new HashMap<>();
		this.toBeProcessedHandleRequests = new ArrayBlockingQueue<>(QUEUE_SIZE);

		// use a mapping with a maximum capacity and removing the oldest one if new
		// entries come in when the max capacity is reached. When a KB accepts many
		// handle requests, but fails to respond to many of them, this causes a memory
		// leak. With the following code we basically say that a KB is only allowed to
		// have 100 parallel handle requests outstanding.
		this.beingProcessedHandleRequests = Collections.synchronizedMap(new LinkedHashMap<Integer, HandleRequest>() {
			private static final long serialVersionUID = 1L;
			private static final int MAX_ENTRIES = 100;

			@Override
			protected boolean removeEldestEntry(Map.Entry<Integer, HandleRequest> eldest) {
				if (size() > MAX_ENTRIES) {
					eldest.getValue().getFuture()
							.completeExceptionally(new KnowledgeEngineException(new Exception("Knowledge base "
									+ RestKnowledgeBase.this.knowledgeBaseId + " should have max " + MAX_ENTRIES
									+ " outstanding handle requests. Cancelling oldest handle request "
									+ eldest.getValue().getHandleRequestId() + ".")));
					return true;
				} else {
					return false;
				}
			}
		});

		this.handleRequestId = new AtomicInteger(0);
		this.onReady = onReady;
		this.leaseRenewalTime = scModel.getLeaseRenewalTime();

		assert RestKnowledgeBase.INACTIVITY_TIMEOUT_SECONDS > ReactiveApiServiceImpl.LONGPOLL_TIMEOUT;

		if (this.leaseRenewalTime != null) {
			// Issue the initial lease.
			LOG.info("Creating REST Knowledge Base with lease that must be renewed every {} seconds.",
					this.leaseRenewalTime);

			Date expiration = new Date();
			expiration.setTime(expiration.getTime() + 1000 * this.leaseRenewalTime);

			this.lease = new SmartConnectorLease().knowledgeBaseId(this.knowledgeBaseId).expires(expiration);
		} else {
			this.lease = null;
		}

		if (smartConnectorProvider == null) {
			throw new IllegalStateException(
					"SmartConnectorProvider not initialized. Make sure there is a SmartConnectorProvider implementation registered on the classpath.");
		}
		this.sc = smartConnectorProvider.create(this);

		if (scModel.getReasonerLevel() != null)
			this.sc.setReasonerLevel(scModel.getReasonerLevel());
	}

	protected void tryProcessHandleRequestElseEnqueue(HandleRequest handleRequest) {
		boolean sentToKnowledgeBase = false;
		synchronized (this.asyncResponseLock) {
			if (this.asyncResponse != null) {
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

					eu.knowledge.engine.rest.model.HandleRequest handleRequestModel = new eu.knowledge.engine.rest.model.HandleRequest()
							.bindingSet(handleRequest.getBindingSet())
							.handleRequestId(handleRequest.getHandleRequestId())
							.knowledgeInteractionId(knowledgeInteractionId);

					if (handleRequest.getRequestingKnowledgeBaseId() != null) {
						handleRequestModel
								.requestingKnowledgeBaseId(handleRequest.getRequestingKnowledgeBaseId().toString());
					}

					sentToKnowledgeBase = this.asyncResponse
							.resume(Response.status(200).entity(handleRequestModel).build());
					// Even if unsuccesful, we want to reset the asyncResponse object, as it
					// is somehow faulty. So we will wait for a new request.
					this.resetAsyncResponse();
				}
			}
		}

		if (!sentToKnowledgeBase) {
			// Offer a new item to the queue
			var enqueued = this.toBeProcessedHandleRequests.offer(handleRequest);
			if (!enqueued) {
				// If unsuccessfull, remove the oldest item and complete it exceptionally.
				HandleRequest oldest = this.toBeProcessedHandleRequests.remove();
				oldest.getFuture().completeExceptionally(new KnowledgeEngineException(
						new Exception("Handle request queue is full. This oldest request has been cancelled.")));

				// And then try again forcibly this time.
				try {
					this.toBeProcessedHandleRequests.add(handleRequest);
				} catch (IllegalStateException e) {
					// If this ALSO failed, we will cancel this new item as well and log.
					handleRequest.getFuture().completeExceptionally(new KnowledgeEngineException(
							new Exception("It was not possible to add this request to the knowledge base's queue.")));
					LOG.warn("Could not add handle request to queue of knowledge base {}, even after removing an item.",
							this.knowledgeBaseId);
					LOG.debug("This handle request couldn't be added: {}", handleRequest);
				}
			}
		}
	}

	public boolean hasAsyncResponse() {
		return this.asyncResponse != null;
	}

	public void resetAsyncResponse() {
		this.asyncResponse = null;
		this.resetInactivityTimeout();
	}

	public void waitForHandleRequest(AsyncResponse asyncResponse) {

		this.asyncResponse = asyncResponse;
		HandleRequest hr = toBeProcessedHandleRequests.poll();
		if (hr != null) {
			beingProcessedHandleRequests.put(hr.getHandleRequestId(), hr);
			// there is a handle request waiting
			tryProcessHandleRequestElseEnqueue(hr);
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
			eu.knowledge.engine.rest.model.HandleResponse responseBody) {

		int handleRequestId = responseBody.getHandleRequestId();

		HandleRequest hr = null;
		BindingSet bs = null;

		try {

			synchronized (this.beingProcessedHandleRequests) {
				hr = this.beingProcessedHandleRequests.get(handleRequestId);
				bs = this.listToBindingSet(responseBody.getBindingSet());

				// Moved the validation to the {@link
				// eu.knowledge.engine.smartconnector.impl.InteractionProcessorImpl} so that
				// also the Java API benefits this, but unfortunately we also have to validate
				// here to be able to return an error to the Knowledge Base using the REST API.
				hr.validateBindings(bs);

				// Now that the validation is done, from the reactive side we are done, so
				// we can remove the HandleRequest from our list.
				this.beingProcessedHandleRequests.remove(handleRequestId);
			}

		} finally {
			// we always want to complete the future, also when the binding set is invalid.
			if (hr != null && bs != null) {
				hr.getFuture().complete(bs);
			}
		}
	}

	public String register(KnowledgeInteractionBase ki) {
		CommunicativeAct ca;
		if (ki.getCommunicativeAct() != null) {
			ca = new CommunicativeAct(toResources(ki.getCommunicativeAct().getRequiredPurposes()),
					toResources(ki.getCommunicativeAct().getSatisfiedPurposes()));
		} else {
			ca = new CommunicativeAct();
		}

		PrefixMapping prefixMapping;
		if (ki.getPrefixes() != null) {
			prefixMapping = new PrefixMappingMem();
			prefixMapping.setNsPrefixes(ki.getPrefixes());
		} else {
			prefixMapping = new PrefixMappingZero();
		}

		boolean knowledgeGapsEnabled = ki.getKnowledgeGapsEnabled() == null ? false : ki.getKnowledgeGapsEnabled();

		String type = ki.getKnowledgeInteractionType();
		URI kiId;
		if (type.equals("AskKnowledgeInteraction")) {

			eu.knowledge.engine.rest.model.AskKnowledgeInteraction aki = (eu.knowledge.engine.rest.model.AskKnowledgeInteraction) ki;

			if (aki.getGraphPattern() == null) {
				throw new IllegalArgumentException("graphPattern must be given for ASK knowledge interactions.");
			}

			MatchStrategy strategy = null;
			if (aki.getKnowledgeGapsEnabled() != null && aki.getKnowledgeGapsEnabled()) {
				strategy = MatchStrategy.SUPREME_LEVEL;
				LOG.info("The MatchStrategy should be '{}' when Knowledge Gaps are enabled. Overriding default.",
						MatchStrategy.SUPREME_LEVEL);
			}

			var askKI = new AskKnowledgeInteraction(ca, new GraphPattern(prefixMapping, aki.getGraphPattern()),
					ki.getKnowledgeInteractionName(), false, false, knowledgeGapsEnabled, strategy);

			kiId = this.sc.register(askKI);
			this.knowledgeInteractions.put(kiId, askKI);
		} else if (type.equals("AnswerKnowledgeInteraction")) {

			eu.knowledge.engine.rest.model.AnswerKnowledgeInteraction aki = (eu.knowledge.engine.rest.model.AnswerKnowledgeInteraction) ki;

			if (aki.getGraphPattern() == null) {
				throw new IllegalArgumentException("graphPattern must be given for ANSWER knowledge interactions.");
			}
			var answerKI = new AnswerKnowledgeInteraction(ca, new GraphPattern(prefixMapping, aki.getGraphPattern()),
					ki.getKnowledgeInteractionName());

			kiId = this.sc.register(answerKI, this.answerHandler);

			this.knowledgeInteractions.put(kiId, answerKI);
		} else if (type.equals("PostKnowledgeInteraction")) {

			eu.knowledge.engine.rest.model.PostKnowledgeInteraction pki = (eu.knowledge.engine.rest.model.PostKnowledgeInteraction) ki;

			GraphPattern argGP = null;
			GraphPattern resGP = null;

			if (pki.getArgumentGraphPattern() != null) {
				argGP = new GraphPattern(prefixMapping, pki.getArgumentGraphPattern());
			}
			if (pki.getResultGraphPattern() != null) {
				resGP = new GraphPattern(prefixMapping, pki.getResultGraphPattern());
			}

			if (resGP == null && argGP == null) {
				throw new IllegalArgumentException(
						"At least one of argumentGraphPattern and resultGraphPattern must be given for POST knowledge interactions.");
			}

			var postKI = new PostKnowledgeInteraction(ca, argGP, resGP, ki.getKnowledgeInteractionName());
			kiId = this.sc.register(postKI);

			this.knowledgeInteractions.put(kiId, postKI);
		} else if (type.equals("ReactKnowledgeInteraction")) {

			eu.knowledge.engine.rest.model.ReactKnowledgeInteraction rki = (eu.knowledge.engine.rest.model.ReactKnowledgeInteraction) ki;

			GraphPattern argGP = null;
			GraphPattern resGP = null;

			if (rki.getArgumentGraphPattern() != null) {
				argGP = new GraphPattern(prefixMapping, rki.getArgumentGraphPattern());
			}
			if (rki.getResultGraphPattern() != null) {
				resGP = new GraphPattern(prefixMapping, rki.getResultGraphPattern());
			}

			if (resGP == null && argGP == null) {
				throw new IllegalArgumentException(
						"At least one of argumentGraphPattern and resultGraphPattern must be given for REACT knowledge interactions.");
			}

			var reactKI = new ReactKnowledgeInteraction(ca, argGP, resGP, ki.getKnowledgeInteractionName());
			kiId = this.sc.register(reactKI, this.reactHandler);

			this.knowledgeInteractions.put(kiId, reactKI);
		} else {
			throw new IllegalArgumentException(String.format(
					"Unexpected value for knowledgeInteractionType: %s. Must be one of: AskKnowledgeInteraction, AnswerKnowledgeInteraction, PostKnowledgeInteraction, ReactKnowledgeInteraction",
					type));
		}

		// If this is a reactive knowledge interaction we set the inactivity timout
		// timer at the moment of registration. Note that this will not overwrite
		// any existing timers (it may not be the first reactive KI of this KB).
		if (type.equals("AnswerKnowledgeInteraction") || type.equals("ReactKnowledgeInteraction"))
			this.setInactivityTimeout(false);

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
				.knowledgeInteractionName(ki.getName()).knowledgeGapsEnabled(ki.knowledgeGapsEnabled())
				.communicativeAct(new eu.knowledge.engine.rest.model.CommunicativeAct().requiredPurposes(requirements)
						.satisfiedPurposes(satisfactions));

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

	public CompletableFuture<eu.knowledge.engine.smartconnector.api.AskResult> ask(String kiId,
			RecipientSelector recipientSelector, List<Map<String, String>> bindings)
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
		var askFuture = this.sc.ask((AskKnowledgeInteraction) ki, recipientSelector, listToBindingSet(bindings));

		return askFuture.handle((r, e) -> {

			if (r == null) {
				LOG.error("An exception has occured while asking ", e);
				return null;
			} else {
				return r;
			}
		});
	}

	public CompletableFuture<eu.knowledge.engine.smartconnector.api.PostResult> post(String kiId,
			RecipientSelector recipientSelector, List<Map<String, String>> bindings)
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
		var postFuture = this.sc.post((PostKnowledgeInteraction) ki, recipientSelector, listToBindingSet(bindings));

		return postFuture.handle((r, e) -> {

			if (r == null) {
				LOG.error("An exception has occured while posting ", e);
				return null;
			} else {
				return r;
			}
		});
	}

	private BindingSet listToBindingSet(List<Map<String, String>> listBindings) {
		var bindings = new BindingSet();
		listBindings.forEach((listBinding) -> {
			if (listBinding == null)
				throw new IllegalArgumentException("Bindings must be non-null.");
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
		this.onReady.run();
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
		if (this.inactivityTimer != null) {
			this.inactivityTimer.cancel();
			LOG.info("canceled inactivity timer for {} because its smart connector stopped.",
					this.getKnowledgeBaseId());
		}
	}

	private void cancelAsyncResponse() {

		var response = new ResponseMessage();
		response.setMessageType("message");
		response.setMessage(
				"This long polling request is cancelled by the server because the Knowledge Base is stopping.");
		boolean cancelledSucceeded = this.asyncResponse.resume(Response.status(410).entity(response).build());

		if (!cancelledSucceeded) {
			this.asyncResponse.cancel();
		}
	}

	public void stop() {
		if (this.hasAsyncResponse())
			this.cancelAsyncResponse();
		this.cancelInactivityTimeout();
		this.sc.stop();
		this.cancelAndClearAllHandleRequests();
	}

	private void cancelAndClearAllHandleRequests() {
		List<Integer> cancelledRequests = this.toBeProcessedHandleRequests.stream()
				.map(HandleRequest::getHandleRequestId).toList();
		LOG.warn("KB with id " + this.knowledgeBaseId
				+ " has stopped. The following handle requests will be cancelled: " + cancelledRequests);

		String cancelMessage = "KB with id " + this.knowledgeBaseId + " will no longer respond, because it stopped.";
		this.toBeProcessedHandleRequests.forEach(hr -> {
			hr.getFuture().completeExceptionally(new CancellationException(cancelMessage));
		});
		this.beingProcessedHandleRequests.forEach((id, hr) -> {
			hr.getFuture().completeExceptionally(new CancellationException(cancelMessage));
		});

		this.toBeProcessedHandleRequests.clear();
		this.beingProcessedHandleRequests.clear();
	}

	/**
	 * @return The lease renewal time of this smart connector, in seconds.
	 */
	public Integer getLeaseRenewalTime() {
		return this.leaseRenewalTime;
	}

	/**
	 * Renew the lease of this KB's smart connector with the configured renewal
	 * time.
	 */
	public void renewLease() {
		if (this.leaseRenewalTime != null && this.lease != null) {
			Date newExpiration = new Date();
			newExpiration.setTime(newExpiration.getTime() + 1000 * leaseRenewalTime);
			this.lease.setExpires(newExpiration);
		} else {
			LOG.warn("renewLease should not be called for knowledge bases that have no lease.");
		}
	}

	/**
	 * Returns true if the lease is expired, and false if it's still valid.
	 */
	public boolean leaseExpired() {
		if (this.lease != null) {
			return this.lease.getExpires().before(new Date());
		} else {
			return false;
		}
	}

	/**
	 * @return This KBs lease of the smart connector.
	 */
	public SmartConnectorLease getLease() {
		return this.lease;
	}

	public synchronized void resetInactivityTimeout() {
		cancelInactivityTimeout();
		setInactivityTimeout(true);
	}

	private synchronized void setInactivityTimeout(boolean overwrite) {
		if (!overwrite && this.inactivityTimer != null) {
			return;
		}

		this.inactivityTimer = new Timer();

		this.inactivityTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				// It could happen that the inactivity timer has been cancelled in the
				// mean time, and this task still happily runs in another thread, so we
				// check if the intactivity timer is still non-null here.
				if (inactivityTimer != null) {
					LOG.warn("Suspending KB {} because of inactivity.", knowledgeBaseId);
					suspend();
				}
			}
		}, RestKnowledgeBase.INACTIVITY_TIMEOUT_SECONDS * 1000);
		LOG.debug("(re)scheduled inactivity timer. KB {} will be suspended if it does not repoll within {} seconds.",
				this.knowledgeBaseId, RestKnowledgeBase.INACTIVITY_TIMEOUT_SECONDS);
	}

	private synchronized void cancelInactivityTimeout() {
		if (this.inactivityTimer != null) {
			LOG.debug("inactivity timer is being canceled for {}.", this.knowledgeBaseId);
			this.inactivityTimer.cancel();
			this.inactivityTimer = null;
		}
	}

	private void suspend() {
		this.stop();
		this.suspended = true;
	}

	public boolean isSuspended() {
		return this.suspended;
	}

	public int getReasonerLevel() {
		return this.sc.getReasonerLevel();
	}

	/**
	 * Converts the given domain knowledge into KE rules provide them to the Smart
	 * Connector.
	 * 
	 * @param someDomainKnowledge The domain knowledge to load into the Smart
	 *                            Connector in Apache Jena Rules syntax.
	 */
	public void setDomainKnowledge(String someDomainKnowledge) {
		Set<BaseRule> firstRules = JenaRules.convertJenaToKeRules(someDomainKnowledge);

		Set<Rule> theRules = new HashSet<>();
		for (BaseRule r : firstRules) {
			theRules.add((Rule) r);
		}

		this.sc.setDomainKnowledge(theRules);
	}
}
