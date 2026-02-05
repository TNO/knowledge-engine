package eu.knowledge.engine.smartconnector.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
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
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.util.JenaRules;
import eu.knowledge.engine.smartconnector.api.AnswerExchangeInfo;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskPlan;
import eu.knowledge.engine.smartconnector.api.BindingValidator;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.PostPlan;
import eu.knowledge.engine.smartconnector.api.ReactExchangeInfo;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.RecipientSelector;
import eu.knowledge.engine.smartconnector.api.SmartConnectorConfig;
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
	 * A set of rules (or facts) that represent the domain knowledge this smart
	 * connector should take into account while orchestrating data exchange.
	 */
	private Set<Rule> additionalDomainKnowledge = new HashSet<>();

	private final LoggerProvider loggerProvider;

	/**
	 * The default reasoner level of this smart connector is retrieved from the
	 * configuration.
	 */
	private int reasonerLevel = ConfigProvider.getConfig().getValue(SmartConnectorConfig.CONF_KEY_KE_REASONER_LEVEL,
			Integer.class);

	private static final Query query = QueryFactory.create(
			"ASK WHERE { ?req <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?someClass . FILTER NOT EXISTS {?sat <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?someClass .} VALUES (?req ?sat) {} }");

	public InteractionProcessorImpl(LoggerProvider loggerProvider, OtherKnowledgeBaseStore otherKnowledgeBaseStore,
			KnowledgeBaseStore myKnowledgeBaseStore) {
		super();
		this.loggerProvider = loggerProvider;
		this.LOG = this.loggerProvider.getLogger(this.getClass());

		this.otherKnowledgeBaseStore = otherKnowledgeBaseStore;
		this.myKnowledgeBaseStore = myKnowledgeBaseStore;

		// load default domain knowledge if available
		Optional<String> pathToDomainKnowledge = ConfigProvider.getConfig()
				.getOptionalValue(SmartConnectorConfig.CONF_KEY_KE_DOMAIN_KNOWLEDGE_PATH, String.class);
		if (pathToDomainKnowledge.isPresent()) {

			String pathString = pathToDomainKnowledge.get();
			readAdditionalDomainKnowledge(pathString);
		}

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
				return anAKI.getKnowledgeInteraction().includeMetaKIs() ? true : !r.isMeta();
			});

			otherKnowledgeInteractions.addAll(knowledgeInteractions.collect(Collectors.toList()));
		}

		// But filter on the communicative act. These have to match!
		filterWithCommunicativeActMatcher(anAKI, otherKnowledgeInteractions);

		// create a new ReasonerProcessor to handle this ask.
		ReasonerProcessor processor = new ReasonerProcessor(otherKnowledgeInteractions, messageRouter,
				this.additionalDomainKnowledge);
		processor.setMatchStrategy(SmartConnectorConfig.toMatchStrategy(this.reasonerLevel));

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

		String queryString = createQuery(aSelector.getPattern(),
				Util.translateFromApiBindingSet(aSelector.getBindingSet()));
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

		PrefixMapping prefixMapping = new PrefixMappingZero();
		if (!bs.isEmpty()) {
			var kbVar = Var.alloc("kb");
			for (Binding b : bs) {
				assert (b.containsKey(kbVar));
				Node kbId = b.get(kbVar);
				queryString += "(" + FmtUtils.stringForNode(kbId, prefixMapping) + ")";
			}
		} else {
			queryString += "(UNDEF)";
		}

		queryString += " }";
		return queryString;
	}

	@Override
	public CompletableFuture<AnswerMessage> processAskFromMessageRouter(AskMessage anAskMsg) {
		URI answerKnowledgeInteractionId = anAskMsg.getToKnowledgeInteraction();

		KnowledgeInteractionInfo knowledgeInteractionById = this.myKnowledgeBaseStore
				.getKnowledgeInteractionById(answerKnowledgeInteractionId);

		if (knowledgeInteractionById == null) {
			AnswerMessage m = new AnswerMessage(anAskMsg.getToKnowledgeBase(), anAskMsg.getToKnowledgeInteraction(),
					anAskMsg.getFromKnowledgeBase(), anAskMsg.getFromKnowledgeInteraction(), anAskMsg.getMessageId(),
					"Received AskMessage wth unknown ToKnowledgeInteractionId");
			LOG.debug("Received AskMessage with unknown ToKnowledgeInteractionId: "
					+ anAskMsg.getToKnowledgeInteraction().toString());
			CompletableFuture<AnswerMessage> f = new CompletableFuture<>();
			f.complete(m);
			return f;
		}

		AnswerKnowledgeInteraction answerKnowledgeInteraction;
		answerKnowledgeInteraction = (AnswerKnowledgeInteraction) knowledgeInteractionById.getKnowledgeInteraction();

		CompletableFuture<eu.knowledge.engine.smartconnector.api.BindingSet> future;

		var handler = this.myKnowledgeBaseStore.getAnswerHandler(answerKnowledgeInteractionId);
		// TODO This should happen in the single thread for the knowledge base

		if (answerKnowledgeInteraction.isMeta()) {
			LOG.debug("Contacting my KB to answer KI <{}>", answerKnowledgeInteractionId);
		} else {
			LOG.info("Contacting my KB to answer KI <{}>", answerKnowledgeInteractionId);
		}

		var aei = new AnswerExchangeInfo(Util.translateToApiBindingSet(anAskMsg.getBindings()),
				anAskMsg.getFromKnowledgeBase(), anAskMsg.getFromKnowledgeInteraction());

		future = handler.answerAsync(answerKnowledgeInteraction, aei);

		return future.handle((b, e) -> {
			if (b != null && e == null) {
				LOG.debug("Received ANSWER from KB for KI <{}>: {}", answerKnowledgeInteractionId, b);
				BindingSet translatedB = Util.translateFromApiBindingSet(b);

				if (this.shouldValidateInputOutputBindings()) {
					var validator = new BindingValidator();
					validator.validateCompleteBindings(answerKnowledgeInteraction.getPattern(), translatedB);
					validator.validateIncomingOutgoingAnswer(answerKnowledgeInteraction.getPattern(),
							anAskMsg.getBindings(), translatedB);
				}
				return new AnswerMessage(anAskMsg.getToKnowledgeBase(), answerKnowledgeInteractionId,
						anAskMsg.getFromKnowledgeBase(), anAskMsg.getFromKnowledgeInteraction(),
						anAskMsg.getMessageId(), translatedB);
			} else {
				String errorMessage;
				if (e == null)
					errorMessage = "An error occurred while answering (because no BindingSet is available), but no exception was thrown.";
				else
					errorMessage = e.getMessage();

				return new AnswerMessage(anAskMsg.getToKnowledgeBase(), answerKnowledgeInteractionId,
						anAskMsg.getFromKnowledgeBase(), anAskMsg.getFromKnowledgeInteraction(),
						anAskMsg.getMessageId(), errorMessage);
			}
		}).exceptionally((e) -> {
			LOG.error("An error occurred while answering a msg: ", e);
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
				return aPKI.getKnowledgeInteraction().includeMetaKIs() ? true : !r.isMeta();
			});
			otherKnowledgeInteractions.addAll(knowledgeInteractions.collect(Collectors.toList()));
		}

		// But filter on the communicative act. These have to match!
		filterWithCommunicativeActMatcher(aPKI, otherKnowledgeInteractions);

		// create a new ReasonerProcessor to handle this ask.
		ReasonerProcessor processor = new ReasonerProcessor(otherKnowledgeInteractions, this.messageRouter,
				this.additionalDomainKnowledge);
		processor.setMatchStrategy(SmartConnectorConfig.toMatchStrategy(this.reasonerLevel));

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

		if (knowledgeInteractionById == null) {
			ReactMessage m = new ReactMessage(aPostMsg.getToKnowledgeBase(), aPostMsg.getToKnowledgeInteraction(),
					aPostMsg.getFromKnowledgeBase(), aPostMsg.getFromKnowledgeInteraction(), aPostMsg.getMessageId(),
					"Received PostMessage with unknown ToKnowledgeInteractionId");
			LOG.debug("Received PostMessage with unknown ToKnowledgeInteractionId: "
					+ aPostMsg.getToKnowledgeInteraction().toString());
			CompletableFuture<ReactMessage> f = new CompletableFuture<>();
			f.complete(m);
			return f;
		}

		ReactKnowledgeInteraction reactKnowledgeInteraction;
		reactKnowledgeInteraction = (ReactKnowledgeInteraction) knowledgeInteractionById.getKnowledgeInteraction();

		CompletableFuture<eu.knowledge.engine.smartconnector.api.BindingSet> future;
		var handler = this.myKnowledgeBaseStore.getReactHandler(reactKnowledgeInteractionId);

		var rei = new ReactExchangeInfo(Util.translateToApiBindingSet(aPostMsg.getArgument()),
				aPostMsg.getFromKnowledgeBase(), aPostMsg.getFromKnowledgeInteraction());

		// TODO This should happen in the single thread for the knowledge base
		if (reactKnowledgeInteraction.isMeta()) {
			LOG.debug("Contacting my KB to react to KI <{}>", reactKnowledgeInteractionId);
		} else {
			LOG.info("Contacting my KB to react to KI <{}>", reactKnowledgeInteractionId);
		}

		future = handler.reactAsync(reactKnowledgeInteraction, rei);

		return future.handle((b, e) -> {
			if (b != null && e == null) {
				LOG.debug("Received REACT from KB for KI <{}>: {}", reactKnowledgeInteraction, b);
				BindingSet translatedB = Util.translateFromApiBindingSet(b);

				if (this.shouldValidateInputOutputBindings()) {
					var validator = new BindingValidator();
					validator.validateCompleteBindings(reactKnowledgeInteraction.getResult(), translatedB);
					validator.validateIncomingOutgoingReact(reactKnowledgeInteraction.getArgument(),
							reactKnowledgeInteraction.getResult(), aPostMsg.getArgument(), translatedB);
				}
				return new ReactMessage(aPostMsg.getToKnowledgeBase(), reactKnowledgeInteractionId,
						aPostMsg.getFromKnowledgeBase(), aPostMsg.getFromKnowledgeInteraction(),
						aPostMsg.getMessageId(), translatedB);
			} else {
				String errorMessage;
				if (e == null)
					errorMessage = "An error occurred while reacting (because no BindingSet is available), but no exception was thrown.";
				else
					errorMessage = e.getMessage();

				return new ReactMessage(aPostMsg.getToKnowledgeBase(), reactKnowledgeInteractionId,
						aPostMsg.getFromKnowledgeBase(), aPostMsg.getFromKnowledgeInteraction(),
						aPostMsg.getMessageId(), errorMessage);

			}
		}).exceptionally((e) -> {
			LOG.error("An error occurred while reacting to a message:", e);
			LOG.debug("The error occured while reacting to this message: {}", aPostMsg);
			return new ReactMessage(aPostMsg.getToKnowledgeBase(), reactKnowledgeInteractionId,
					aPostMsg.getFromKnowledgeBase(), aPostMsg.getFromKnowledgeInteraction(), aPostMsg.getMessageId(),
					e.getMessage());
		});
	}

	private boolean shouldValidateInputOutputBindings() {
		return ConfigProvider.getConfig().getValue(
				SmartConnectorConfig.CONF_KEY_VALIDATE_OUTGOING_BINDINGS_WRT_INCOMING_BINDINGS, Boolean.class);
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
	 * @param otherKIs The collection that will be modified by removing all
	 *                 non-matching KIs.
	 */
	private void filterWithCommunicativeActMatcher(MyKnowledgeInteractionInfo myKI,
			Set<KnowledgeInteractionInfo> otherKIs) {

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
		for (KnowledgeInteractionInfo otherKI : otherKIs) {
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
		}

		// then apply the reasoner
		InfModel infModel = ModelFactory.createInfModel(this.reasoner.bind(m.getGraph()));

		// query the model from both my and the other perspective (both should match)
		// TODO can we do this with a single query execution? This might be a lot
		// faster. either we set multiple iris for the same params. Or we change the ASK
		// to include myReq/otherReq and mySat/otherSat vars.

		// my and other perspective
		var iter = otherKIs.iterator();
		while (iter.hasNext()) {
			KnowledgeInteractionInfo otherKI = iter.next();
			Resource otherActResource = ResourceFactory.createResource(otherKI.id + "/act");
			Resource otherRequirementPurpose = ResourceFactory.createResource(otherActResource + "/requirement");
			Resource otherSatisfactionPurpose = ResourceFactory.createResource(otherActResource + "/satisfaction");

			Var reqVar = Var.alloc("req");
			Var satVar = Var.alloc("sat");
			org.apache.jena.sparql.engine.binding.Binding theFirstBinding = BindingFactory.binding(reqVar,
					NodeFactory.createURI(myRequirementPurpose.toString()), satVar,
					NodeFactory.createURI(otherSatisfactionPurpose.toString()));

			org.apache.jena.sparql.engine.binding.Binding theSecondBinding = BindingFactory.binding(reqVar,
					NodeFactory.createURI(otherRequirementPurpose.toString()), satVar,
					NodeFactory.createURI(mySatisfactionPurpose.toString()));

			Query q = (Query) query.clone();
			ElementData de = ((ElementData) ((ElementGroup) q.getQueryPattern()).getLast());

			List<org.apache.jena.sparql.engine.binding.Binding> data = de.getRows();
			data.add(theFirstBinding);
			data.add(theSecondBinding);

			QueryExecution myQe = QueryExecutionFactory.create(q, infModel);
			boolean execAskMy = myQe.execAsk();
			myQe.close();

			doTheyMatch = !execAskMy;

			if (!doTheyMatch) {
				iter.remove();
			}
		}
		LOG.trace("Communicative Act time ({}): {}ms", doTheyMatch, Duration.between(start, Instant.now()).toMillis());
	}

	@Override
	public void setDomainKnowledge(Set<Rule> someRules) {
		this.additionalDomainKnowledge = someRules;
	}

	@Override
	public void setReasonerLevel(int aReasonerLevel) {
		this.reasonerLevel = aReasonerLevel;
	}

	@Override
	public int getReasonerLevel() {
		return this.reasonerLevel;
	}

	private void readAdditionalDomainKnowledge(String pathString) {
		Path p = FileSystems.getDefault().getPath(pathString);

		LOG.debug("Reading additional domain knowledge from path: " + p.toAbsolutePath());

		try (BufferedReader r = Files.newBufferedReader(p.toAbsolutePath(), StandardCharsets.UTF_8)) {

			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				sb.append(line).append("\n");
			}

			Set<BaseRule> baseRules = JenaRules.convertJenaToKeRules(sb.toString());
			Set<Rule> rules = new HashSet<>();
			for (BaseRule br : baseRules)
				rules.add((Rule) br);
			this.setDomainKnowledge(rules);

		} catch (IOException e) {
			LOG.warn("Reading the configured domain knowledge via config property '{}' from path '{}' should succeed.",
					SmartConnectorConfig.CONF_KEY_KE_DOMAIN_KNOWLEDGE_PATH, p.toAbsolutePath(), e);
		}
	}

}
