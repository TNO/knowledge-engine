package interconnect.ke.sc;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.binding.Binding;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;

public class MetaKnowledgeBaseImpl implements MetaKnowledgeBase {

	private static final Logger LOG = LoggerFactory.getLogger(MetaKnowledgeBaseImpl.class);

	private static final String ASK_SUFFIX = "/meta/knowledgeinteractions/ask";
	private static final String ANSWER_SUFFIX = "/meta/knowledgeinteractions/answer";

	private static final Resource ASK_KI = ResourceFactory
			.createResource("https://www.tno.nl/energy/ontology/interconnect#AskKnowledgeInteraction");
	private static final Resource ANSWER_KI = ResourceFactory
			.createResource("https://www.tno.nl/energy/ontology/interconnect#AnswerKnowledgeInteraction");

	private final URI myKnowledgeBaseId;
	private final MessageRouter messageRouter;
	private final GraphPattern metaGraphPattern;
	private final MyKnowledgeBaseStore myKnowledgeBaseStore;

	public MetaKnowledgeBaseImpl(MessageRouter aMessageRouter,
			MyKnowledgeBaseStore aKnowledgeBaseStore) {
		this.myKnowledgeBaseId = aKnowledgeBaseStore.getKnowledgeBaseId();
		this.messageRouter = aMessageRouter;
		this.myKnowledgeBaseStore = aKnowledgeBaseStore;

		var prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("kb", "https://www.tno.nl/energy/ontology/interconnect#");
		this.metaGraphPattern = new GraphPattern(prefixes,
				"?kb rdf:type kb:KnowledgeBase . ?kb kb:hasName ?name . ?kb kb:hasDescription ?description . ?kb kb:hasKnowledgeInteraction ?ki . ?ki rdf:type ?kiType . ?ki kb:isMeta ?isMeta . ?ki kb:hasGraphPattern ?gp . ?ki kb:hasPattern ?pattern .");
	}

	@Override
	public AnswerMessage processAskFromMessageRouter(AskMessage anAskMessage) {
		assert anAskMessage.getToKnowledgeBase() == this.myKnowledgeBaseId;
		assert anAskMessage.getToKnowledgeInteraction() == this
				.constructAnswerMetaKnowledgeInteractionId(this.myKnowledgeBaseId);
		assert anAskMessage.getFromKnowledgeInteraction() == this
				.constructAskMetaKnowledgeInteractionId(anAskMessage.getFromKnowledgeBase());

		URI fromKnowledgeInteraction = this.constructAnswerMetaKnowledgeInteractionId(this.myKnowledgeBaseId);
		URI toKnowledgeInteraction = this.constructAskMetaKnowledgeInteractionId(anAskMessage.getFromKnowledgeBase());

		BindingSet bindings = new BindingSet();

		for (MyKnowledgeInteractionInfo knowledgeInteractionInfo : myKnowledgeBaseStore.getKnowledgeInteractions()) {
			Binding binding = new Binding();
			binding.put("kb", "<" + this.myKnowledgeBaseId.toString() + ">");
			binding.put("name", myKnowledgeBaseStore.getKnowledgeBaseName());
			binding.put("description", myKnowledgeBaseStore.getKnowledgeBaseDescription());
			// binding.put("sc", "TODO"); // TODO
			// binding.put("endpoint", "TODO"); // TODO
			binding.put("ki", "<" + knowledgeInteractionInfo.getId() + ">");
			binding.put("isMeta", "false");
			var knowledgeInteraction = knowledgeInteractionInfo.getKnowledgeInteraction();
			switch (knowledgeInteractionInfo.getType()) {
			case ASK:
				binding.put("gp", "TODO"); // TODO
				binding.put("pattern", ((AskKnowledgeInteraction) knowledgeInteraction).getPattern().getPattern());
				break;
			case ANSWER:
				binding.put("gp", "TODO"); // TODO
				binding.put("pattern", ((AskKnowledgeInteraction) knowledgeInteraction).getPattern().getPattern());
				break;
			default:
				LOG.warn("Ignored currently unsupported knowledge interaction type " + knowledgeInteractionInfo.getType());
				assert false;
			}
		}

		var answerMessage = new AnswerMessage(this.myKnowledgeBaseId, fromKnowledgeInteraction,
				anAskMessage.getFromKnowledgeBase(), toKnowledgeInteraction, anAskMessage.getMessageId(), bindings);

		return answerMessage;
	}

