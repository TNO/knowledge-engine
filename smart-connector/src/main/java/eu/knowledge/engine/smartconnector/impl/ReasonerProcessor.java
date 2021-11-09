package eu.knowledge.engine.smartconnector.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.util.FmtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BindingSetHandler;
import eu.knowledge.engine.reasoner.KeReasoner;
import eu.knowledge.engine.reasoner.ReasoningNode;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.TaskBoard;
import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TriplePattern.Literal;
import eu.knowledge.engine.reasoner.api.TriplePattern.Variable;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskExchangeInfo;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostExchangeInfo;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostResult;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.ExchangeInfo.Initiator;
import eu.knowledge.engine.smartconnector.api.ExchangeInfo.Status;
import eu.knowledge.engine.smartconnector.impl.KnowledgeInteractionInfo.Type;
import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.messaging.PostMessage;
import eu.knowledge.engine.smartconnector.messaging.ReactMessage;

public class ReasonerProcessor extends SingleInteractionProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(ReasonerProcessor.class);

	private KeReasoner reasoner;
	private MyKnowledgeInteractionInfo myKnowledgeInteraction;
	private final Set<AskExchangeInfo> askExchangeInfos;
	private final Set<PostExchangeInfo> postExchangeInfos;
	private Instant previousSend = null;
	private Set<Rule> additionalDomainKnowledge;

	public ReasonerProcessor(Set<KnowledgeInteractionInfo> knowledgeInteractions, MessageRouter messageRouter,
			Set<Rule> someDomainKnowledge) {
		super(knowledgeInteractions, messageRouter);

		this.additionalDomainKnowledge = someDomainKnowledge;

		reasoner = new KeReasoner();

		for (Rule r : this.additionalDomainKnowledge) {
			reasoner.addRule(r);
		}

		this.askExchangeInfos = new HashSet<>();
		this.postExchangeInfos = new HashSet<>();

		for (KnowledgeInteractionInfo kii : knowledgeInteractions) {
			KnowledgeInteraction ki = kii.getKnowledgeInteraction();
			if (kii.getType().equals(Type.ANSWER)) {
				AnswerKnowledgeInteraction aki = (AnswerKnowledgeInteraction) ki;
				GraphPattern gp = aki.getPattern();
				reasoner.addRule(new Rule(new HashSet<>(), new HashSet<>(translateGraphPatternTo(gp)),
						new OtherKnowledgeBaseBindingSetHandler(kii.getKnowledgeBaseId().toString()) {

							@Override
							public eu.knowledge.engine.reasoner.api.BindingSet handle(
									eu.knowledge.engine.reasoner.api.BindingSet bs) {

								BindingSet newBS = translateBindingSetFrom(bs);

								AskMessage askMessage = new AskMessage(
										ReasonerProcessor.this.myKnowledgeInteraction.getKnowledgeBaseId(),
										ReasonerProcessor.this.myKnowledgeInteraction.getId(), kii.getKnowledgeBaseId(),
										kii.getId(), newBS);

								CompletableFuture<AnswerMessage> sendAskMessage = new CompletableFuture<AnswerMessage>();
								try {
									sendAskMessage = ReasonerProcessor.this.messageRouter.sendAskMessage(askMessage);
									ReasonerProcessor.this.previousSend = Instant.now();
									AnswerMessage answerMessage = sendAskMessage.get();

									ReasonerProcessor.this.askExchangeInfos.add(
											convertMessageToExchangeInfo(answerMessage.getBindings(), answerMessage));

									eu.knowledge.engine.reasoner.api.BindingSet translateBindingSetTo = translateBindingSetTo(
											answerMessage.getBindings());

									return translateBindingSetTo;
								} catch (IOException | InterruptedException | ExecutionException e) {
									LOG.error("No errors should occur while sending an AskMessage.", e);
									sendAskMessage.completeExceptionally(e);
								}
								return new eu.knowledge.engine.reasoner.api.BindingSet();
							}

						}));
			} else if (kii.getType().equals(Type.REACT)) {
				ReactKnowledgeInteraction rki = (ReactKnowledgeInteraction) ki;
				GraphPattern argGp = rki.getArgument();
				GraphPattern resGp = rki.getResult();

				Set<TriplePattern> resPattern;
				if (resGp == null) {
					resPattern = new HashSet<>();
				} else {
					resPattern = new HashSet<>(translateGraphPatternTo(resGp));
				}

				reasoner.addRule(new Rule(translateGraphPatternTo(argGp), resPattern,
						new OtherKnowledgeBaseBindingSetHandler(kii.getKnowledgeBaseId().toString()) {

							@Override
							public eu.knowledge.engine.reasoner.api.BindingSet handle(
									eu.knowledge.engine.reasoner.api.BindingSet bs) {

								BindingSet newBS = translateBindingSetFrom(bs);

								PostMessage postMessage = new PostMessage(
										ReasonerProcessor.this.myKnowledgeInteraction.getKnowledgeBaseId(),
										ReasonerProcessor.this.myKnowledgeInteraction.getId(), kii.getKnowledgeBaseId(),
										kii.getId(), newBS);

								CompletableFuture<ReactMessage> sendPostMessage = new CompletableFuture<ReactMessage>();
								try {
									sendPostMessage = ReasonerProcessor.this.messageRouter.sendPostMessage(postMessage);
									ReasonerProcessor.this.previousSend = Instant.now();
									ReactMessage reactMessage = sendPostMessage.get();

									BindingSet resultBindingSet = reactMessage.getResult();

									if (resultBindingSet == null)
										resultBindingSet = new BindingSet();

									ReasonerProcessor.this.postExchangeInfos.add(
											convertMessageToExchangeInfo(newBS, reactMessage.getResult(), reactMessage));

									return translateBindingSetTo(resultBindingSet);
								} catch (IOException | InterruptedException | ExecutionException e) {
									LOG.error("No errors should occur while sending an PostMessage.", e);
									sendPostMessage.completeExceptionally(e);
								}
								return new eu.knowledge.engine.reasoner.api.BindingSet();
							}

						}));
			}

		}

	}

	@Override
	public CompletableFuture<AskResult> processAskInteraction(MyKnowledgeInteractionInfo aAKI,
			BindingSet someBindings) {
		KnowledgeInteraction ki = aAKI.getKnowledgeInteraction();

		this.myKnowledgeInteraction = aAKI;

		eu.knowledge.engine.reasoner.api.BindingSet bs = null;
		if (aAKI.getType().equals(Type.ASK)) {
			AskKnowledgeInteraction aki = (AskKnowledgeInteraction) ki;

			TaskBoard taskboard = new TaskBoard();

			ReasoningNode node = this.reasoner.backwardPlan(translateGraphPatternTo(aki.getPattern()),
					ki.fullMatchOnly() ? MatchStrategy.FIND_ONLY_FULL_MATCHES : MatchStrategy.FIND_ONLY_BIGGEST_MATCHES,
					taskboard);

			while ((bs = node.continueBackward(translateBindingSetTo(someBindings))) == null) {
				LOG.error("\n{}", node);
				taskboard.executeScheduledTasks();
			}

		} else {
			LOG.info("Type should be Ask, not {}", aAKI.getType());
		}

		CompletableFuture<AskResult> result = new CompletableFuture<AskResult>();
		result.complete(new AskResult(translateBindingSetFrom(bs), this.askExchangeInfos));
		return result;
	}

	/**
	 * Translate bindingset from the reasoner bindingsets to the ke bindingsets.
	 * 
	 * @param bs a reasoner bindingset
	 * @return a ke bindingset
	 */
	private BindingSet translateBindingSetFrom(eu.knowledge.engine.reasoner.api.BindingSet bs) {
		BindingSet newBS = new BindingSet();
		Binding newB;
		for (eu.knowledge.engine.reasoner.api.Binding b : bs) {
			newB = new Binding();
			for (Map.Entry<Variable, Literal> entry : b.entrySet()) {

				String value = entry.getValue().getValue();
				if (entry.getValue().getValue().startsWith("https:")) {
					value = "<" + value + ">";
				}

				newB.put(entry.getKey().getVariableName().substring(1), value);
			}
			newBS.add(newB);
		}
		return newBS;
	}

	/**
	 * Translate bindingset from the ke bindingset to the reasoner bindingset.
	 * 
	 * @param bs a ke bindingset
	 * @return a reasoner bindingset
	 */
	private eu.knowledge.engine.reasoner.api.BindingSet translateBindingSetTo(BindingSet someBindings) {

		eu.knowledge.engine.reasoner.api.BindingSet newBindingSet = new eu.knowledge.engine.reasoner.api.BindingSet();
		eu.knowledge.engine.reasoner.api.Binding newBinding;
		for (Binding b : someBindings) {

			newBinding = new eu.knowledge.engine.reasoner.api.Binding();
			for (String var : b.getVariables()) {
				newBinding.put("?" + var, b.get(var));
			}
			newBindingSet.add(newBinding);
		}

		return newBindingSet;
	}

	private Set<TriplePattern> translateGraphPatternTo(GraphPattern pattern) {

		TriplePattern tp;
		TriplePath triplePath;
		String triple;
		ElementPathBlock epb = pattern.getGraphPattern();
		Iterator<TriplePath> iter = epb.patternElts();

		Set<TriplePattern> triplePatterns = new HashSet<TriplePattern>();

		while (iter.hasNext()) {

			triplePath = iter.next();

			triple = FmtUtils.stringForTriple(triplePath.asTriple(), new PrefixMappingZero());

			tp = new TriplePattern(triple);
			triplePatterns.add(tp);
		}

		return triplePatterns;
	}

	public static class CaptureBindingSetHandler implements BindingSetHandler {

		private eu.knowledge.engine.reasoner.api.BindingSet bs;

		@Override
		public eu.knowledge.engine.reasoner.api.BindingSet handle(
				eu.knowledge.engine.reasoner.api.BindingSet bs) {

			this.bs = bs;

			return new eu.knowledge.engine.reasoner.api.BindingSet();
		}

		public eu.knowledge.engine.reasoner.api.BindingSet getBindingSet() {
			return bs;
		}

	}

	@Override
	public CompletableFuture<PostResult> processPostInteraction(MyKnowledgeInteractionInfo aPKI,
			BindingSet someBindings) {
		KnowledgeInteraction ki = aPKI.getKnowledgeInteraction();

		this.myKnowledgeInteraction = aPKI;

		BindingSet resultBS = new BindingSet();

		if (aPKI.getType().equals(Type.POST)) {

			PostKnowledgeInteraction pki = (PostKnowledgeInteraction) ki;

			CaptureBindingSetHandler aBindingSetHandler = null;
			if (pki.getResult() != null) {
				aBindingSetHandler = new CaptureBindingSetHandler();
				reasoner.addRule(
						new Rule(translateGraphPatternTo(pki.getResult()), new HashSet<>(), aBindingSetHandler));
			}

			TaskBoard taskboard = new TaskBoard();

			ReasoningNode node = this.reasoner.forwardPlan(translateGraphPatternTo(pki.getArgument()),
					pki.fullMatchOnly() ? MatchStrategy.FIND_ONLY_FULL_MATCHES
							: MatchStrategy.FIND_ONLY_BIGGEST_MATCHES,
					taskboard);

			while (!node.continueForward(translateBindingSetTo(someBindings))) {

				LOG.error("\n{}", node);
				taskboard.executeScheduledTasks();
			}

			if (aBindingSetHandler != null) {
				resultBS = translateBindingSetFrom(aBindingSetHandler.getBindingSet());
			}

		} else {
			LOG.info("Type should be Post, not {}", aPKI.getType());
		}

		CompletableFuture<PostResult> result = new CompletableFuture<>();
		result.complete(new PostResult(resultBS, this.postExchangeInfos));
		return result;
	}

	public static abstract class OtherKnowledgeBaseBindingSetHandler implements BindingSetHandler {

		private String kbName;

		public OtherKnowledgeBaseBindingSetHandler(String aName) {
			this.kbName = aName;
		}

		public String getKnowledgeBaseName() {
			return this.kbName;
		}

		@Override
		public abstract eu.knowledge.engine.reasoner.api.BindingSet handle(
				eu.knowledge.engine.reasoner.api.BindingSet bs);

	}

	private AskExchangeInfo convertMessageToExchangeInfo(BindingSet someConvertedBindings, AnswerMessage aMessage) {

		return new AskExchangeInfo(Initiator.KNOWLEDGEBASE, aMessage.getFromKnowledgeBase(),
				aMessage.getFromKnowledgeInteraction(), someConvertedBindings, this.previousSend, Instant.now(),
				Status.SUCCEEDED, null);
	}

	private PostExchangeInfo convertMessageToExchangeInfo(BindingSet someConvertedArgumentBindings,
			BindingSet someConvertedResultBindings, ReactMessage aMessage) {

		return new PostExchangeInfo(Initiator.KNOWLEDGEBASE, aMessage.getFromKnowledgeBase(),
				aMessage.getFromKnowledgeInteraction(), someConvertedArgumentBindings, someConvertedResultBindings,
				this.previousSend, Instant.now(), Status.SUCCEEDED, null);
	}

}
