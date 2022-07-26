package eu.knowledge.engine.smartconnector.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Phaser;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.smartconnector.impl.SmartConnectorBuilder;
import eu.knowledge.engine.smartconnector.impl.Util;

public class MockedKnowledgeBase implements KnowledgeBase {

	private static final Logger LOG = LoggerFactory.getLogger(MockedKnowledgeBase.class);

	private final Set<AskKnowledgeInteraction> registeredAskKIs;
	private final Map<AnswerKnowledgeInteraction, AnswerHandler> registeredAnswerKIs;
	private final Set<PostKnowledgeInteraction> registeredPostKIs;
	private final Map<ReactKnowledgeInteraction, ReactHandler> registeredReactKIs;

	private final Set<AskKnowledgeInteraction> currentAskKIs;
	private final Map<AnswerKnowledgeInteraction, AnswerHandler> currentAnswerKIs;
	private final Set<PostKnowledgeInteraction> currentPostKIs;
	private final Map<ReactKnowledgeInteraction, ReactHandler> currentReactKIs;

	private final Set<AskKnowledgeInteraction> unregisteredAskKIs;
	private final Set<AnswerKnowledgeInteraction> unregisteredAnswerKIs;
	private final Set<PostKnowledgeInteraction> unregisteredPostKIs;
	private final Set<ReactKnowledgeInteraction> unregisteredReactKIs;

	private Set<Rule> domainKnowledge = new HashSet<>();

	private SmartConnector sc;
	protected String name;
	private Phaser readyPhaser;

	private CompletableFuture<Void> stoppedFuture = new CompletableFuture<Void>();

	/**
	 * Enable the reasoner. Off by default (for now).
	 */
	private boolean reasonerEnabled = false;

	public MockedKnowledgeBase(String aName) {

		this.registeredAskKIs = ConcurrentHashMap.newKeySet();
		this.registeredAnswerKIs = new ConcurrentHashMap<>();
		this.registeredPostKIs = ConcurrentHashMap.newKeySet();
		this.registeredReactKIs = new ConcurrentHashMap<>();

		this.currentAskKIs = ConcurrentHashMap.newKeySet();
		this.currentAnswerKIs = new ConcurrentHashMap<>();
		this.currentPostKIs = ConcurrentHashMap.newKeySet();
		this.currentReactKIs = new ConcurrentHashMap<>();

		this.unregisteredAskKIs = ConcurrentHashMap.newKeySet();
		this.unregisteredAnswerKIs = ConcurrentHashMap.newKeySet();
		this.unregisteredPostKIs = ConcurrentHashMap.newKeySet();
		this.unregisteredReactKIs = ConcurrentHashMap.newKeySet();

		this.name = aName;
	}

	/**
	 * This method is used to synchronize all knowledge bases and their smart
	 * connectors in the knowledge network and be able to wait for everyone to be up
	 * to date.
	 * 
	 * @param aPhaser a concurrent object that allows multiple parties to wait for
	 *                each other to go through different phases.
	 */
	public void setPhaser(Phaser aReadyPhaser) {
		// this knowledge base will participate in phase 1.
		this.readyPhaser = aReadyPhaser;
		this.readyPhaser.register();
	}

	@Override
	public URI getKnowledgeBaseId() {
		URI uri = null;
		try {
			uri = new URI("https://www.tno.nl/" + this.name);
		} catch (URISyntaxException e) {
			LOG.error("Could not parse the uri.", e);
		}
		return uri;
	}

	@Override
	public String getKnowledgeBaseDescription() {
		return "description of " + this.name;
	}

	@Override
	public void smartConnectorReady(SmartConnector aSC) {
		LOG.debug(this.name + " ready");
		this.readyPhaser.arrive();
	}

	@Override
	public void smartConnectorConnectionLost(SmartConnector aSC) {
		LOG.info(this.name + " connection lost");

	}

	@Override
	public void smartConnectorConnectionRestored(SmartConnector aSC) {
		LOG.info(this.name + " connection restored");
	}

