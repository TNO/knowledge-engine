package eu.knowledge.engine.smartconnector.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
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
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.smartconnector.impl.SmartConnectorBuilder;
import eu.knowledge.engine.smartconnector.impl.Util;

public class MockedKnowledgeBase implements KnowledgeBase, SmartConnector {

	private static final Logger LOG = LoggerFactory.getLogger(MockedKnowledgeBase.class);

	private final Set<KnowledgeInteraction> kis;
	private SmartConnector sc;
	protected String name;
	private Phaser readyPhaser;

	private CompletableFuture<Void> stoppedFuture = new CompletableFuture<Void>();

	/**
	 * Enable the reasoner. Off by default (for now).
	 */
	private boolean reasonerEnabled = false;

	public MockedKnowledgeBase(String aName) {
		this.kis = ConcurrentHashMap.newKeySet();
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
	}

	public CompletableFuture<Void> getStopFuture() {
		return this.stoppedFuture;
	}

	@Override
	public URI register(AskKnowledgeInteraction anAskKI) {
		var id = this.getSC().register(anAskKI);
		this.kis.add(anAskKI);
		return id;
	}

	@Override
	public void unregister(AskKnowledgeInteraction anAskKI) {
		this.getSC().unregister(anAskKI);
		this.kis.remove(anAskKI);
	}

	@Override
	public URI register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler aAnswerHandler) {
		var id = this.getSC().register(anAnswerKI, aAnswerHandler);
		this.kis.add(anAnswerKI);
		return id;
	}

	@Override
	public void unregister(AnswerKnowledgeInteraction anAnswerKI) {
		this.getSC().unregister(anAnswerKI);
		this.kis.remove(anAnswerKI);
	}

	@Override
	public URI register(PostKnowledgeInteraction aPostKI) {
		var id = this.getSC().register(aPostKI);
		this.kis.add(aPostKI);
		return id;
	}

	@Override
	public void unregister(PostKnowledgeInteraction aPostKI) {
		this.getSC().unregister(aPostKI);
		this.kis.remove(aPostKI);
	}

	@Override
	public URI register(ReactKnowledgeInteraction anReactKI, ReactHandler aReactHandler) {
		var id = this.getSC().register(anReactKI, aReactHandler);
		this.kis.add(anReactKI);
		return id;
	}

	@Override
	public void unregister(ReactKnowledgeInteraction anReactKI) {
		this.getSC().unregister(anReactKI);
		this.kis.remove(anReactKI);
	}

	@Override
	public CompletableFuture<AskResult> ask(AskKnowledgeInteraction anAKI, RecipientSelector aSelector,
			BindingSet aBindingSet) {

		return this.getSC().ask(anAKI, aSelector, aBindingSet);
	}

	@Override
	public CompletableFuture<AskResult> ask(AskKnowledgeInteraction ki, BindingSet bindings) {
		return this.getSC().ask(ki, bindings);
	}

	@Override
	public CompletableFuture<PostResult> post(PostKnowledgeInteraction aPKI, RecipientSelector aSelector,
			BindingSet someArguments) {
		return this.getSC().post(aPKI, aSelector, someArguments);
	}

	@Override
	public CompletableFuture<PostResult> post(PostKnowledgeInteraction ki, BindingSet argument) {
		return this.getSC().post(ki, argument);
	}

	public Set<KnowledgeInteraction> getKnowledgeInteractions() {
		return this.kis;
	}

	public boolean isUpToDate(AskKnowledgeInteraction askKnowledgeInteraction,
			Set<MockedKnowledgeBase> someKnowledgeBases) {

		boolean isUpToDate = true;

		// ask and check the result.
		try {
			LOG.trace("before ask metadata");
			AskResult result = this.sc.ask(askKnowledgeInteraction, new BindingSet()).get();
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

					Set<KnowledgeInteraction> someKis = aMockedKB.getKnowledgeInteractions();

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
							Resource gp1 = ki.getRequiredProperty(Vocab.HAS_ARG).getObject().asResource();
							String argPatternFromRDF = gp1.getRequiredProperty(Vocab.HAS_PATTERN).getLiteral()
									.getLexicalForm();
							String argPatternFromObject = convertToPattern(postKI.getArgument());

							boolean resultPatternsEqual = false;
							if (ki.hasProperty(Vocab.HAS_RES)) {
								Resource gp2 = ki.getProperty(Vocab.HAS_RES).getObject().asResource();
								String resPatternFromRDF = gp2.getRequiredProperty(Vocab.HAS_PATTERN).getLiteral()
										.getLexicalForm();
								String resPatternFromObject = convertToPattern(postKI.getResult());
								resultPatternsEqual = resPatternFromRDF.equals(resPatternFromObject);
							} else if (!ki.hasProperty(Vocab.HAS_RES) && postKI.getResult() == null) {
								resultPatternsEqual = true;
							}

							sameKI |= argPatternFromRDF.equals(argPatternFromObject) && resultPatternsEqual;

						} else if (isOfType(ki, Vocab.REACT_KI) && someKi instanceof ReactKnowledgeInteraction) {
							var reactKI = (ReactKnowledgeInteraction) someKi;
							// compare graph pattern
							Resource gp1 = ki.getRequiredProperty(Vocab.HAS_ARG).getObject().asResource();
							String argPatternFromRDF = gp1.getRequiredProperty(Vocab.HAS_PATTERN).getLiteral()
									.getLexicalForm();
							String argPatternFromObject = convertToPattern(reactKI.getArgument());

							boolean resultPatternsEqual = false;
							if (ki.hasProperty(Vocab.HAS_RES)) {
								Resource gp2 = ki.getProperty(Vocab.HAS_RES).getObject().asResource();
								String resPatternFromRDF = gp2.getRequiredProperty(Vocab.HAS_PATTERN).getLiteral()
										.getLexicalForm();
								String resPatternFromObject = convertToPattern(reactKI.getResult());
								resultPatternsEqual = resPatternFromRDF.equals(resPatternFromObject);
							} else if (!ki.hasProperty(Vocab.HAS_RES) && reactKI.getResult() == null) {
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

	public void start() {
		this.sc = SmartConnectorBuilder.newSmartConnector(this).create();
		this.sc.setReasonerEnabled(this.reasonerEnabled);
	}

	@Override
	public void setDomainKnowledge(Set<Rule> someDomainKnowledge) {
		this.sc.setDomainKnowledge(someDomainKnowledge);
	}

	@Override
	public void setReasonerEnabled(boolean aReasonerEnabled) {
		this.reasonerEnabled = aReasonerEnabled;

	}

	@Override
	public boolean isReasonerEnabled() {
		return this.reasonerEnabled;
	}

	@Override
	public AskPlan planAsk(AskKnowledgeInteraction anAKI, RecipientSelector aSelector) {
		return this.sc.planAsk(anAKI, aSelector);
	}

	@Override
	public PostPlan planPost(PostKnowledgeInteraction aPKI, RecipientSelector aSelector) {
		return this.sc.planPost(aPKI, aSelector);
	}

}
