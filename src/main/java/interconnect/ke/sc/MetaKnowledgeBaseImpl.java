package interconnect.ke.sc;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;

import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.binding.Binding;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;
import interconnect.ke.sc.KnowledgeInteractionInfo.Type;

public class MetaKnowledgeBaseImpl implements MetaKnowledgeBase {

	private final Logger LOG;

	private static final String ASK_SUFFIX = "/meta/knowledgeinteractions/ask";
	private static final String ANSWER_SUFFIX = "/meta/knowledgeinteractions/answer";

	private static final Resource ASK_KI = ResourceFactory
			.createResource("https://www.tno.nl/energy/ontology/interconnect#AskKnowledgeInteraction");
	private static final Resource ANSWER_KI = ResourceFactory
			.createResource("https://www.tno.nl/energy/ontology/interconnect#AnswerKnowledgeInteraction");
	private static final Resource POST_KI = ResourceFactory
			.createResource("https://www.tno.nl/energy/ontology/interconnect#PostKnowledgeInteraction");
	private static final Resource REACT_KI = ResourceFactory
			.createResource("https://www.tno.nl/energy/ontology/interconnect#ReactKnowledgeInteraction");

	private final URI myKnowledgeBaseId;
	private final MessageRouter messageRouter;
	private final GraphPattern metaGraphPattern;
	private final MyKnowledgeBaseStore myKnowledgeBaseStore;

	public MetaKnowledgeBaseImpl(LoggerProvider loggerProvider, MessageRouter aMessageRouter,
			MyKnowledgeBaseStore aKnowledgeBaseStore) {
		this.LOG = loggerProvider.getLogger(this.getClass());

		this.myKnowledgeBaseId = aKnowledgeBaseStore.getKnowledgeBaseId();
		this.messageRouter = aMessageRouter;
		this.myKnowledgeBaseStore = aKnowledgeBaseStore;

		var prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("kb", "https://www.tno.nl/energy/ontology/interconnect#");
		this.metaGraphPattern = new GraphPattern(prefixes,
				"?kb rdf:type kb:KnowledgeBase . ?kb kb:hasName ?name . ?kb kb:hasDescription ?description . ?kb kb:hasKnowledgeInteraction ?ki . ?ki rdf:type ?kiType . ?ki kb:isMeta ?isMeta . ?ki kb:hasGraphPattern ?gp . ?ki ?patternType ?gp . ?gp rdf:type kb:GraphPattern . ?gp kb:hasPattern ?pattern ."
		);
	}

