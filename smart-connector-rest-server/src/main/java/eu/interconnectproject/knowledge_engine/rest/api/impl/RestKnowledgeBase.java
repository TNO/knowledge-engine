package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.rest.model.KnowledgeInteractionBase;
import eu.interconnectproject.knowledge_engine.rest.model.KnowledgeInteractionWithId;
import eu.interconnectproject.knowledge_engine.rest.model.SmartConnectorLease;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerHandler;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AskKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.Binding;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.CommunicativeAct;
import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.PostKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactHandler;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnectorProvider;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnectorSPI;

public class RestKnowledgeBase implements KnowledgeBase {
	private static final Logger LOG = LoggerFactory.getLogger(RestKnowledgeBase.class);

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
					KnowledgeInteractionType.ANSWER, bindings, future);

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
					KnowledgeInteractionType.REACT, bindings, future);

			toBeProcessedByKnowledgeBase(hr);
			return future;
		}

		public BindingSet react(ReactKnowledgeInteraction aRKI, BindingSet aBindingSet) {
			throw new IllegalArgumentException("Should not be called.");
		}
	};

	/**
	 * Runnable to run when smart connector is ready. This currently triggers the
	 * response to a successful /sc POST request.
	 */
	private final Runnable onReady;

	/**
	 * The lease that must be periodically renewed to keep using the smart connector.
	 * If this is `null`, the lease is permanent.
	 */
	private final SmartConnectorLease lease;

	/**
	 * The renewal time of the lease.
	 */
	private final Integer leaseRenewalTime;

	public RestKnowledgeBase(
		eu.interconnectproject.knowledge_engine.rest.model.SmartConnector scModel,
		final Runnable onReady
	) {
		this.knowledgeBaseId = scModel.getKnowledgeBaseId();
		this.knowledgeBaseName = scModel.getKnowledgeBaseName();
		this.knowledgeBaseDescription = scModel.getKnowledgeBaseDescription();
		this.knowledgeInteractions = new HashMap<>();
		this.toBeProcessedHandleRequests = new ArrayBlockingQueue<>(50);
		this.beingProcessedHandleRequests = Collections.synchronizedMap(new HashMap<Integer, HandleRequest>());
		this.handleRequestId = new AtomicInteger(0);
		this.onReady = onReady;
		this.leaseRenewalTime = scModel.getLeaseRenewalTime();

		if (this.leaseRenewalTime != null) {
			// Issue the initial lease.
			LOG.info("Creating REST Knowledge Base with lease that must be renewed every {} seconds.", this.leaseRenewalTime);

			Date expiration = new Date();
			expiration.setTime(expiration.getTime() + 1000 * this.leaseRenewalTime);

			this.lease = new SmartConnectorLease()
				.knowledgeBaseId(this.knowledgeBaseId)
				.expires(expiration);
		} else {
			this.lease = null;
		}

		if (smartConnectorProvider == null) {
			throw new IllegalStateException(
					"SmartConnectorProvider not initialized. Make sure there is a SmartConnectorProvider implementation registered on the classpath.");
		}
		this.sc = smartConnectorProvider.create(this);
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
		HandleRequest hr = this.beingProcessedHandleRequests.get(handleRequestId);
		BindingSet bs = this.listToBindingSet(responseBody.getBindingSet());

		// TODO: Can this be moved to somewhere internal so that it can also be
		// caught in the Java developer api?
		// See https://gitlab.inesctec.pt/interconnect/knowledge-engine/-/issues/148
		hr.validateBindings(bs);

		// Now that the validation is done, from the reactive side we are done, so
		// we can remove the HandleRequest from our list.
		this.beingProcessedHandleRequests.remove(handleRequestId);

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

		PrefixMapping prefixMapping;
		if (ki.getPrefixes() != null) {
			prefixMapping = new PrefixMappingMem();
			prefixMapping.setNsPrefixes(ki.getPrefixes());
		} else {
			prefixMapping = new PrefixMappingZero();
		}

		String type = ki.getKnowledgeInteractionType();
		URI kiId;
		if (type.equals("AskKnowledgeInteraction")) {

			eu.interconnectproject.knowledge_engine.rest.model.AskKnowledgeInteraction aki = (eu.interconnectproject.knowledge_engine.rest.model.AskKnowledgeInteraction) ki;

			if (aki.getGraphPattern() == null) {
				throw new IllegalArgumentException("graphPattern must be given for ASK knowledge interactions.");
			}
			var askKI = new AskKnowledgeInteraction(ca, new GraphPattern(prefixMapping, aki.getGraphPattern()));
			kiId = this.sc.register(askKI);
			this.knowledgeInteractions.put(kiId, askKI);
		} else if (type.equals("AnswerKnowledgeInteraction")) {

			eu.interconnectproject.knowledge_engine.rest.model.AnswerKnowledgeInteraction aki = (eu.interconnectproject.knowledge_engine.rest.model.AnswerKnowledgeInteraction) ki;

			if (aki.getGraphPattern() == null) {
				throw new IllegalArgumentException("graphPattern must be given for ANSWER knowledge interactions.");
			}
			var answerKI = new AnswerKnowledgeInteraction(ca, new GraphPattern(prefixMapping, aki.getGraphPattern()));

			kiId = this.sc.register(answerKI, this.answerHandler);

			this.knowledgeInteractions.put(kiId, answerKI);
		} else if (type.equals("PostKnowledgeInteraction")) {

			eu.interconnectproject.knowledge_engine.rest.model.PostKnowledgeInteraction pki = (eu.interconnectproject.knowledge_engine.rest.model.PostKnowledgeInteraction) ki;

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

			var postKI = new PostKnowledgeInteraction(ca, argGP, resGP);
			kiId = this.sc.register(postKI);

			this.knowledgeInteractions.put(kiId, postKI);
		} else if (type.equals("ReactKnowledgeInteraction")) {

			eu.interconnectproject.knowledge_engine.rest.model.ReactKnowledgeInteraction rki = (eu.interconnectproject.knowledge_engine.rest.model.ReactKnowledgeInteraction) ki;

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

	public CompletableFuture<eu.interconnectproject.knowledge_engine.smartconnector.api.AskResult> ask(String kiId,
			List<Map<String, String>> bindings) throws URISyntaxException, InterruptedException, ExecutionException {
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
		var askFuture = this.sc.ask((AskKnowledgeInteraction) ki, listToBindingSet(bindings));

		return askFuture;
	}

	public CompletableFuture<eu.interconnectproject.knowledge_engine.smartconnector.api.PostResult> post(String kiId,
			List<Map<String, String>> bindings) throws URISyntaxException, InterruptedException, ExecutionException {
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
		var postFuture = this.sc.post((PostKnowledgeInteraction) ki, listToBindingSet(bindings));

		return postFuture;
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
		// Do nothing. The REST API doesn't provide these signals (yet).
	}

	public void stop() {
		this.sc.stop();
		this.cancelAllHandleRequests();
	}

	private void cancelAllHandleRequests() {
		this.toBeProcessedHandleRequests.forEach(hr -> {
			hr.getFuture().cancel(false);
		});
		this.beingProcessedHandleRequests.forEach((id, hr) -> {
			hr.getFuture().cancel(false);
		});
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
			LOG.warn("renewLease should not be called for knowledge bases that have a permanent lease.");
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
}
