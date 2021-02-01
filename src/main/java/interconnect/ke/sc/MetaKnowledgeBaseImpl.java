package interconnect.ke.sc;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.util.FmtUtils;
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

	private final URI myKnowledgeBaseId;
	private final MessageRouter messageRouter;
	private final GraphPattern metaGraphPattern;
	private final KnowledgeBaseStore knowledgeBaseStore;

	public MetaKnowledgeBaseImpl(LoggerProvider loggerProvider, MessageRouter aMessageRouter,
			KnowledgeBaseStore aKnowledgeBaseStore) {
		this.LOG = loggerProvider.getLogger(this.getClass());

		this.myKnowledgeBaseId = aKnowledgeBaseStore.getKnowledgeBaseId();
		this.messageRouter = aMessageRouter;
		this.knowledgeBaseStore = aKnowledgeBaseStore;

		var prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		this.metaGraphPattern = new GraphPattern(prefixes,
				"?kb rdf:type kb:KnowledgeBase . ?kb kb:hasName ?name . ?kb kb:hasDescription ?description . ?kb kb:hasKnowledgeInteraction ?ki . ?ki rdf:type ?kiType . ?ki kb:isMeta ?isMeta . ?ki kb:hasGraphPattern ?gp . ?ki ?patternType ?gp . ?gp rdf:type kb:GraphPattern . ?gp kb:hasPattern ?pattern .");

		// create answer knowledge interaction
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), this.metaGraphPattern);
		this.knowledgeBaseStore.register(aKI, (anAKI, aBindingSet) -> this.fillMetaBindings(), true);

		// create ask knowledge interaction
		AskKnowledgeInteraction aKI2 = new AskKnowledgeInteraction(new CommunicativeAct(), this.metaGraphPattern);
		this.knowledgeBaseStore.register(aKI2, true);
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
		BindingSet bindings = new BindingSet();

		for (KnowledgeInteractionInfo knowledgeInteractionInfo : this.knowledgeBaseStore.getKnowledgeInteractions()) {

			this.LOG.trace("Creating meta info for: {}", knowledgeInteractionInfo);

			Binding binding = new Binding();
			binding.put("kb", bracketize(this.myKnowledgeBaseId.toString()));
			binding.put("name", quotize(this.knowledgeBaseStore.getKnowledgeBaseName()));
			binding.put("description", quotize(this.knowledgeBaseStore.getKnowledgeBaseDescription()));
			// binding.put("sc", "TODO"); // TODO
			// binding.put("endpoint", "TODO"); // TODO
			binding.put("ki", bracketize(knowledgeInteractionInfo.getId().toString()));
			binding.put("isMeta", Boolean.toString(knowledgeInteractionInfo.isMeta()));
			var knowledgeInteraction = knowledgeInteractionInfo.getKnowledgeInteraction();

			switch (knowledgeInteractionInfo.getType()) {
			case ASK:
				binding.put("kiType", bracketize(Vocab.ASK_KI.toString()));
				binding.put("gp", bracketize(knowledgeInteractionInfo.getId() + "/gp"));
				binding.put("patternType", bracketize(Vocab.HAS_GP.toString()));
				binding.put("pattern",
						quotize(this.convertToPattern(((AskKnowledgeInteraction) knowledgeInteraction).getPattern())));
				break;
			case ANSWER:
				binding.put("kiType", bracketize(Vocab.ANSWER_KI.toString()));
				binding.put("gp", bracketize(knowledgeInteractionInfo.getId() + "/gp"));
				binding.put("patternType", bracketize(Vocab.HAS_GP.toString()));
				binding.put("pattern", quotize(
						this.convertToPattern(((AnswerKnowledgeInteraction) knowledgeInteraction).getPattern())));
				break;
			case POST:
				binding.put("kiType", bracketize(Vocab.POST_KI.toString()));
				binding.put("gp", bracketize(knowledgeInteractionInfo.getId() + "/argumentgp"));
				binding.put("patternType", bracketize(Vocab.HAS_ARG.toString()));
				binding.put("pattern", quotize(
						this.convertToPattern(((PostKnowledgeInteraction) knowledgeInteraction).getArgument())));
				break;
			case REACT:
				binding.put("kiType", bracketize(Vocab.REACT_KI.toString()));
				binding.put("gp", bracketize(knowledgeInteractionInfo.getId() + "/argumentgp"));
				binding.put("patternType", bracketize(Vocab.HAS_ARG.toString()));
				binding.put("pattern", quotize(
						this.convertToPattern(((ReactKnowledgeInteraction) knowledgeInteraction).getArgument())));
				break;
			default:
				this.LOG.warn("Ignored currently unsupported knowledge interaction type {}.",
						knowledgeInteractionInfo.getType());
				assert false;
			}
			bindings.add(binding);

			// Also add another binding for the resultPattern if we're processing a
			// POST/REACT Knowledge Interaction and there actually is a result graph
			// pattern.
			if (knowledgeInteractionInfo.getType() == Type.POST
					&& ((PostKnowledgeInteraction) knowledgeInteraction).getResult() != null) {
				Binding additionalBinding = binding.clone();
				additionalBinding.put("gp", bracketize(knowledgeInteractionInfo.getId() + "/resultgp")); // TODO
				additionalBinding.put("patternType", bracketize(Vocab.HAS_RES.toString()));

				this.LOG.trace("{}", ((PostKnowledgeInteraction) knowledgeInteraction).getResult());

				additionalBinding.put("pattern",
						quotize(((PostKnowledgeInteraction) knowledgeInteraction).getResult().getPattern()));
				bindings.add(additionalBinding);
			} else if (knowledgeInteractionInfo.getType() == Type.REACT
					&& ((ReactKnowledgeInteraction) knowledgeInteraction).getResult() != null) {
				Binding additionalBinding = binding.clone();
				additionalBinding.put("gp", quotize(knowledgeInteractionInfo.getId() + "/resultgp")); // TODO
				additionalBinding.put("patternType", quotize(Vocab.HAS_RES.toString()));
				additionalBinding.put("pattern",
						quotize(((ReactKnowledgeInteraction) knowledgeInteraction).getResult().getPattern()));
				bindings.add(additionalBinding);
			}
		}
		return bindings;
	}

	private String convertToPattern(GraphPattern gp) {

		try {

			Iterator<TriplePath> iter = gp.getGraphPattern().patternElts();

			StringBuilder sb = new StringBuilder();

			while (iter.hasNext()) {

				TriplePath tp = iter.next();
				sb.append(FmtUtils.stringForTriple(tp.asTriple(), new PrefixMappingMem()));
				sb.append(" . ");
			}

			return sb.toString();
		} catch (ParseException pe) {
			this.LOG.error("The graph pattern should be parseable.", pe);
		}
		return "<errorgraphpattern>";
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

		var toKnowledgeInteraction = this.knowledgeBaseStore.getMetaId(toKnowledgeBaseId,
				KnowledgeInteractionInfo.Type.ANSWER);
		var fromKnowledgeInteraction = this.knowledgeBaseStore.getMetaId(this.myKnowledgeBaseId,
				KnowledgeInteractionInfo.Type.ASK);
		var askMsg = new AskMessage(this.myKnowledgeBaseId, fromKnowledgeInteraction, toKnowledgeBaseId,
				toKnowledgeInteraction, new BindingSet());
		try {
			CompletableFuture<OtherKnowledgeBase> future = this.messageRouter.sendAskMessage(askMsg)
					.thenApply(answerMsg -> {
						try {
							return this.constructOtherKnowledgeBaseFromAnswerMessage(answerMsg);
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

	private OtherKnowledgeBase constructOtherKnowledgeBaseFromAnswerMessage(AnswerMessage answerMessage) {
		var knowledgeBaseId = answerMessage.getFromKnowledgeBase();
		var bindings = answerMessage.getBindings();
		this.LOG.trace("Received message: {}", answerMessage);

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
			try {

				if (kiType.equals(Vocab.ASK_KI) || kiType.equals(Vocab.ANSWER_KI)) {
					Resource graphPattern = model.listObjectsOfProperty(ki, Vocab.HAS_GP).next().asResource();
					String graphPatternString = model.listObjectsOfProperty(graphPattern, Vocab.HAS_PATTERN).next()
							.asLiteral().getString();
					if (kiType.equals(Vocab.ASK_KI)) {
						AskKnowledgeInteraction askKnowledgeInteraction = new AskKnowledgeInteraction(
								new CommunicativeAct(), new GraphPattern(graphPatternString));

						KnowledgeInteractionInfo knowledgeInteractionInfo = new KnowledgeInteractionInfo(
								new URI(ki.toString()), new URI(kb.toString()), askKnowledgeInteraction, isMeta);

						knowledgeInteractions.add(knowledgeInteractionInfo);
					} else if (kiType.equals(Vocab.ANSWER_KI)) {
						AnswerKnowledgeInteraction answerKnowledgeInteraction = new AnswerKnowledgeInteraction(
								new CommunicativeAct(), new GraphPattern(graphPatternString));

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

						PostKnowledgeInteraction postKnowledgeInteraction = new PostKnowledgeInteraction(
								new CommunicativeAct(),
								(argumentGraphPatternString != null ? new GraphPattern(argumentGraphPatternString)
										: null),
								(resultGraphPatternString != null ? new GraphPattern(resultGraphPatternString) : null));
						KnowledgeInteractionInfo knowledgeInteractionInfo = new KnowledgeInteractionInfo(
								new URI(ki.toString()), new URI(kb.toString()), postKnowledgeInteraction, isMeta);
						knowledgeInteractions.add(knowledgeInteractionInfo);
					} else if (kiType.equals(Vocab.REACT_KI)) {
						ReactKnowledgeInteraction reactKnowledgeInteraction = new ReactKnowledgeInteraction(
								new CommunicativeAct(),
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

		return new OtherKnowledgeBase(knowledgeBaseId, name, description, knowledgeInteractions, null);

	}

	@Override
	public boolean isMetaKnowledgeInteraction(URI id) {
		return this.knowledgeBaseStore.getKnowledgeInteractionById(id).isMeta();
	}

	private static String bracketize(String anUri) {
		return "<" + anUri + ">";
	}

	private static String quotize(String aValue) {
		return "\"" + aValue + "\"";
	}
}
