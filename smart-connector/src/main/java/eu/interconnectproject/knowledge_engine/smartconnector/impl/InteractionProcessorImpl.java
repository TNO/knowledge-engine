package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.SparqlQueryConnection;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;

import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AskResult;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.CommunicativeAct;
import eu.interconnectproject.knowledge_engine.smartconnector.api.PostResult;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.RecipientSelector;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AnswerMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AskMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.PostMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.ReactMessage;

public class InteractionProcessorImpl implements InteractionProcessor {

	private final Logger LOG;

	private final OtherKnowledgeBaseStore otherKnowledgeBaseStore;
	private MessageRouter messageRouter;
	private final KnowledgeBaseStore myKnowledgeBaseStore;
	private final MetaKnowledgeBase metaKnowledgeBase;

	private final LoggerProvider loggerProvider;

	public InteractionProcessorImpl(LoggerProvider loggerProvider, OtherKnowledgeBaseStore otherKnowledgeBaseStore,
			KnowledgeBaseStore myKnowledgeBaseStore, MetaKnowledgeBase metaKnowledgeBase) {
		super();
		this.loggerProvider = loggerProvider;
		this.LOG = loggerProvider.getLogger(this.getClass());
		this.otherKnowledgeBaseStore = otherKnowledgeBaseStore;
		this.myKnowledgeBaseStore = myKnowledgeBaseStore;
		this.metaKnowledgeBase = metaKnowledgeBase;
	}

	@Override
	public CompletableFuture<AskResult> processAskFromKnowledgeBase(MyKnowledgeInteractionInfo anAKI,
			RecipientSelector aSelector, BindingSet aBindingSet) {
		assert anAKI != null : "the knowledge interaction should be non-null";
		assert aBindingSet != null : "the binding set should be non-null";

		var myKnowledgeInteraction = anAKI.getKnowledgeInteraction();

		// TODO use RecipientSelector. In the MVP we interpret the recipient selector as
		// a wildcard.

		// retrieve other knowledge bases
		Set<OtherKnowledgeBase> otherKnowledgeBases = this.otherKnowledgeBaseStore.getOtherKnowledgeBases();
		Set<KnowledgeInteractionInfo> otherKnowledgeInteractions = new HashSet<>();

		for (OtherKnowledgeBase otherKB : otherKnowledgeBases) {
			// Use the knowledge interactions from the other KB
			var knowledgeInteractions = otherKB.getKnowledgeInteractions().stream().filter((r) -> {
				// But filter on the communicative act. These have to match!
				return communicativeActMatcher(anAKI, r);
			});
			otherKnowledgeInteractions.addAll(knowledgeInteractions.collect(Collectors.toList()));
		}

		// create a new SingleInteractionProcessor to handle this ask.
		SingleInteractionProcessor processor = new SerialMatchingProcessor(this.loggerProvider,
				otherKnowledgeInteractions, this.messageRouter);

		// give the caller something to chew on while it waits. This method starts the
		// interaction process as far as it can until it is blocked because it waits for
		// outstanding message replies. Then it returns the future. Threads from the
		// MessageDispatcher will finish this process and the thread that handles the
		// last reply message will complete the future and notify the caller
		// KnowledgeBase.
		CompletableFuture<AskResult> future = processor.processAskInteraction(anAKI, aBindingSet);

		return future;
	}

	@Override
	public CompletableFuture<AnswerMessage> processAskFromMessageRouter(AskMessage anAskMsg) {
		URI answerKnowledgeInteractionId = anAskMsg.getToKnowledgeInteraction();

		try {
			KnowledgeInteractionInfo knowledgeInteractionById = this.myKnowledgeBaseStore
					.getKnowledgeInteractionById(answerKnowledgeInteractionId);

			AnswerKnowledgeInteraction answerKnowledgeInteraction;
			answerKnowledgeInteraction = (AnswerKnowledgeInteraction) knowledgeInteractionById
					.getKnowledgeInteraction();
			var future = new CompletableFuture<AnswerMessage>();
			{
				BindingSet bindings = null;
				if (knowledgeInteractionById.isMeta()) {
					// TODO: Ask MyMetaKnowledgeBase for the bindings.
				} else {
					var handler = this.myKnowledgeBaseStore.getAnswerHandler(answerKnowledgeInteractionId);
					// TODO This should happen in the single thread for the knowledge base
					bindings = handler.answer(answerKnowledgeInteraction, anAskMsg.getBindings());
				}

				AnswerMessage result = new AnswerMessage(anAskMsg.getToKnowledgeBase(), answerKnowledgeInteractionId,
						anAskMsg.getFromKnowledgeBase(), anAskMsg.getFromKnowledgeInteraction(),
						anAskMsg.getMessageId(), bindings);
				// TODO: Here I just complete the future in the same thread, but we should
				// figure out how to do it asynchronously.
				future.complete(result);

				return future;
			}
		} catch (Throwable t) {
			this.LOG.warn("Encountered an unresolvable KnowledgeInteraction ID '" + answerKnowledgeInteractionId
					+ "' that was expected to resolve to one of our own.", t);
			var future = new CompletableFuture<AnswerMessage>();
			future.completeExceptionally(t);
			return future;
		}
	}

