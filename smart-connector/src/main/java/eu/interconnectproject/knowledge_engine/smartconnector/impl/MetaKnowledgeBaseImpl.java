package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
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

import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AskKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.CommunicativeAct;
import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;
import eu.interconnectproject.knowledge_engine.smartconnector.api.PostKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AnswerMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AskMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.PostMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.ReactMessage;

public class MetaKnowledgeBaseImpl implements MetaKnowledgeBase, KnowledgeBaseStoreListener {

	private final Logger LOG;

	private final URI myKnowledgeBaseId;
	private final MessageRouter messageRouter;
	private final GraphPattern metaGraphPattern;
	private final KnowledgeBaseStore knowledgeBaseStore;
	private OtherKnowledgeBaseStore otherKnowledgeBaseStore;

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
				"?ki kb:hasGraphPattern ?gp .", "?ki ?patternType ?gp .", "?gp rdf:type kb:GraphPattern .",
				"?gp kb:hasPattern ?pattern .");

		// create answer knowledge interaction
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), this.metaGraphPattern);
		this.knowledgeBaseStore.register(aKI, (anAKI, aBindingSet) -> this.fillMetaBindings(), true);

		// create ask knowledge interaction
		AskKnowledgeInteraction aKI2 = new AskKnowledgeInteraction(new CommunicativeAct(), this.metaGraphPattern);
		this.knowledgeBaseStore.register(aKI2, true);

		PostKnowledgeInteraction pKINew = new PostKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.NEW_KNOWLEDGE_PURPOSE))),
				this.metaGraphPattern, null);
		this.knowledgeBaseStore.register(pKINew, true);
		PostKnowledgeInteraction pKIUpdated = new PostKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.CHANGED_KNOWLEDGE_PURPOSE))),
				this.metaGraphPattern, null);
		this.knowledgeBaseStore.register(pKIUpdated, true);
		PostKnowledgeInteraction pKIRemoved = new PostKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.REMOVED_KNOWLEDGE_PURPOSE))),
				this.metaGraphPattern, null);
		this.knowledgeBaseStore.register(pKIRemoved, true);

		ReactKnowledgeInteraction rKINew = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.NEW_KNOWLEDGE_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE))),
				this.metaGraphPattern, null);
		this.knowledgeBaseStore.register(rKINew, (aRKI, aBindingSet) -> new BindingSet(), true); // This handler
																									// shouldn't be
																									// called under
																									// normal
																									// circumstances.
		ReactKnowledgeInteraction rKIUpdated = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.CHANGED_KNOWLEDGE_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE))),
				this.metaGraphPattern, null);
		this.knowledgeBaseStore.register(rKIUpdated, (aRKI, aBindingSet) -> new BindingSet(), true); // This handler
																										// shouldn't be
																										// called under
																										// normal
																										// circumstances.
		ReactKnowledgeInteraction rKIRemoved = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.REMOVED_KNOWLEDGE_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE))),
				this.metaGraphPattern, null);
		this.knowledgeBaseStore.register(rKIRemoved, (aRKI, aBindingSet) -> new BindingSet(), true); // This handler
																										// shouldn't be
																										// called under
																										// normal
																										// circumstances.
	}

	@Override
	public void setOtherKnowledgeBaseStore(OtherKnowledgeBaseStore otherKnowledgeBaseStore) {
		this.otherKnowledgeBaseStore = otherKnowledgeBaseStore;
	}

	@Override
	public CompletableFuture<Void> postNewKnowledgeBase(Set<OtherKnowledgeBase> otherKnowledgeBases) {
		return this.postInformToKnowledgeBases(otherKnowledgeBases, Vocab.NEW_KNOWLEDGE_PURPOSE);
	}

	@Override
	public CompletableFuture<Void> postChangedKnowledgeBase(Set<OtherKnowledgeBase> otherKnowledgeBases) {
		return this.postInformToKnowledgeBases(otherKnowledgeBases, Vocab.CHANGED_KNOWLEDGE_PURPOSE);
	}

	@Override
	public CompletableFuture<Void> postRemovedKnowledgeBase(Set<OtherKnowledgeBase> otherKnowledgeBases) {
		return this.postInformToKnowledgeBases(otherKnowledgeBases, Vocab.REMOVED_KNOWLEDGE_PURPOSE);
	}

	private CompletableFuture<Void> postInformToKnowledgeBases(Set<OtherKnowledgeBase> otherKnowledgeBases,
			Resource purpose) {
		// Prepare the meta bindings once
		var myMetaBindings = this.fillMetaBindings();
		// And also my KI id. We re-use this every time.
		var fromKnowledgeInteraction = this.knowledgeBaseStore.getMetaId(this.myKnowledgeBaseId,
				KnowledgeInteractionInfo.Type.POST, purpose);

		// We keep track of all the interactions in this set of futures.
		var futures = new HashSet<CompletableFuture<ReactMessage>>();

		// Then send messages to all knowledge peers
		for (OtherKnowledgeBase kb : otherKnowledgeBases) {
			// Construct the knowledge interaction ID of the receiving side.
			var toKnowledgeInteraction = this.knowledgeBaseStore.getMetaId(kb.getId(),
					KnowledgeInteractionInfo.Type.REACT, purpose);

			// Construct and send the message, and keep track of the future.
			PostMessage msg = new PostMessage(this.myKnowledgeBaseId, fromKnowledgeInteraction, kb.getId(),
					toKnowledgeInteraction, myMetaBindings);
			try {
				futures.add(this.messageRouter.sendPostMessage(msg));
			} catch (IOException e) {
				this.LOG.error("Could not send POST message to KB " + kb.getId(), e);
			}
		}
		// Return a future that completes when all futures in it have completed.
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()]));
	}

	@Override
	public AnswerMessage processAskFromMessageRouter(AskMessage anAskMessage) {
		assert anAskMessage.getToKnowledgeBase().equals(this.myKnowledgeBaseId) : "the message should be for me";

		URI toKnowledgeInteraction = anAskMessage.getFromKnowledgeInteraction();
		URI fromKnowledgeInteraction = this.knowledgeBaseStore
				.getKnowledgeInteractionById(anAskMessage.getToKnowledgeInteraction()).id;

		BindingSet bindings = this.fillMetaBindings();

		var answerMessage = new AnswerMessage(this.myKnowledgeBaseId, fromKnowledgeInteraction,
				anAskMessage.getFromKnowledgeBase(), toKnowledgeInteraction, anAskMessage.getMessageId(), bindings);
		this.LOG.trace("Sending meta message: {}", answerMessage);

		return answerMessage;
	}

	private BindingSet fillMetaBindings() {

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
//			m.add(ki, RDF.type, Vocab.KNOWLEDGE_INTERACTION);
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
				m.add(ki, Vocab.HAS_ARG, argGp);
				m.add(argGp, RDF.type, Vocab.GRAPH_PATTERN);
				GraphPattern argument = ((PostKnowledgeInteraction) myKI.getKnowledgeInteraction()).getArgument();
				if (argument != null)
					m.add(argGp, Vocab.HAS_PATTERN, m.createLiteral(this.convertToPattern(argument)));

				Resource resGp = m.createResource(myKI.getId() + "/resultgp");
				m.add(ki, Vocab.HAS_GP, resGp);
				m.add(ki, Vocab.HAS_RES, resGp);
				m.add(resGp, RDF.type, Vocab.GRAPH_PATTERN);
				GraphPattern result = ((PostKnowledgeInteraction) myKI.getKnowledgeInteraction()).getResult();
				if (result != null)
					m.add(resGp, Vocab.HAS_PATTERN, m.createLiteral(this.convertToPattern(result)));
				break;
			case REACT:
				m.add(ki, RDF.type, Vocab.REACT_KI);
				argGp = m.createResource(myKI.getId() + "/argumentgp");
				m.add(ki, Vocab.HAS_GP, argGp);
				m.add(ki, Vocab.HAS_ARG, argGp);
				m.add(argGp, RDF.type, Vocab.GRAPH_PATTERN);
				argument = ((ReactKnowledgeInteraction) myKI.getKnowledgeInteraction()).getArgument();
				if (argument != null)
					m.add(argGp, Vocab.HAS_PATTERN, m.createLiteral(this.convertToPattern(argument)));

				resGp = m.createResource(myKI.getId() + "/resultgp");
				m.add(ki, Vocab.HAS_GP, resGp);
				m.add(ki, Vocab.HAS_RES, resGp);
				m.add(resGp, RDF.type, Vocab.GRAPH_PATTERN);
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
	public ReactMessage processPostFromMessageRouter(PostMessage aPostMessage) {
		assert aPostMessage.getToKnowledgeBase().equals(this.myKnowledgeBaseId);

		// Parse the incoming PostMessage and inform `this.otherKnowledgeBaseStore`

		OtherKnowledgeBase otherKB = this.constructOtherKnowledgeBaseFromBindingSet(aPostMessage.getArgument());
		assert otherKB.getId().equals(aPostMessage.getFromKnowledgeBase());

		var purpose = this.knowledgeBaseStore.getPurpose(aPostMessage.getFromKnowledgeBase(),
				aPostMessage.getFromKnowledgeInteraction());

		var myKI = this.knowledgeBaseStore.getMetaId(this.myKnowledgeBaseId, KnowledgeInteractionInfo.Type.REACT,
				purpose);
		assert myKI.equals(aPostMessage.getToKnowledgeInteraction()) : myKI + " is not "
				+ aPostMessage.getToKnowledgeInteraction();

		if (purpose.equals(Vocab.NEW_KNOWLEDGE_PURPOSE)) {
			this.otherKnowledgeBaseStore.addKnowledgeBase(otherKB);
		} else if (purpose.equals(Vocab.CHANGED_KNOWLEDGE_PURPOSE)) {
			this.otherKnowledgeBaseStore.updateKnowledgeBase(otherKB);
		} else if (purpose.equals(Vocab.REMOVED_KNOWLEDGE_PURPOSE)) {
			this.otherKnowledgeBaseStore.removeKnowledgeBase(otherKB);
		}

		return new ReactMessage(this.myKnowledgeBaseId, myKI, aPostMessage.getFromKnowledgeBase(),
				aPostMessage.getFromKnowledgeInteraction(), aPostMessage.getMessageId(), new BindingSet());
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
							var otherKB = this.constructOtherKnowledgeBaseFromBindingSet(answerMsg.getBindings());
							assert otherKB.getId().equals(answerMsg.getFromKnowledgeBase());
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

	private OtherKnowledgeBase constructOtherKnowledgeBaseFromBindingSet(BindingSet bindings) {
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

		StringWriter sw = new StringWriter();
		model.write(sw, "turtle");
		this.LOG.trace("Incoming RDF: {}", sw.toString());

		Resource kb = model.listSubjectsWithProperty(RDF.type, Vocab.KNOWLEDGE_BASE).next();

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
								new GraphPattern(graphPatternString));

						KnowledgeInteractionInfo knowledgeInteractionInfo = new KnowledgeInteractionInfo(
								new URI(ki.toString()), new URI(kb.toString()), askKnowledgeInteraction, isMeta);

						knowledgeInteractions.add(knowledgeInteractionInfo);
					} else if (kiType.equals(Vocab.ANSWER_KI)) {
						AnswerKnowledgeInteraction answerKnowledgeInteraction = new AnswerKnowledgeInteraction(
								actObject, new GraphPattern(graphPatternString));

						KnowledgeInteractionInfo knowledgeInteractionInfo = new KnowledgeInteractionInfo(
								new URI(ki.toString()), new URI(kb.toString()), answerKnowledgeInteraction, isMeta);

						knowledgeInteractions.add(knowledgeInteractionInfo);
					}
				} else if (kiType.equals(Vocab.POST_KI) || kiType.equals(Vocab.REACT_KI)) {

					// argument
					NodeIterator listObjectsOfProperty = model.listObjectsOfProperty(ki, Vocab.HAS_ARG);
					Resource argumentGraphPattern = null;
					String argumentGraphPatternString = null;
					if (listObjectsOfProperty.hasNext()) {
						argumentGraphPattern = listObjectsOfProperty.next().asResource();
						argumentGraphPatternString = model
								.listObjectsOfProperty(argumentGraphPattern, Vocab.HAS_PATTERN).next().asLiteral()
								.getString();
					}

					// result
					NodeIterator listObjectsOfProperty2 = model.listObjectsOfProperty(ki, Vocab.HAS_RES);
					Resource resultGraphPattern = null;
					String resultGraphPatternString = null;
					if (listObjectsOfProperty2.hasNext()) {
						resultGraphPattern = listObjectsOfProperty2.next().asResource();
						resultGraphPatternString = model.listObjectsOfProperty(resultGraphPattern, Vocab.HAS_PATTERN)
								.next().asLiteral().getString();
					}

					if (kiType.equals(Vocab.POST_KI)) {

						this.LOG.trace("{} - {}", argumentGraphPatternString, resultGraphPatternString);

						PostKnowledgeInteraction postKnowledgeInteraction = new PostKnowledgeInteraction(actObject,
								(argumentGraphPatternString != null ? new GraphPattern(argumentGraphPatternString)
										: null),
								(resultGraphPatternString != null ? new GraphPattern(resultGraphPatternString) : null));
						KnowledgeInteractionInfo knowledgeInteractionInfo = new KnowledgeInteractionInfo(
								new URI(ki.toString()), new URI(kb.toString()), postKnowledgeInteraction, isMeta);
						knowledgeInteractions.add(knowledgeInteractionInfo);
					} else if (kiType.equals(Vocab.REACT_KI)) {
						ReactKnowledgeInteraction reactKnowledgeInteraction = new ReactKnowledgeInteraction(actObject,
								(argumentGraphPatternString != null ? new GraphPattern(argumentGraphPatternString)
										: null),
								(resultGraphPatternString != null ? new GraphPattern(resultGraphPatternString) : null));
						KnowledgeInteractionInfo knowledgeInteractionInfo = new KnowledgeInteractionInfo(
								new URI(ki.toString()), new URI(kb.toString()), reactKnowledgeInteraction, isMeta);
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
		}

		return new OtherKnowledgeBase(kbId, name, description, knowledgeInteractions, null);
	}

	@Override
	public boolean isMetaKnowledgeInteraction(URI id) {
		return this.knowledgeBaseStore.getKnowledgeInteractionById(id).isMeta();
	}

	@Override
	public void knowledgeInteractionRegistered(KnowledgeInteractionInfo ki) {

		if (!ki.isMeta()) {
			try {
				this.postChangedKnowledgeBase(this.otherKnowledgeBaseStore.getOtherKnowledgeBases()).get();
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("Notifying other knowledgebases of a register, should not fail.", e);
			}
		}

	}

	@Override
	public void knowledgeInteractionUnregistered(KnowledgeInteractionInfo ki) {
		if (!ki.isMeta()) {
			try {
				this.postChangedKnowledgeBase(this.otherKnowledgeBaseStore.getOtherKnowledgeBases()).get();
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("Notifying other knowledgebases of a unregister, should not fail.", e);
			}
		}

	}

	@Override
	public void smartConnectorStopping() {
		try {
			// Block on the future.(TODO: Timeout?)
			this.postRemovedKnowledgeBase(this.otherKnowledgeBaseStore.getOtherKnowledgeBases()).get();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error("An error occured while informing peers about our "
					+ "termination. Proceeding to stop the smart connector regardless.", e);
		}
	}
}