	@Override
	public ReactMessage processPostFromMessageRouter(PostMessage aPostMessage) {
		// TODO After MVP
		return null;
	}

	@Override
	public CompletableFuture<OtherKnowledgeBase> getOtherKnowledgeBase(URI toKnowledgeBaseId) {
		// Use the knowledge base ID to construct a meta knowledge interaction ID,
		// and add it to the message. Once the message dispatcher is done, we are
		// also responsible for constructing the OtherKnowledgeBase object. What is
		// sent from here to the message dispatcher is handled (in the other SC) in
		// processAskFromMessageRouter.

		var toKnowledgeInteraction = this.constructAnswerMetaKnowledgeInteractionId(toKnowledgeBaseId);
		var fromKnowledgeInteraction = this.constructAskMetaKnowledgeInteractionId(this.myKnowledgeBaseId);
		var askMsg = new AskMessage(this.myKnowledgeBaseId, fromKnowledgeInteraction, toKnowledgeBaseId,
				toKnowledgeInteraction, new BindingSet());
		try {
			CompletableFuture<OtherKnowledgeBase> future = this.messageRouter.sendAskMessage(askMsg)
					.thenApply((answerMsg) -> this.constructOtherKnowledgeBaseFromAnswerMessage(answerMsg));
			return future;
		} catch (IOException e) {
			var failedFuture = new CompletableFuture<OtherKnowledgeBase>();
			failedFuture.completeExceptionally(e);
			return failedFuture;
		}
	}

	private URI constructAskMetaKnowledgeInteractionId(URI knowledgeBaseId) {
		return knowledgeBaseId.resolve(ASK_SUFFIX);
	}

	private URI constructAnswerMetaKnowledgeInteractionId(URI knowledgeBaseId) {
		return knowledgeBaseId.resolve(ANSWER_SUFFIX);
	}

	private OtherKnowledgeBase constructOtherKnowledgeBaseFromAnswerMessage(AnswerMessage answerMessage) {
		var knowledgeBaseId = answerMessage.getFromKnowledgeBase();
		var bindings = answerMessage.getBindings();

		Model model;
		try {
			model = Util.generateModel(this.metaGraphPattern, bindings);
		} catch (ParseException e) {
			LOG.warn("Received parse error while generating model.", e);
			assert false;
			return null;
		}
		model.setNsPrefix("kb", "https://www.tno.nl/energy/ontology/interconnect#");

		Resource kb = model.listSubjectsWithProperty(RDF.type, "kb:KnowledgeBase").next();
		String name = model.listObjectsOfProperty(kb, model.getProperty("kb:hasName")).next().asLiteral().getString();
		String description = model.listObjectsOfProperty(kb, model.getProperty("kb:hasDescription")).next().asLiteral()
				.getString();

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

		ExtendedIterator<Resource> kis = model.listObjectsOfProperty(kb, model.getProperty("kb:hasKnowledgeInteraction"))
				.mapWith((n) -> n.asResource());

		var knowledgeInteractions = new ArrayList<KnowledgeInteraction>();
		for (Resource ki : kis.toList()) {
			Resource graphPattern = model.listObjectsOfProperty(ki, model.getProperty("kb:hasGraphPattern")).next()
					.asResource();
			String graphPatternString = model.listObjectsOfProperty(graphPattern, model.getProperty("kb:hasPattern")).next()
					.asLiteral().getString();
			var kiType = model.listObjectsOfProperty(ki, RDF.type).next();
			if (kiType.equals(ASK_KI)) {
				knowledgeInteractions
						.add(new AskKnowledgeInteraction(new CommunicativeAct(), new GraphPattern(graphPatternString)));
			} else if (kiType.equals(ANSWER_KI)) {
				knowledgeInteractions
						.add(new AnswerKnowledgeInteraction(new CommunicativeAct(), new GraphPattern(graphPatternString)));
			} else {
				LOG.warn("Ignored unexpected knowledge interaction type " + kiType);
			}
		}

		return new OtherKnowledgeBase(knowledgeBaseId, name, description, knowledgeInteractions, null);
	}

	@Override
	public boolean isMetaKnowledgeInteraction(URI id) {
		String urlString = id.toString();
		// For now, a meta knowledge interaction id can be identified by its suffix.
		return urlString.endsWith(ASK_SUFFIX) || urlString.endsWith(ANSWER_SUFFIX);
	}
}