	@Override
	public CompletableFuture<PostResult> processPostFromKnowledgeBase(MyKnowledgeInteractionInfo aPKI,
			RecipientSelector aSelector, BindingSet someArguments) {
		assert aPKI != null : "the knowledge interaction should be non-null";
		assert someArguments != null : "the binding set should be non-null";

		var myKnowledgeInteraction = aPKI.getKnowledgeInteraction();

		// TODO: Use RecipientSelector

		// retrieve other knowledge bases
		Set<OtherKnowledgeBase> otherKnowledgeBases = this.otherKnowledgeBaseStore.getOtherKnowledgeBases();
		Set<KnowledgeInteractionInfo> otherKnowledgeInteractions = new HashSet<>();

		for (OtherKnowledgeBase otherKB : otherKnowledgeBases) {
			// Use the knowledge interactions from the other KB
			var knowledgeInteractions = otherKB.getKnowledgeInteractions().stream().filter((r) -> {
				// But filter on the communicative act. These have to match!
				return communicativeActMatcher(aPKI, r);
			});
			otherKnowledgeInteractions.addAll(knowledgeInteractions.collect(Collectors.toList()));
		}

		// create a new SingleInteractionProcessor to handle this ask.
		SingleInteractionProcessor processor = new SerialMatchingProcessor(this.loggerProvider,
				otherKnowledgeInteractions, this.messageRouter);

		// give the caller something to chew on while it waits. This method starts the
		// interaction process as far as it can until it is blocked because it waits for
		// outstanding message replies. Then it returns the future. Threads from the
		// MessageDispatcher will finish this process and the thread that handles the
		// last reply message will complete the future and notify the caller
		// KnowledgeBase.
		CompletableFuture<PostResult> future = processor.processPostInteraction(aPKI, someArguments);

		return future;
	}

	@Override
	public CompletableFuture<ReactMessage> processPostFromMessageRouter(PostMessage aPostMsg) {
		URI reactKnowledgeInteractionId = aPostMsg.getToKnowledgeInteraction();
		try {
			KnowledgeInteractionInfo knowledgeInteractionById = this.myKnowledgeBaseStore
					.getKnowledgeInteractionById(reactKnowledgeInteractionId);
			ReactKnowledgeInteraction reactKnowledgeInteraction;
			reactKnowledgeInteraction = (ReactKnowledgeInteraction) knowledgeInteractionById.getKnowledgeInteraction();
			var future = new CompletableFuture<ReactMessage>();
			{
				BindingSet bindings = null;
				if (knowledgeInteractionById.isMeta()) {
					// TODO: Ask MyMetaKnowledgeBase for the bindings.
				} else {
					var handler = this.myKnowledgeBaseStore.getReactHandler(reactKnowledgeInteractionId);
					// TODO This should happen in the single thread for the knowledge base
					bindings = handler.react(reactKnowledgeInteraction, aPostMsg.getBindings());
				}

				ReactMessage result = new ReactMessage(aPostMsg.getToKnowledgeBase(), reactKnowledgeInteractionId,
						aPostMsg.getFromKnowledgeBase(), aPostMsg.getFromKnowledgeInteraction(),
						aPostMsg.getMessageId(), bindings);
				// TODO: Here I just complete the future in the same thread, but we should
				// figure out how to do it asynchronously.
				future.complete(result);
			}

			return future;
		} catch (Throwable t) {
			this.LOG.warn("Encountered an unresolvable KnowledgeInteraction ID '" + reactKnowledgeInteractionId
					+ "' that was expected to resolve to one of our own.", t);
			var future = new CompletableFuture<ReactMessage>();
			future.completeExceptionally(t);
			return future;
		}

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
		boolean doTheyMatch = false;

		Model m = ModelFactory.createDefaultModel();
		m.read(InteractionProcessorImpl.class.getResourceAsStream(Vocab.ONTOLOGY_RESOURCE_LOCATION), null, "turtle");

		assert !m.isEmpty();

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
		InfModel infModel = ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), m);

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
		
		return doTheyMatch;
	}

}
