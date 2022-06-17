package eu.knowledge.engine.smartconnector.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;

import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostResult;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.RecipientSelector;
import eu.knowledge.engine.smartconnector.api.Vocab;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;

public class MetaKnowledgeBaseImpl implements MetaKnowledgeBase, KnowledgeBaseStoreListener {

	private static final long POST_REMOVED_TIMEOUT_MILLIS_PER_OTHERKB = 500;

	private final Logger LOG;

	private final URI myKnowledgeBaseId;
	private final MessageRouter messageRouter;
	private final GraphPattern metaGraphPattern;
	private final KnowledgeBaseStore knowledgeBaseStore;
	private OtherKnowledgeBaseStore otherKnowledgeBaseStore;
	private InteractionProcessor interactionProcessor;

	private AnswerKnowledgeInteraction metaAnswerKI;
	private AskKnowledgeInteraction metaAskKI;
	private PostKnowledgeInteraction metaPostNewKI;
	private PostKnowledgeInteraction metaPostRemovedKI;
	private PostKnowledgeInteraction metaPostChangedKI;
	private ReactKnowledgeInteraction metaReactNewKI;
	private ReactKnowledgeInteraction metaReactChangedKI;
	private ReactKnowledgeInteraction metaReactRemovedKI;

	public MetaKnowledgeBaseImpl(LoggerProvider loggerProvider, MessageRouter aMessageRouter,
			KnowledgeBaseStore aKnowledgeBaseStore) {
		this.LOG = loggerProvider.getLogger(this.getClass());

		this.myKnowledgeBaseId = aKnowledgeBaseStore.getKnowledgeBaseId();
		this.messageRouter = aMessageRouter;
		this.knowledgeBaseStore = aKnowledgeBaseStore;
		this.knowledgeBaseStore.addListener(this);

		var prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		this.metaGraphPattern = new GraphPattern(prefixes, "?kb rdf:type kb:KnowledgeBase .", "?kb kb:hasName ?name .",
				"?kb kb:hasDescription ?description .", "?kb kb:hasKnowledgeInteraction ?ki .",
				"?ki rdf:type ?kiType .", "?ki kb:isMeta ?isMeta .", "?ki kb:hasCommunicativeAct ?act .",
				"?act rdf:type kb:CommunicativeAct .", "?act kb:hasRequirement ?req .",
				"?act kb:hasSatisfaction ?sat .", "?req rdf:type ?reqType .", "?sat rdf:type ?satType .",
				"?ki kb:hasGraphPattern ?gp .", "?gp rdf:type ?patternType .",
				"?gp kb:hasPattern ?pattern .");

		this.metaAnswerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), this.metaGraphPattern, true, true);
		this.knowledgeBaseStore.register(this.metaAnswerKI, (anAKI, anAnswerExchangeInfo) -> this.fillMetaBindings(anAnswerExchangeInfo.getIncomingBindings()),
				true);

		this.metaAskKI = new AskKnowledgeInteraction(new CommunicativeAct(), this.metaGraphPattern, true, true);
		this.knowledgeBaseStore.register(this.metaAskKI, true);

		this.metaPostNewKI = new PostKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.NEW_KNOWLEDGE_PURPOSE))),
				this.metaGraphPattern, null, true, true);
		this.knowledgeBaseStore.register(this.metaPostNewKI, true);

		this.metaPostChangedKI = new PostKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.CHANGED_KNOWLEDGE_PURPOSE))),
				this.metaGraphPattern, null, true, true);
		this.knowledgeBaseStore.register(this.metaPostChangedKI, true);

		this.metaPostRemovedKI = new PostKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.REMOVED_KNOWLEDGE_PURPOSE))),
				this.metaGraphPattern, null, true, true);
		this.knowledgeBaseStore.register(this.metaPostRemovedKI, true);

		this.metaReactNewKI = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.NEW_KNOWLEDGE_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE))),
				this.metaGraphPattern, null, true, true);
		this.knowledgeBaseStore.register(this.metaReactNewKI, (aRKI, aReactExchangeInfo) -> {
			var newKb = this.constructOtherKnowledgeBaseFromBindingSet(aReactExchangeInfo.getArgumentBindings(), aReactExchangeInfo.getPostingKnowledgeBaseId());
			this.otherKnowledgeBaseStore.addKnowledgeBase(newKb);
			return new BindingSet();
		}, true);

		this.metaReactChangedKI = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.CHANGED_KNOWLEDGE_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE))),
				this.metaGraphPattern, null, true, true);
		this.knowledgeBaseStore.register(this.metaReactChangedKI, (aRKI, aReactExchangeInfo) -> {
			var changedKb = this.constructOtherKnowledgeBaseFromBindingSet(aReactExchangeInfo.getArgumentBindings(), aReactExchangeInfo.getPostingKnowledgeBaseId());
			this.otherKnowledgeBaseStore.updateKnowledgeBase(changedKb);
			return new BindingSet();
		}, true);

		this.metaReactRemovedKI = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.REMOVED_KNOWLEDGE_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE))),
				this.metaGraphPattern, null, true, true);
		this.knowledgeBaseStore.register(this.metaReactRemovedKI, (aRKI, aReactExchangeInfo) -> {
			var removedKb = this.constructOtherKnowledgeBaseFromBindingSet(aReactExchangeInfo.getArgumentBindings(), aReactExchangeInfo.getPostingKnowledgeBaseId());
			this.otherKnowledgeBaseStore.removeKnowledgeBase(removedKb);
			return new BindingSet();
		}, true);
	}

	/**
	 * Generate a binding set that (together with the meta graph pattern)
	 * represents this knowledge base and its knowledge interactions.
	 *
	 * @param incoming If given, this method will make sure to return a binding
	 * set that only contains bindings that 'fit on' a binding in the given
	 * binding set by removing bindings that do not 'fit'. If null, no such
	 * operation is performed.
	 *
	 * @return a binding set (or part thereof, if {@code incoming} is given) that
	 * (together with the meta graph pattern) represents this knowledge base and
	 * its knowledge interactions.
	 */
	private BindingSet fillMetaBindings(BindingSet incoming) {

		// first create a RDF version of this KnowledgeBase
		Model m = ModelFactory.createDefaultModel();

		Resource kb = m.createResource(this.myKnowledgeBaseId.toString());
		m.add(kb, RDF.type, Vocab.KNOWLEDGE_BASE);
		m.add(kb, Vocab.HAS_NAME, m.createLiteral(this.knowledgeBaseStore.getKnowledgeBaseName()));
		m.add(kb, Vocab.HAS_DESCR, m.createLiteral(this.knowledgeBaseStore.getKnowledgeBaseDescription()));

		Set<MyKnowledgeInteractionInfo> myKIs = this.knowledgeBaseStore.getKnowledgeInteractions();

		for (KnowledgeInteractionInfo myKI : myKIs) {
			Resource ki = m.createResource(myKI.getId().toString());
			m.add(kb, Vocab.HAS_KI, ki);
			m.add(ki, Vocab.IS_META, ResourceFactory.createTypedLiteral(myKI.isMeta()));
			Resource act = m.createResource(myKI.getId().toString() + "/act");
			m.add(ki, Vocab.HAS_ACT, act);
			m.add(act, RDF.type, Vocab.COMMUNICATIVE_ACT);
			Resource req = m.createResource(act.toString() + "/req");
			m.add(act, Vocab.HAS_REQ, req);
			Resource sat = m.createResource(act.toString() + "/sat");
			m.add(act, Vocab.HAS_SAT, sat);
			for (Resource r : myKI.getKnowledgeInteraction().getAct().getRequirementPurposes()) {
				m.add(req, RDF.type, r);
			}
			for (Resource r : myKI.getKnowledgeInteraction().getAct().getSatisfactionPurposes()) {
				m.add(sat, RDF.type, r);
			}

			switch (myKI.getType()) {
			case ASK:
				m.add(ki, RDF.type, Vocab.ASK_KI);
				Resource gp = m.createResource(myKI.getId() + "/gp");
				m.add(ki, Vocab.HAS_GP, gp);
				m.add(gp, RDF.type, Vocab.GRAPH_PATTERN);
				m.add(gp, Vocab.HAS_PATTERN, m.createLiteral(this
						.convertToPattern(((AskKnowledgeInteraction) myKI.getKnowledgeInteraction()).getPattern())));
				break;
			case ANSWER:
				m.add(ki, RDF.type, Vocab.ANSWER_KI);
				gp = m.createResource(myKI.getId() + "/gp");
				m.add(ki, Vocab.HAS_GP, gp);
				m.add(gp, RDF.type, Vocab.GRAPH_PATTERN);
				m.add(gp, Vocab.HAS_PATTERN, m.createLiteral(this
						.convertToPattern(((AnswerKnowledgeInteraction) myKI.getKnowledgeInteraction()).getPattern())));
				break;
			case POST:
				m.add(ki, RDF.type, Vocab.POST_KI);
				Resource argGp = m.createResource(myKI.getId() + "/argumentgp");
				m.add(ki, Vocab.HAS_GP, argGp);
				m.add(argGp, RDF.type, Vocab.ARGUMENT_GRAPH_PATTERN);
				GraphPattern argument = ((PostKnowledgeInteraction) myKI.getKnowledgeInteraction()).getArgument();
				if (argument != null)
					m.add(argGp, Vocab.HAS_PATTERN, m.createLiteral(this.convertToPattern(argument)));

				// CHECK: If the KI doesn't have a result gp, do we still need to create
				// these resources? Currently, we do.
				Resource resGp = m.createResource(myKI.getId() + "/resultgp");
				m.add(ki, Vocab.HAS_GP, resGp);
				m.add(resGp, RDF.type, Vocab.RESULT_GRAPH_PATTERN);
				GraphPattern result = ((PostKnowledgeInteraction) myKI.getKnowledgeInteraction()).getResult();
				if (result != null)
					m.add(resGp, Vocab.HAS_PATTERN, m.createLiteral(this.convertToPattern(result)));
				break;
			case REACT:
				m.add(ki, RDF.type, Vocab.REACT_KI);
				argGp = m.createResource(myKI.getId() + "/argumentgp");
				m.add(ki, Vocab.HAS_GP, argGp);
				m.add(argGp, RDF.type, Vocab.ARGUMENT_GRAPH_PATTERN);
				argument = ((ReactKnowledgeInteraction) myKI.getKnowledgeInteraction()).getArgument();
				if (argument != null)
					m.add(argGp, Vocab.HAS_PATTERN, m.createLiteral(this.convertToPattern(argument)));

				// CHECK: If the KI doesn't have a result gp, do we still need to create
				// these resources? Currently, we do.
				resGp = m.createResource(myKI.getId() + "/resultgp");
				m.add(ki, Vocab.HAS_GP, resGp);
				m.add(resGp, RDF.type, Vocab.RESULT_GRAPH_PATTERN);
				result = ((ReactKnowledgeInteraction) myKI.getKnowledgeInteraction()).getResult();
				if (result != null)
					m.add(resGp, Vocab.HAS_PATTERN, m.createLiteral(this.convertToPattern(result)));
				break;
			default:
				this.LOG.warn("Ignored currently unsupported knowledge interaction type {}.", myKI.getType());
				assert false;
			}
		}

//		System.out.println("------------------------");
//		m.write(System.out, "turtle");
//		System.out.println("------------------------");

		// then use the Knowledge Interaction as a query to retrieve the bindings.
		Query q = QueryFactory.create("SELECT * WHERE {" + this.convertToPattern(this.metaGraphPattern) + "}");
		LOG.trace("Query: {}", q);
		QueryExecution qe = QueryExecutionFactory.create(q, m);
		ResultSet rs = qe.execSelect();
		BindingSet bindings = new BindingSet(rs);
		qe.close();

		LOG.trace("BindingSet: {}", bindings);

		String val = bindings.iterator().next().get("isMeta");

		if (incoming != null) {
			Util.removeRedundantBindingsAnswer(incoming, bindings);
		}

		return bindings;
	}

	private String convertToPattern(GraphPattern gp) {
		Iterator<TriplePath> iter = gp.getGraphPattern().patternElts();

		StringBuilder sb = new StringBuilder();

		while (iter.hasNext()) {

			TriplePath tp = iter.next();
			sb.append(FmtUtils.stringForTriple(tp.asTriple(), new PrefixMappingMem()));
			sb.append(" . ");
		}

		return sb.toString();
	}

	@Override
	public CompletableFuture<OtherKnowledgeBase> getOtherKnowledgeBase(URI toKnowledgeBaseId) {
		// Use the knowledge base ID to construct a meta knowledge interaction ID,
		// and add it to the message. Once the message dispatcher is done, we are
		// also responsible for constructing the OtherKnowledgeBase object. What is
		// sent from here to the message dispatcher is handled (in the other SC) in
		// processAskFromMessageRouter.

		var toKnowledgeInteraction = this.knowledgeBaseStore.getMetaId(toKnowledgeBaseId,
				KnowledgeInteractionInfo.Type.ANSWER, null);
		var fromKnowledgeInteraction = this.knowledgeBaseStore.getMetaId(this.myKnowledgeBaseId,
				KnowledgeInteractionInfo.Type.ASK, null);
		var askMsg = new AskMessage(this.myKnowledgeBaseId, fromKnowledgeInteraction, toKnowledgeBaseId,
				toKnowledgeInteraction, new BindingSet());
		try {
			CompletableFuture<OtherKnowledgeBase> future = this.messageRouter.sendAskMessage(askMsg)
					.thenApply(answerMsg -> {
						try {
							this.LOG.trace("Received message: {}", answerMsg);
							var otherKB = this.constructOtherKnowledgeBaseFromBindingSet(answerMsg.getBindings(), toKnowledgeBaseId);
							return otherKB;
						} catch (Throwable t) {
							this.LOG.error("The construction of other knowledge base should succeed.", t);
							// TODO do we want to complete the future exceptionally, here? Because we have a
							// condition that otherKnowledgeBase should NEVER be null.
							return null;
						}
					});
			return future;
		} catch (IOException e) {
			var failedFuture = new CompletableFuture<OtherKnowledgeBase>();
			failedFuture.completeExceptionally(e);
			return failedFuture;
		}
	}

	private OtherKnowledgeBase constructOtherKnowledgeBaseFromBindingSet(BindingSet bindings, URI otherKnowledgeBaseId) {
		assert !bindings.isEmpty() : "An answer meta message should always have at least a single binding.";

		Model model;
		try {
			model = Util.generateModel(this.metaGraphPattern, bindings);
		} catch (ParseException e) {
			this.LOG.warn("Received parse error while generating model.", e);
			assert false;
			return null;
		}
		model.setNsPrefix("kb", Vocab.ONTO_URI);

		// StringWriter sw = new StringWriter();
		// model.write(sw, "turtle");
		// this.LOG.trace("Incoming RDF: {}", sw.toString());

		ResIterator listSubjectsWithProperty = model.listSubjectsWithProperty(RDF.type, Vocab.KNOWLEDGE_BASE);
		Resource kb = listSubjectsWithProperty.next();
		assert !listSubjectsWithProperty.hasNext();

		String name = model.listObjectsOfProperty(kb, Vocab.HAS_NAME).next().asLiteral().getString();
		String description = model.listObjectsOfProperty(kb, Vocab.HAS_DESCR).next().asLiteral().getString();

		// Commented out because we don't need this for now.
		// Resource sc = model.listObjectsOfProperty(kb,
		// model.getProperty("kb:hasSmartConnector")).next().asResource();
		// String endpointString = model.listObjectsOfProperty(sc,
		// model.getProperty("kb:hasEndpoint")).next().asLiteral().getString();
		// URL endpoint;
		// try {
		// endpoint = new URL(endpointString);
		// } catch (MalformedURLException e1) {
		// LOG.warn("Invalid URL for endpoint.", e1);
		// assert false;
		// return null;
		// }

		ExtendedIterator<Resource> kis = model.listObjectsOfProperty(kb, Vocab.HAS_KI).mapWith(RDFNode::asResource);

		var knowledgeInteractions = new ArrayList<KnowledgeInteractionInfo>();
		for (Resource ki : kis.toList()) {
			var kiType = model.listObjectsOfProperty(ki, RDF.type).next();
			var kiMeta = model.listObjectsOfProperty(ki, Vocab.IS_META).next();

			boolean isMeta = kiMeta.asLiteral().getBoolean();
			assert isMeta == kiMeta.toString().contains("true") : "If the text contains 'true' (=" + kiMeta
					+ ") then the boolean should be true.";

			this.LOG.trace("meta: {} = {}", FmtUtils.stringForNode(kiMeta.asNode()), isMeta);

			// retrieve acts
			Resource act = model.getProperty(ki, Vocab.HAS_ACT).getObject().asResource();
			Resource req = model.getProperty(act, Vocab.HAS_REQ).getObject().asResource();
			Resource sat = model.getProperty(act, Vocab.HAS_SAT).getObject().asResource();

			ExtendedIterator<Resource> reqTypes = model.listObjectsOfProperty(req, RDF.type)
					.mapWith(RDFNode::asResource);
			ExtendedIterator<Resource> satTypes = model.listObjectsOfProperty(sat, RDF.type)
					.mapWith(RDFNode::asResource);

			Set<Resource> reqPurposes = new HashSet<>();
			Resource r = null;
			while (reqTypes.hasNext()) {
				r = reqTypes.next();
				reqPurposes.add(r);
			}
			reqTypes.close();

			Set<Resource> satPurposes = new HashSet<>();
			while (satTypes.hasNext()) {
				r = satTypes.next();
				satPurposes.add(r);
			}
			satTypes.close();

			CommunicativeAct actObject = new CommunicativeAct(reqPurposes, satPurposes);

			// fill knowledge interactions
			try {

				if (kiType.equals(Vocab.ASK_KI) || kiType.equals(Vocab.ANSWER_KI)) {
					Resource graphPattern = model.listObjectsOfProperty(ki, Vocab.HAS_GP).next().asResource();
					String graphPatternString = model.listObjectsOfProperty(graphPattern, Vocab.HAS_PATTERN).next()
							.asLiteral().getString();
					if (kiType.equals(Vocab.ASK_KI)) {
						AskKnowledgeInteraction askKnowledgeInteraction = new AskKnowledgeInteraction(actObject,
								new GraphPattern(graphPatternString), isMeta, isMeta);

						KnowledgeInteractionInfo knowledgeInteractionInfo = new KnowledgeInteractionInfo(
								new URI(ki.toString()), new URI(kb.toString()), askKnowledgeInteraction);

						knowledgeInteractions.add(knowledgeInteractionInfo);
					} else if (kiType.equals(Vocab.ANSWER_KI)) {
						AnswerKnowledgeInteraction answerKnowledgeInteraction = new AnswerKnowledgeInteraction(
								actObject, new GraphPattern(graphPatternString), isMeta, isMeta);

						KnowledgeInteractionInfo knowledgeInteractionInfo = new KnowledgeInteractionInfo(
								new URI(ki.toString()), new URI(kb.toString()), answerKnowledgeInteraction);

						knowledgeInteractions.add(knowledgeInteractionInfo);
					}
				} else if (kiType.equals(Vocab.POST_KI) || kiType.equals(Vocab.REACT_KI)) {

					// read the argument and (optional) result patterns.
					NodeIterator graphPatternIterator = model.listObjectsOfProperty(ki, Vocab.HAS_GP);
					String argumentGraphPatternString = null;
					String resultGraphPatternString = null;
					while (graphPatternIterator.hasNext()) {
						Resource graphPattern = graphPatternIterator.next().asResource();
						Resource gpType = graphPattern.getPropertyResourceValue(RDF.type);
						if (gpType.equals(Vocab.ARGUMENT_GRAPH_PATTERN)) {
							if (argumentGraphPatternString != null) {
								throw new IllegalArgumentException("Knowledge interaction cannot have multiple argument patterns.");
							}
							argumentGraphPatternString = graphPattern.getProperty(Vocab.HAS_PATTERN).getString();
						} else if (gpType.equals(Vocab.RESULT_GRAPH_PATTERN)) {
							if (resultGraphPatternString != null) {
								throw new IllegalArgumentException("Knowledge interaction cannot have multiple result patterns.");
							}
							resultGraphPatternString = graphPattern.getProperty(Vocab.HAS_PATTERN).getString();
						} else {
							throw new IllegalArgumentException(String.format("For a POST/REACT Knowledge Interaction, their graph pattern must be either %s or %s. Not %s.", Vocab.ARGUMENT_GRAPH_PATTERN, Vocab.RESULT_GRAPH_PATTERN, gpType));
						}
					}
					
					if (argumentGraphPatternString == null) {
						throw new IllegalArgumentException(
								"Every Post or React Knowledge Interaction should have an argument graph pattern.");
					}

					if (kiType.equals(Vocab.POST_KI)) {

						this.LOG.trace("{} - {}", argumentGraphPatternString, resultGraphPatternString);

						PostKnowledgeInteraction postKnowledgeInteraction = new PostKnowledgeInteraction(actObject,
								new GraphPattern(argumentGraphPatternString),
								resultGraphPatternString != null ? new GraphPattern(resultGraphPatternString) : null,
								isMeta, isMeta);
						KnowledgeInteractionInfo knowledgeInteractionInfo = new KnowledgeInteractionInfo(
								new URI(ki.toString()), new URI(kb.toString()), postKnowledgeInteraction);
						knowledgeInteractions.add(knowledgeInteractionInfo);
					} else if (kiType.equals(Vocab.REACT_KI)) {
						ReactKnowledgeInteraction reactKnowledgeInteraction = new ReactKnowledgeInteraction(actObject,
								new GraphPattern(argumentGraphPatternString),
								resultGraphPatternString != null ? new GraphPattern(resultGraphPatternString) : null,
								isMeta, isMeta);
						KnowledgeInteractionInfo knowledgeInteractionInfo = new KnowledgeInteractionInfo(
								new URI(ki.toString()), new URI(kb.toString()), reactKnowledgeInteraction);
						knowledgeInteractions.add(knowledgeInteractionInfo);
					}
				} else {
					this.LOG.warn("Ignored unexpected knowledge interaction type {}", kiType);
				}
			} catch (URISyntaxException e) {
				this.LOG.error("The URIs should be correctly formatted.", e);
			}

		}

		URI kbId = null;
		try {
			kbId = new URI(kb.getURI());
		} catch (URISyntaxException e) {
			LOG.error("Invalid URI for knowledge base: " + kb.getURI(), e);
			return null;
		}
		if (kbId != otherKnowledgeBaseId) {
			LOG.error("Received KB metadata about a KB from another KB! This is not allowed.");
			return null;
		}

		return new OtherKnowledgeBase(kbId, name, description, knowledgeInteractions, null);
	}

	@Override
	public CompletableFuture<PostResult> postNewKnowledgeBase() {
		var kiInfo = this.knowledgeBaseStore.getKnowledgeInteractionByObject(this.metaPostNewKI);
		return this.interactionProcessor.planPostFromKnowledgeBase(kiInfo, new RecipientSelector())
				.execute(this.fillMetaBindings(null));
	}

	@Override
	public void knowledgeInteractionRegistered(KnowledgeInteractionInfo ki) {
		if (!ki.isMeta()) {
			var kiInfo = this.knowledgeBaseStore.getKnowledgeInteractionByObject(this.metaPostChangedKI);
			try {
				this.interactionProcessor.planPostFromKnowledgeBase(kiInfo, new RecipientSelector())
						.execute(this.fillMetaBindings(null)).get();
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("No error should occur while notifying others of a registered knowledge interaction.");
			}
		}
	}

	@Override
	public void knowledgeInteractionUnregistered(KnowledgeInteractionInfo ki) {
		if (!ki.isMeta()) {
			var kiInfo = this.knowledgeBaseStore.getKnowledgeInteractionByObject(this.metaPostChangedKI);
			try {
				this.interactionProcessor.planPostFromKnowledgeBase(kiInfo, new RecipientSelector())
						.execute(this.fillMetaBindings(null)).get();
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("No error should occur while notifying others of an unregistered knowledge interaction.");
			}
		}
	}

	@Override
	public void smartConnectorStopping() {
		try {

			var kiInfo = this.knowledgeBaseStore.getKnowledgeInteractionByObject(this.metaPostRemovedKI);
			Set<OtherKnowledgeBase> otherKnowledgeBases = this.otherKnowledgeBaseStore.getOtherKnowledgeBases();
			// Block on the future, but wait no longer than the timeout.
			long timeout = POST_REMOVED_TIMEOUT_MILLIS_PER_OTHERKB
					+ otherKnowledgeBases.size() * POST_REMOVED_TIMEOUT_MILLIS_PER_OTHERKB;
			LOG.debug("Waiting for max {}ms for other KBs to ack my termination message.", timeout);
			this.interactionProcessor.planPostFromKnowledgeBase(kiInfo, new RecipientSelector())
					.execute(this.fillMetaBindings(null)).get(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			LOG.error("An error occured while informing other KBs about our "
					+ "termination. Proceeding to stop the smart connector regardless.", e);
		}
	}

	@Override
	public void setOtherKnowledgeBaseStore(OtherKnowledgeBaseStore otherKnowledgeBaseStore) {
		this.otherKnowledgeBaseStore = otherKnowledgeBaseStore;

	}

	@Override
	public void setInteractionProcessor(InteractionProcessor interactionProcessor) {
		this.interactionProcessor = interactionProcessor;

	}

}
