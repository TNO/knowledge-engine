package eu.knowledge.engine.smartconnector.impl;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.smartconnector.api.AnswerExchangeInfo;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskPlan;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.PostPlan;
import eu.knowledge.engine.smartconnector.api.ReactExchangeInfo;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.RecipientSelector;
import eu.knowledge.engine.smartconnector.api.Vocab;
import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.messaging.PostMessage;
import eu.knowledge.engine.smartconnector.messaging.ReactMessage;

public class InteractionProcessorImpl implements InteractionProcessor {

	private final Logger LOG;

	private final OtherKnowledgeBaseStore otherKnowledgeBaseStore;
	private MessageRouter messageRouter;
	private final KnowledgeBaseStore myKnowledgeBaseStore;

	private final Model ontology;
	private final Reasoner reasoner;

	/**
	 * A set of rules that represent the domain knowledge this smart connector
	 * should take into account while orchestrating data exchange. Only available if
	 * reasoning is enabled.
	 */
	private Set<Rule> additionalDomainKnowledge = new HashSet<>();

	private final LoggerProvider loggerProvider;

	/**
	 * Whether this interaction processor should use reasoning to orchestrate the
	 * data exchange.
	 */
	private boolean reasonerEnabled = true;

	public InteractionProcessorImpl(LoggerProvider loggerProvider, OtherKnowledgeBaseStore otherKnowledgeBaseStore,
			KnowledgeBaseStore myKnowledgeBaseStore) {
		super();
		this.loggerProvider = loggerProvider;
		this.LOG = loggerProvider.getLogger(this.getClass());

		this.otherKnowledgeBaseStore = otherKnowledgeBaseStore;
		this.myKnowledgeBaseStore = myKnowledgeBaseStore;

		ontology = ModelFactory.createDefaultModel();
		ontology.read(InteractionProcessorImpl.class.getResourceAsStream(Vocab.ONTOLOGY_RESOURCE_LOCATION), null,
				"turtle");
		assert !this.ontology.isEmpty();

		reasoner = ReasonerRegistry.getRDFSSimpleReasoner().bindSchema(this.ontology);
	}

	@Override
	public AskPlan planAskFromKnowledgeBase(MyKnowledgeInteractionInfo anAKI, RecipientSelector aSelector) {
		assert anAKI != null : "the knowledge interaction should be non-null";
		assert aSelector != null : "the selector should be non-null";

		var myKnowledgeInteraction = anAKI.getKnowledgeInteraction();

		// retrieve other knowledge bases
		Set<OtherKnowledgeBase> otherKnowledgeBases = this.otherKnowledgeBaseStore.getOtherKnowledgeBases();

		// use RecipientSelector to make a subset. We could do this in the easy way, but
		// in the future we hope that these RecipientSelectors can be merged with the
		// regular Knowledge Interaction graph pattern. Therefore we use a more
		// complicated algorithm to create a subset of the other knowledge bases.
		Set<OtherKnowledgeBase> filteredOtherKnowledgeBases = filterOtherKnowledgeBases(otherKnowledgeBases, aSelector);

		Set<KnowledgeInteractionInfo> otherKnowledgeInteractions = new HashSet<>();

		for (OtherKnowledgeBase otherKB : filteredOtherKnowledgeBases) {
			// Use the knowledge interactions from the other KB
			var knowledgeInteractions = otherKB.getKnowledgeInteractions().stream().filter((r) -> {
				// But filter on the communicative act. These have to match!
				return communicativeActMatcher(anAKI, r);
			});
			otherKnowledgeInteractions.addAll(knowledgeInteractions.collect(Collectors.toList()));
		}

		// create a new SingleInteractionProcessor to handle this ask.
		SingleInteractionProcessor processor;
		if (this.reasonerEnabled) {
			processor = new ReasonerProcessor(otherKnowledgeInteractions, messageRouter,
					this.additionalDomainKnowledge);
		} else {
			processor = new SerialMatchingProcessor(this.loggerProvider, otherKnowledgeInteractions,
					this.messageRouter);
		}

		// give the caller something to chew on while it waits. This method starts the
		// interaction process as far as it can until it is blocked because it waits for
		// outstanding message replies. Then it returns the future. Threads from the
		// MessageDispatcher will finish this process and the thread that handles the
		// last reply message will complete the future and notify the caller
		// KnowledgeBase.
		processor.planAskInteraction(anAKI);
		return new AskPlanImpl(processor);
	}