	@Override
	public AnswerMessage processAskFromMessageRouter(AskMessage anAskMessage) {
		assert anAskMessage.getToKnowledgeBase().equals(this.myKnowledgeBaseId) : "the message should be for me";
		assert anAskMessage.getToKnowledgeInteraction()
				.equals(this.constructAnswerMetaKnowledgeInteractionId(this.myKnowledgeBaseId));
		assert anAskMessage.getFromKnowledgeInteraction()
				.equals(this.constructAskMetaKnowledgeInteractionId(anAskMessage.getFromKnowledgeBase()));

		URI fromKnowledgeInteraction = this.constructAnswerMetaKnowledgeInteractionId(this.myKnowledgeBaseId);
		URI toKnowledgeInteraction = this.constructAskMetaKnowledgeInteractionId(anAskMessage.getFromKnowledgeBase());

		BindingSet bindings = new BindingSet();

		for (KnowledgeInteractionInfo knowledgeInteractionInfo : this.myKnowledgeBaseStore.getKnowledgeInteractions()) {
			Binding binding = new Binding();
			binding.put("kb", "<" + this.myKnowledgeBaseId.toString() + ">");
			binding.put("name", "\"" + this.myKnowledgeBaseStore.getKnowledgeBaseName() + "\"");
			binding.put("description", "\"" + this.myKnowledgeBaseStore.getKnowledgeBaseDescription() + "\"");
			// binding.put("sc", "TODO"); // TODO
			// binding.put("endpoint", "TODO"); // TODO
			binding.put("ki", "<" + knowledgeInteractionInfo.getId() + ">");
			binding.put("isMeta", "\"false\"");
			var knowledgeInteraction = knowledgeInteractionInfo.getKnowledgeInteraction();
			switch (knowledgeInteractionInfo.getType()) {
			case ASK:
				binding.put("kiType", "<" + ASK_KI.toString() + ">");
				binding.put("gp", "<https://www.tno.nl/TODO1>"); // TODO
				binding.put("patternType", "<https://www.tno.nl/energy/ontology/interconnect#hasGraphPattern>");
				binding.put("pattern",
				"\"" + ((AskKnowledgeInteraction) knowledgeInteraction).getPattern().getPattern() + "\"");
				break;
			case ANSWER:
				binding.put("kiType", "<" + ANSWER_KI.toString() + ">");
				binding.put("gp", "<https://www.tno.nl/TODO2>"); // TODO
				binding.put("patternType", "<https://www.tno.nl/energy/ontology/interconnect#hasGraphPattern>");
				binding.put("pattern",
				"\"" + ((AnswerKnowledgeInteraction) knowledgeInteraction).getPattern().getPattern() + "\"");
				break;
			case POST:
				binding.put("kiType", "<" + POST_KI.toString() + ">");
				binding.put("gp", "<https://www.tno.nl/TODO3>"); // TODO
				binding.put("patternType", "<https://www.tno.nl/energy/ontology/interconnect#hasArgumentPattern>");
				binding.put("pattern",
				"\"" + ((PostKnowledgeInteraction) knowledgeInteraction).getArgument().getPattern() + "\"");
				break;
			case REACT:
				binding.put("kiType", "<" + REACT_KI.toString() + ">");
				binding.put("gp", "<https://www.tno.nl/TODO4>"); // TODO
				binding.put("patternType", "<https://www.tno.nl/energy/ontology/interconnect#hasArgumentPattern>");
				binding.put("pattern",
						"\"" + ((ReactKnowledgeInteraction) knowledgeInteraction).getArgument().getPattern() + "\"");
				break;
			default:
				this.LOG.warn("Ignored currently unsupported knowledge interaction type {}.",
						knowledgeInteractionInfo.getType());
				assert false;
			}
			bindings.add(binding);
			
			// Also add another binding for the resultPattern if we're processing a POST/REACT Knowledge Interaction.
			if (knowledgeInteractionInfo.getType() == Type.POST) {
				Binding additionalBinding = binding.clone();
				additionalBinding.put("gp", "<https://www.tno.nl/TODO5>"); // TODO
				additionalBinding.put("patternType", "<https://www.tno.nl/energy/ontology/interconnect#hasResultPattern>");
				additionalBinding.put("pattern", "\"" + ((PostKnowledgeInteraction) knowledgeInteraction).getResult().getPattern() + "\"");
				bindings.add(additionalBinding);
			} else if (knowledgeInteractionInfo.getType() == Type.REACT) {
				Binding additionalBinding = binding.clone();
				additionalBinding.put("gp", "<https://www.tno.nl/TODO6>"); // TODO
				additionalBinding.put("patternType", "<https://www.tno.nl/energy/ontology/interconnect#hasResultPattern>");
				additionalBinding.put("pattern", "\"" + ((ReactKnowledgeInteraction) knowledgeInteraction).getResult().getPattern() + "\"");
				bindings.add(additionalBinding);
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
					.thenApply(answerMsg -> {
						try {
							return this.constructOtherKnowledgeBaseFromAnswerMessage(answerMsg);
						} catch (Throwable t) {
							this.LOG.error("The construction of other knowledge base should succeed.", t);
						}
						return null;
					});
			return future;
		} catch (IOException e) {
			var failedFuture = new CompletableFuture<OtherKnowledgeBase>();
			failedFuture.completeExceptionally(e);
			return failedFuture;
		}
	}

	private URI constructAskMetaKnowledgeInteractionId(URI knowledgeBaseId) {
		try {
			return new URI(knowledgeBaseId.toString() + ASK_SUFFIX);
		} catch (URISyntaxException e) {
			this.LOG.error("", e);
		}
		return null;
	}

	private URI constructAnswerMetaKnowledgeInteractionId(URI knowledgeBaseId) {
		try {
			return new URI(knowledgeBaseId.toString() + ANSWER_SUFFIX);
		} catch (URISyntaxException e) {
			this.LOG.error("", e);
		}
		return null;
	}

	private OtherKnowledgeBase constructOtherKnowledgeBaseFromAnswerMessage(AnswerMessage answerMessage) {
		var knowledgeBaseId = answerMessage.getFromKnowledgeBase();
		var bindings = answerMessage.getBindings();

		Model model;
		try {
			model = Util.generateModel(this.metaGraphPattern, bindings);
		} catch (ParseException e) {
			this.LOG.warn("Received parse error while generating model.", e);
			assert false;
			return null;
		}
		model.setNsPrefix("kb", "https://www.tno.nl/energy/ontology/interconnect#");

		Resource kb = model.listSubjectsWithProperty(RDF.type,
				model.createResource("https://www.tno.nl/energy/ontology/interconnect#KnowledgeBase")).next();

		String name = model
				.listObjectsOfProperty(kb, model.getProperty("https://www.tno.nl/energy/ontology/interconnect#hasName"))
				.next().asLiteral().getString();
		String description = model
				.listObjectsOfProperty(kb,
						model.getProperty("https://www.tno.nl/energy/ontology/interconnect#hasDescription"))
				.next().asLiteral().getString();

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

		ExtendedIterator<Resource> kis = model
				.listObjectsOfProperty(kb,
						model.getProperty("https://www.tno.nl/energy/ontology/interconnect#hasKnowledgeInteraction"))
				.mapWith(RDFNode::asResource);

		var knowledgeInteractions = new ArrayList<KnowledgeInteractionInfo>();
		for (Resource ki : kis.toList()) {
			Resource graphPattern = model
					.listObjectsOfProperty(ki,
							model.getProperty("https://www.tno.nl/energy/ontology/interconnect#hasGraphPattern"))
					.next().asResource();
			String graphPatternString = model
					.listObjectsOfProperty(graphPattern,
							model.getProperty("https://www.tno.nl/energy/ontology/interconnect#hasPattern"))
					.next().asLiteral().getString();
			var kiType = model.listObjectsOfProperty(ki, RDF.type).next();

			try {

				if (kiType.equals(ASK_KI)) {
					AskKnowledgeInteraction askKnowledgeInteraction = new AskKnowledgeInteraction(
							new CommunicativeAct(), new GraphPattern(graphPatternString));

					KnowledgeInteractionInfo knowledgeInteractionInfo = new KnowledgeInteractionInfo(
							new URI(ki.toString()), new URI(kb.toString()), askKnowledgeInteraction);

					knowledgeInteractions.add(knowledgeInteractionInfo);
				} else if (kiType.equals(ANSWER_KI)) {
					AnswerKnowledgeInteraction answerKnowledgeInteraction = new AnswerKnowledgeInteraction(
							new CommunicativeAct(), new GraphPattern(graphPatternString));

					KnowledgeInteractionInfo knowledgeInteractionInfo = new KnowledgeInteractionInfo(
							new URI(ki.toString()), new URI(kb.toString()), answerKnowledgeInteraction);

					knowledgeInteractions.add(knowledgeInteractionInfo);
				} else {
					this.LOG.warn("Ignored unexpected knowledge interaction type " + kiType);
				}
			} catch (URISyntaxException e) {
				this.LOG.error("The URIs should be correctly formatted.", e);
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