	@Override
	public void smartConnectorStopped(SmartConnector aSC) {
		LOG.info(this.name + " smartconnnector stopped");
		this.stoppedFuture.complete(null);
	}

	@Override
	public String getKnowledgeBaseName() {
		return this.name;
	}

	private SmartConnector getSC() {
		return this.sc;
	}

	public void stop() {
		this.sc.stop();
		// remove all KIs
		this.unregisteredAskKIs.clear();
		this.unregisteredAnswerKIs.clear();
		this.currentPostKIs.clear();
		this.currentReactKIs.clear();
	}

	public CompletableFuture<Void> getStopFuture() {
		return this.stoppedFuture;
	}

	public void register(AskKnowledgeInteraction anAskKI) {
		this.registeredAskKIs.add(anAskKI);
	}

	public void unregister(AskKnowledgeInteraction anAskKI) {
		boolean removed = this.currentAskKIs.remove(anAskKI);
		assert removed;
		this.unregisteredAskKIs.add(anAskKI);
	}

	public void register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler aAnswerHandler) {
		this.registeredAnswerKIs.put(anAnswerKI, aAnswerHandler);
	}

	public void unregister(AnswerKnowledgeInteraction anAnswerKI) {
		assert this.currentAnswerKIs.containsKey(anAnswerKI);
		this.currentAnswerKIs.remove(anAnswerKI);
		this.unregisteredAnswerKIs.add(anAnswerKI);
	}

	public void register(PostKnowledgeInteraction aPostKI) {
		this.registeredPostKIs.add(aPostKI);
	}

	public void unregister(PostKnowledgeInteraction aPostKI) {
		boolean removed = this.currentPostKIs.remove(aPostKI);
		assert removed;
		this.unregisteredPostKIs.add(aPostKI);
	}

	public void register(ReactKnowledgeInteraction anReactKI, ReactHandler aReactHandler) {
		this.registeredReactKIs.put(anReactKI, aReactHandler);
	}

	public void unregister(ReactKnowledgeInteraction anReactKI) {
		assert this.currentReactKIs.containsKey(anReactKI);
		this.currentReactKIs.remove(anReactKI);
		this.unregisteredReactKIs.add(anReactKI);
	}

	public CompletableFuture<AskResult> ask(AskKnowledgeInteraction anAKI, RecipientSelector aSelector,
			BindingSet aBindingSet) {
		return this.getSC().ask(anAKI, aSelector, aBindingSet);
	}

	public CompletableFuture<AskResult> ask(AskKnowledgeInteraction ki, BindingSet bindings) {
		return this.getSC().ask(ki, bindings);
	}

	public CompletableFuture<PostResult> post(PostKnowledgeInteraction aPKI, RecipientSelector aSelector,
			BindingSet someArguments) {
		return this.getSC().post(aPKI, aSelector, someArguments);
	}

	public CompletableFuture<PostResult> post(PostKnowledgeInteraction ki, BindingSet argument) {
		return this.getSC().post(ki, argument);
	}

	public Set<AskKnowledgeInteraction> getAskKnowledgeInteractions() {
		Set<AskKnowledgeInteraction> all = new HashSet<>();
		all.addAll(currentAskKIs);
		all.addAll(registeredAskKIs);
		return all;
	}

	public Map<AnswerKnowledgeInteraction, AnswerHandler> getAnswerKnowledgeInteractions() {
		Map<AnswerKnowledgeInteraction, AnswerHandler> all = new HashMap<>();
		all.putAll(currentAnswerKIs);
		all.putAll(registeredAnswerKIs);

		return all;
	}

	public Set<PostKnowledgeInteraction> getPostKnowledgeInteractions() {
		Set<PostKnowledgeInteraction> all = new HashSet<>();
		all.addAll(currentPostKIs);
		all.addAll(registeredPostKIs);
		return all;
	}

	public Map<ReactKnowledgeInteraction, ReactHandler> getReactKnowledgeInteractions() {
		Map<ReactKnowledgeInteraction, ReactHandler> all = new HashMap<>();
		all.putAll(currentReactKIs);
		all.putAll(registeredReactKIs);

		return all;
	}

	public boolean isUpToDate(AskKnowledgeInteraction askKnowledgeInteraction,
			Set<MockedKnowledgeBase> someKnowledgeBases) {

		boolean isUpToDate = true;

		// ask and check the result.
		try {
			LOG.trace("before ask metadata");
			AskResult result = this.getSC().ask(askKnowledgeInteraction, new BindingSet()).get();
			LOG.trace("after ask metadata");
			Model m = Util.generateModel(askKnowledgeInteraction.getPattern(), result.getBindings());

//			System.out.println("----------" + this.getKnowledgeBaseName() + "-------------");
//			m.write(System.out, "turtle");
//			System.out.println("-----------------------");

			for (MockedKnowledgeBase aKnowledgeBase : someKnowledgeBases) {
				if (!this.getKnowledgeBaseId().toString().equals(aKnowledgeBase.getKnowledgeBaseId().toString())) {
					isUpToDate &= isKnowledgeBaseUpToDate(aKnowledgeBase, m);
				}
			}

		} catch (InterruptedException | ExecutionException | ParseException e) {
			LOG.error("{}", e);
		}

		return isUpToDate;

	}

	private boolean isKnowledgeBaseUpToDate(MockedKnowledgeBase aMockedKB, Model aModel) {

		boolean isSame = true;
		Resource kb = ResourceFactory.createResource(aMockedKB.getKnowledgeBaseId().toString());

		isSame &= aModel.contains(kb, RDF.type, Vocab.KNOWLEDGE_BASE);

		if (isSame) {
			kb = aModel.createResource(aMockedKB.getKnowledgeBaseId().toString());

			isSame &= kb.getRequiredProperty(Vocab.HAS_NAME).getLiteral().getString()
					.equals(aMockedKB.getKnowledgeBaseName());

			isSame &= kb.getRequiredProperty(Vocab.HAS_DESCR).getLiteral().getString()
					.equals(aMockedKB.getKnowledgeBaseDescription());

			var kiIter = kb.listProperties(Vocab.HAS_KI);
			while (isSame && kiIter.hasNext()) {
				Statement s = kiIter.next();
				Resource ki = s.getObject().asResource();

				if (!ki.getRequiredProperty(Vocab.IS_META).getLiteral().getBoolean()) {

					Set<KnowledgeInteraction> someKis = new HashSet<>();
					someKis.addAll(aMockedKB.getAskKnowledgeInteractions());
					someKis.addAll(aMockedKB.getAnswerKnowledgeInteractions().keySet());
					someKis.addAll(aMockedKB.getPostKnowledgeInteractions());
					someKis.addAll(aMockedKB.getReactKnowledgeInteractions().keySet());

					boolean sameKI = false;
					for (KnowledgeInteraction someKi : someKis) {

						if (isOfType(ki, Vocab.ASK_KI) && someKi instanceof AskKnowledgeInteraction) {
							var askKI = (AskKnowledgeInteraction) someKi;
							// compare graph pattern

							Resource gp = ki.getRequiredProperty(Vocab.HAS_GP).getObject().asResource();

							String patternFromRDF = gp.getRequiredProperty(Vocab.HAS_PATTERN).getLiteral()
									.getLexicalForm();
							String patternFromObject = convertToPattern(askKI.getPattern());
							sameKI |= patternFromRDF.equals(patternFromObject);

						} else if (isOfType(ki, Vocab.ANSWER_KI) && someKi instanceof AnswerKnowledgeInteraction) {
							var answerKI = (AnswerKnowledgeInteraction) someKi;
							// compare graph pattern
							Resource gp = ki.getRequiredProperty(Vocab.HAS_GP).getObject().asResource();
							String patternFromRDF = gp.getRequiredProperty(Vocab.HAS_PATTERN).getLiteral()
									.getLexicalForm();
							String patternFromObject = convertToPattern(answerKI.getPattern());
							sameKI |= patternFromRDF.equals(patternFromObject);

						} else if (isOfType(ki, Vocab.POST_KI) && someKi instanceof PostKnowledgeInteraction) {
							var postKI = (PostKnowledgeInteraction) someKi;
							// compare graph pattern
							ExtendedIterator<Resource> graphPatternIterator = ki.listProperties(Vocab.HAS_GP)
									.mapWith(stmt -> stmt.getObject().asResource());
							String argPatternFromRDF = null;
							String resPatternFromRDF = null;
							while (graphPatternIterator.hasNext()) {
								Resource graphPattern = graphPatternIterator.next();
								Resource gpType = graphPattern.getPropertyResourceValue(RDF.type);
								if (gpType.equals(Vocab.ARGUMENT_GRAPH_PATTERN)) {
									if (argPatternFromRDF != null) {
										throw new IllegalArgumentException(
												"Knowledge interaction cannot have multiple argument patterns.");
									}
									argPatternFromRDF = graphPattern.getProperty(Vocab.HAS_PATTERN).getString();
								} else if (gpType.equals(Vocab.RESULT_GRAPH_PATTERN)) {
									if (resPatternFromRDF != null) {
										throw new IllegalArgumentException(
												"Knowledge interaction cannot have multiple result patterns.");
									}
									resPatternFromRDF = graphPattern.getProperty(Vocab.HAS_PATTERN).getString();
								} else {
									throw new IllegalArgumentException(String.format(
											"For a POST/REACT Knowledge Interaction, their graph pattern must be either %s or %s. Not %s.",
											Vocab.ARGUMENT_GRAPH_PATTERN, Vocab.RESULT_GRAPH_PATTERN, gpType));
								}
							}
							String argPatternFromObject = convertToPattern(postKI.getArgument());

							boolean resultPatternsEqual = false;
							if (resPatternFromRDF != null) {
								String resPatternFromObject = convertToPattern(postKI.getResult());
								resultPatternsEqual = resPatternFromRDF.equals(resPatternFromObject);
							} else if (resPatternFromRDF == null && postKI.getResult() == null) {
								resultPatternsEqual = true;
							}

							sameKI |= argPatternFromRDF.equals(argPatternFromObject) && resultPatternsEqual;

						} else if (isOfType(ki, Vocab.REACT_KI) && someKi instanceof ReactKnowledgeInteraction) {
							var reactKI = (ReactKnowledgeInteraction) someKi;
							// compare graph pattern
							ExtendedIterator<Resource> graphPatternIterator = ki.listProperties(Vocab.HAS_GP)
									.mapWith(stmt -> stmt.getObject().asResource());
							String argPatternFromRDF = null;
							String resPatternFromRDF = null;
							while (graphPatternIterator.hasNext()) {
								Resource graphPattern = graphPatternIterator.next();
								Resource gpType = graphPattern.getPropertyResourceValue(RDF.type);
								if (gpType.equals(Vocab.ARGUMENT_GRAPH_PATTERN)) {
									if (argPatternFromRDF != null) {
										throw new IllegalArgumentException(
												"Knowledge interaction cannot have multiple argument patterns.");
									}
									argPatternFromRDF = graphPattern.getProperty(Vocab.HAS_PATTERN).getString();
								} else if (gpType.equals(Vocab.RESULT_GRAPH_PATTERN)) {
									if (resPatternFromRDF != null) {
										throw new IllegalArgumentException(
												"Knowledge interaction cannot have multiple result patterns.");
									}
									resPatternFromRDF = graphPattern.getProperty(Vocab.HAS_PATTERN).getString();
								} else {
									throw new IllegalArgumentException(String.format(
											"For a POST/REACT Knowledge Interaction, their graph pattern must be either %s or %s. Not %s.",
											Vocab.ARGUMENT_GRAPH_PATTERN, Vocab.RESULT_GRAPH_PATTERN, gpType));
								}
							}
							String argPatternFromObject = convertToPattern(reactKI.getArgument());

							boolean resultPatternsEqual = false;
							if (resPatternFromRDF != null) {
								String resPatternFromObject = convertToPattern(reactKI.getResult());
								resultPatternsEqual = resPatternFromRDF.equals(resPatternFromObject);
							} else if (resPatternFromRDF == null && reactKI.getResult() == null) {
								resultPatternsEqual = true;
							}

							sameKI |= argPatternFromRDF.equals(argPatternFromObject) && resultPatternsEqual;
						}
					}
					isSame &= sameKI;
				}

			}
		}

		return isSame;

	}

	private boolean isOfType(Resource aResource, Resource aType) {
		var typeIter = aResource.listProperties(RDF.type);
		boolean foundType = false;
		while (typeIter.hasNext()) {
			var t = typeIter.next();
			foundType |= t.getObject().equals(aType);
		}
		return foundType;
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

	/**
	 * Start this KB if it was not already started.
	 */
	public void start() {
		if (!isStarted()) {
			this.sc = SmartConnectorBuilder.newSmartConnector(this).create();
			this.sc.setReasonerEnabled(this.reasonerEnabled);
		}
	}

	/**
	 * Registers all KIs that have not yet already been registered.
	 */
	public void syncKIs() {
		if (!this.isStarted())
			throw new IllegalStateException("The KB should be started before registering KIs.");

		for (var ki : this.registeredAskKIs) {
			this.getSC().register(ki);
			this.currentAskKIs.add(ki);
		}
		this.registeredAskKIs.clear();

		for (var entry : this.registeredAnswerKIs.entrySet()) {
			this.getSC().register(entry.getKey(), entry.getValue());
			this.currentAnswerKIs.put(entry.getKey(), entry.getValue());
		}
		this.registeredAnswerKIs.clear();

		for (var ki : this.registeredPostKIs) {
			this.getSC().register(ki);
			this.currentPostKIs.add(ki);
		}
		this.registeredPostKIs.clear();

		for (var entry : this.registeredReactKIs.entrySet()) {
			this.getSC().register(entry.getKey(), entry.getValue());
			this.currentReactKIs.put(entry.getKey(), entry.getValue());
		}
		this.registeredReactKIs.clear();

		for (var ki : this.unregisteredAskKIs) {
			this.getSC().unregister(ki);
			this.currentAskKIs.remove(ki);
		}
		this.unregisteredAskKIs.clear();

		for (var ki : this.unregisteredAnswerKIs) {
			this.getSC().unregister(ki);
			this.currentAnswerKIs.remove(ki);
		}
		this.unregisteredAnswerKIs.clear();

		for (var ki : this.unregisteredPostKIs) {
			this.getSC().unregister(ki);
			this.currentPostKIs.remove(ki);
		}
		this.unregisteredPostKIs.clear();

		for (var ki : this.unregisteredReactKIs) {
			this.getSC().unregister(ki);
			this.currentReactKIs.remove(ki);
		}
		this.unregisteredReactKIs.clear();

		this.getSC().setDomainKnowledge(this.domainKnowledge);

	}

	public void setDomainKnowledge(Set<Rule> someDomainKnowledge) {
		this.domainKnowledge = someDomainKnowledge;
	}

	public void setReasonerEnabled(boolean aReasonerEnabled) {
		this.reasonerEnabled = aReasonerEnabled;

	}

	public boolean isReasonerEnabled() {
		return this.reasonerEnabled;
	}

	public AskPlan planAsk(AskKnowledgeInteraction anAKI, RecipientSelector aSelector) {
		return this.getSC().planAsk(anAKI, aSelector);
	}

	public PostPlan planPost(PostKnowledgeInteraction aPKI, RecipientSelector aSelector) {
		return this.getSC().planPost(aPKI, aSelector);
	}

	public boolean isStarted() {
		return this.getSC() != null;
	}
}