	private Set<OtherKnowledgeBase> filterOtherKnowledgeBases(Set<OtherKnowledgeBase> otherKnowledgeBases,
			RecipientSelector aSelector) {

		Set<OtherKnowledgeBase> filtered = new HashSet<>();

		// convert to RDF
		Model m = ModelFactory.createDefaultModel();
		for (OtherKnowledgeBase okb : otherKnowledgeBases) {
			m.add(okb.getRDF());
		}

		String queryString = createQuery(aSelector.getPattern(), aSelector.getBindingSet());
		LOG.debug("Query: {}", queryString);
		Query q = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(q, m);
		ResultSet rs = qe.execSelect();

		Set<URI> filteredKBs = new HashSet<>();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			filteredKBs.add(URI.create(qs.get("kb").toString()));
		}

		filtered = otherKnowledgeBases.stream().filter((kb) -> {
			return filteredKBs.contains(kb.getId());
		}).collect(Collectors.toSet());

		qe.close();

		return filtered;
	}

	private String createQuery(GraphPattern gp, BindingSet bs) {
		// then query to retrieve the selected KBs.
		String queryString = "SELECT * WHERE { " + gp.getPattern() + " } VALUES (?kb) { ";

		if (!bs.isEmpty()) {
			for (Binding b : bs) {
				assert (b.containsKey("kb"));
				String kbId = b.get("kb");
				queryString += "(" + kbId + "),";
			}
		} else {
			queryString += "(UNDEF),";
		}

		queryString = queryString.substring(0, queryString.length() - 1);
		queryString += " }";
		return queryString;
	}

	@Override
	public CompletableFuture<AnswerMessage> processAskFromMessageRouter(AskMessage anAskMsg) {
		URI answerKnowledgeInteractionId = anAskMsg.getToKnowledgeInteraction();

		KnowledgeInteractionInfo knowledgeInteractionById = this.myKnowledgeBaseStore
				.getKnowledgeInteractionById(answerKnowledgeInteractionId);

		AnswerKnowledgeInteraction answerKnowledgeInteraction;
		answerKnowledgeInteraction = (AnswerKnowledgeInteraction) knowledgeInteractionById.getKnowledgeInteraction();

		CompletableFuture<BindingSet> future;

		var handler = this.myKnowledgeBaseStore.getAnswerHandler(answerKnowledgeInteractionId);
		// TODO This should happen in the single thread for the knowledge base

		LOG.info("Contacting my KB to answer KI <{}>", answerKnowledgeInteractionId);

		var aei = new AnswerExchangeInfo(anAskMsg.getBindings(), anAskMsg.getFromKnowledgeBase(),
				anAskMsg.getFromKnowledgeInteraction());

		future = handler.answerAsync(answerKnowledgeInteraction, aei);

		return future.thenApply((b) -> {
			LOG.debug("Received ANSWER from KB for KI <{}>: {}", answerKnowledgeInteractionId, b);
			return new AnswerMessage(anAskMsg.getToKnowledgeBase(), answerKnowledgeInteractionId,
					anAskMsg.getFromKnowledgeBase(), anAskMsg.getFromKnowledgeInteraction(), anAskMsg.getMessageId(),
					b);
		}).exceptionally((e) -> {
			LOG.error("An error occurred while answering a msg: {}", e);
			LOG.debug("The error occured while answering this message: {}", anAskMsg);
			return new AnswerMessage(anAskMsg.getToKnowledgeBase(), answerKnowledgeInteractionId,
					anAskMsg.getFromKnowledgeBase(), anAskMsg.getFromKnowledgeInteraction(), anAskMsg.getMessageId(),
					e.getMessage());
		});
	}

	@Override
	public PostPlan planPostFromKnowledgeBase(MyKnowledgeInteractionInfo aPKI, RecipientSelector aSelector) {
		assert aPKI != null : "the knowledge interaction should be non-null";
		assert aSelector != null : "the selector should be non-null";

		var myKnowledgeInteraction = aPKI.getKnowledgeInteraction();

		// retrieve other knowledge bases
		Set<OtherKnowledgeBase> otherKnowledgeBases = this.otherKnowledgeBaseStore.getOtherKnowledgeBases();

		// use RecipientSelector to make a subset. We could do this in the easy way, but
		// in the future we hope that these RecipientSelectors can be merged with the
		// regular Knowledge Interaction graph pattern. Therefore we use a more
		// complicated algorithm to create a subset of the other knowledge bases.
		Set<OtherKnowledgeBase> filteredOtherKnowledgeBases = filterOtherKnowledgeBases(otherKnowledgeBases, aSelector);

		Set<KnowledgeInteractionInfo> otherKnowledgeInteractions = new HashSet<>();

		for (OtherKnowledgeBase otherKB : filteredOtherKnowledgeBases) {
			// Use the knowledge interactions from the other KB
			var knowledgeInteractions = otherKB.getKnowledgeInteractions().stream().filter((r) -> {
				// But filter on the communicative act. These have to match!
				return communicativeActMatcher(aPKI, r);
			});
			otherKnowledgeInteractions.addAll(knowledgeInteractions.collect(Collectors.toList()));
		}

		// create a new SingleInteractionProcessor to handle this ask.
		SingleInteractionProcessor processor;
		if (this.reasonerEnabled) {
			processor = new ReasonerProcessor(otherKnowledgeInteractions, this.messageRouter,
					this.additionalDomainKnowledge);
		} else {
			processor = new SerialMatchingProcessor(this.loggerProvider, otherKnowledgeInteractions,
					this.messageRouter);
		}
		// give the caller something to chew on while it waits. This method starts the
		// interaction process as far as it can until it is blocked because it waits for
		// outstanding message replies. Then it returns the future. Threads from the
		// MessageDispatcher will finish this process and the thread that handles the
		// last reply message will complete the future and notify the caller
		// KnowledgeBase.

		processor.planPostInteraction(aPKI);
		return new PostPlanImpl(processor);
	}

	@Override
	public CompletableFuture<ReactMessage> processPostFromMessageRouter(PostMessage aPostMsg) {
		URI reactKnowledgeInteractionId = aPostMsg.getToKnowledgeInteraction();
		KnowledgeInteractionInfo knowledgeInteractionById = this.myKnowledgeBaseStore
				.getKnowledgeInteractionById(reactKnowledgeInteractionId);
		ReactKnowledgeInteraction reactKnowledgeInteraction;
		reactKnowledgeInteraction = (ReactKnowledgeInteraction) knowledgeInteractionById.getKnowledgeInteraction();

		CompletableFuture<BindingSet> future;
		var handler = this.myKnowledgeBaseStore.getReactHandler(reactKnowledgeInteractionId);

		var rei = new ReactExchangeInfo(aPostMsg.getArgument(), aPostMsg.getFromKnowledgeBase(),
				aPostMsg.getFromKnowledgeInteraction());

		// TODO This should happen in the single thread for the knowledge base
		LOG.info("Contacting my KB to react to KI <{}>", reactKnowledgeInteractionId);

		future = handler.reactAsync(reactKnowledgeInteraction, rei);

		return future.thenApply(b -> {
			LOG.debug("Received REACT from KB for KI <{}>: {}", reactKnowledgeInteraction, b);
			return new ReactMessage(aPostMsg.getToKnowledgeBase(), reactKnowledgeInteractionId,
					aPostMsg.getFromKnowledgeBase(), aPostMsg.getFromKnowledgeInteraction(), aPostMsg.getMessageId(),
					b);
		}).exceptionally((e) -> {
			LOG.error("An error occurred while answering a msg: {}", e);
			LOG.debug("The error occured while answering this message: {}", aPostMsg);
			return new ReactMessage(aPostMsg.getToKnowledgeBase(), reactKnowledgeInteractionId,
					aPostMsg.getFromKnowledgeBase(), aPostMsg.getFromKnowledgeInteraction(), aPostMsg.getMessageId(),
					e.getMessage());
		});
	}

	@Override
	public void setMessageRouter(MessageRouter messageRouter) {
		this.messageRouter = messageRouter;
	}

	@Override
	public void unsetMessageRouter() {
		this.messageRouter = null;
	}

	/**
	 * We need the KnowledgeInteractionInfo objects, because they contain ID
	 * information that we use to construct unique identifiers for the temporary
	 * model.
	 * 
	 * @param myKI
	 * @param otherKI
	 * @return {@code true} if the communicative acts of the given
	 *         KnowledgeInteractions match, {@code false} otherwise.
	 */
	private boolean communicativeActMatcher(MyKnowledgeInteractionInfo myKI, KnowledgeInteractionInfo otherKI) {

		Instant start = Instant.now();

		boolean doTheyMatch = false;

		Model m = ModelFactory.createDefaultModel();

		// first add my knowledge interaction communicative act
		CommunicativeAct myAct = myKI.getKnowledgeInteraction().getAct();
		Resource myActResource = ResourceFactory.createResource(myKI.id + "/act");

		m.add(myActResource, RDF.type, Vocab.COMMUNICATIVE_ACT);

		Resource myRequirementPurpose = ResourceFactory.createResource(myActResource + "/requirement");
		Resource mySatisfactionPurpose = ResourceFactory.createResource(myActResource + "/satisfaction");

		m.add(myActResource, Vocab.HAS_REQ, myRequirementPurpose);
		m.add(mySatisfactionPurpose, Vocab.HAS_SAT, mySatisfactionPurpose);

		// give the purposes the correct types
		for (Resource r : myAct.getRequirementPurposes()) {
			m.add(myRequirementPurpose, RDF.type, r);
		}
		for (Resource r : myAct.getSatisfactionPurposes()) {
			m.add(mySatisfactionPurpose, RDF.type, r);
		}

		// then add the other knowledge interaction communicative act
		CommunicativeAct otherAct = otherKI.getKnowledgeInteraction().getAct();
		Resource otherActResource = ResourceFactory.createResource(otherKI.id + "/act");

		m.add(otherActResource, RDF.type, Vocab.COMMUNICATIVE_ACT);

		Resource otherRequirementPurpose = ResourceFactory.createResource(otherActResource + "/requirement");
		Resource otherSatisfactionPurpose = ResourceFactory.createResource(otherActResource + "/satisfaction");

		m.add(otherActResource, Vocab.HAS_REQ, otherRequirementPurpose);
		m.add(otherSatisfactionPurpose, Vocab.HAS_SAT, otherSatisfactionPurpose);

		// give the purposes the correct types
		for (Resource r : otherAct.getRequirementPurposes()) {
			m.add(otherRequirementPurpose, RDF.type, r);
		}
		for (Resource r : otherAct.getSatisfactionPurposes()) {
			m.add(otherSatisfactionPurpose, RDF.type, r);
		}

		// then apply the reasoner
		InfModel infModel = ModelFactory.createInfModel(this.reasoner.bind(m.getGraph()));

		// query the model from both my and the other perspective (both should match)
		// TODO can we do this with a single query execution? This might be a lot
		// faster. either we set multiple iris for the same params. Or we change the ASK
		// to include myReq/otherReq and mySat/otherSat vars.
		ParameterizedSparqlString queryString = new ParameterizedSparqlString(
				"ASK WHERE { ?req <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?someClass . FILTER NOT EXISTS {?sat <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?someClass .}}");

		// my perspective
		queryString.setIri("req", myRequirementPurpose.toString());
		queryString.setIri("sat", otherSatisfactionPurpose.toString());
		QueryExecution myQe = QueryExecutionFactory.create(queryString.asQuery(), infModel);

		queryString.clearParams();

		// other perspective
		queryString.setIri("req", otherRequirementPurpose.toString());
		queryString.setIri("sat", mySatisfactionPurpose.toString());
		QueryExecution otherQe = QueryExecutionFactory.create(queryString.asQuery(), infModel);

		doTheyMatch = !myQe.execAsk() && !otherQe.execAsk();

		myQe.close();
		otherQe.close();

		LOG.trace("Communicative Act time ({}): {}ms", doTheyMatch, Duration.between(start, Instant.now()).toMillis());

		return doTheyMatch;
	}

	@Override
	public void setDomainKnowledge(Set<Rule> someRules) {
		this.additionalDomainKnowledge = someRules;
	}

	@Override
	public void setReasonerEnabled(boolean aReasonerEnabled) {
		this.reasonerEnabled = aReasonerEnabled;
	}

	@Override
	public boolean isReasonerEnabled() {
		return this.reasonerEnabled;
	}

}
