package eu.knowledge.engine.smartconnector.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.util.FmtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.SinkBindingSetHandler;
import eu.knowledge.engine.reasoner.TaskBoard;
import eu.knowledge.engine.reasoner.TransformBindingSetHandler;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskExchangeInfo;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.ExchangeInfo.Initiator;
import eu.knowledge.engine.smartconnector.api.ExchangeInfo.Status;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.MatchStrategy;
import eu.knowledge.engine.smartconnector.api.PostExchangeInfo;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostResult;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.impl.KnowledgeInteractionInfo.Type;
import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.messaging.KnowledgeMessage;
import eu.knowledge.engine.smartconnector.messaging.PostMessage;
import eu.knowledge.engine.smartconnector.messaging.ReactMessage;

/**
 * The Knowledge Engine reasoner processor class. It uses the independent
 * reasoner (which does not know anything about KE concepts like Knowledge
 * Interactions and Knowledge Bases) and makes sure all KE related concepts are
 * automatically and correctly translated into the reasoner concepts like
 * BindingSetHandler, Rule, etc.
 * 
 * @author nouwtb
 *
 */
public class ReasonerProcessor extends SingleInteractionProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(ReasonerProcessor.class);

	private RuleStore store;
	private MyKnowledgeInteractionInfo myKnowledgeInteraction;
	private final Set<AskExchangeInfo> askExchangeInfos;
	private final Set<PostExchangeInfo> postExchangeInfos;
	private Set<Rule> additionalDomainKnowledge;
	private ReasonerPlan reasonerPlan;

	private MatchStrategy matchStrategy = MatchStrategy.NORMAL_LEVEL;

	/**
	 * These two bindingset handler are a bit dodgy. We need them to make the post
	 * interactions work correctly in the reasoner.
	 */
	private CaptureBindingSetHandler captureResultBindingSetHandler;

	/**
	 * The future returned to the caller of this class which keeps track of when all
	 * the futures of outstanding messages are completed.
	 */
	private CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet> finalBindingSetFuture;

	public ReasonerProcessor(Set<KnowledgeInteractionInfo> knowledgeInteractions, MessageRouter messageRouter,
			Set<Rule> someDomainKnowledge) {
		super(knowledgeInteractions, messageRouter);

		this.additionalDomainKnowledge = someDomainKnowledge;

		store = new RuleStore();

		for (BaseRule r : this.additionalDomainKnowledge) {
			store.addRule(r);
		}

		this.askExchangeInfos = Collections.newSetFromMap(new ConcurrentHashMap<AskExchangeInfo, Boolean>());
		this.postExchangeInfos = Collections.newSetFromMap(new ConcurrentHashMap<PostExchangeInfo, Boolean>());

		for (KnowledgeInteractionInfo kii : knowledgeInteractions) {
			String ruleName = kii.getKnowledgeInteraction().getName() != null ? kii.getKnowledgeInteraction().getName()
					: kii.getId().toString();
			KnowledgeInteraction ki = kii.getKnowledgeInteraction();
			if (kii.getType().equals(Type.ANSWER)) {
				AnswerKnowledgeInteraction aki = (AnswerKnowledgeInteraction) ki;
				GraphPattern gp = aki.getPattern();
				Rule aRule = new Rule(ruleName, new HashSet<>(translateGraphPatternTo(gp)),
						new AnswerBindingSetHandler(kii));
				store.addRule(aRule);
				LOG.debug("Adding ANSWER to store: {}", aRule);
			} else if (kii.getType().equals(Type.REACT)) {
				ReactKnowledgeInteraction rki = (ReactKnowledgeInteraction) ki;
				GraphPattern argGp = rki.getArgument();
				GraphPattern resGp = rki.getResult();

				Set<TriplePattern> resPattern;
				Rule aRule;
				if (resGp == null) {
					aRule = new Rule(ruleName, translateGraphPatternTo(argGp), new ReactVoidBindingSetHandler(kii));
				} else {
					resPattern = new HashSet<>(translateGraphPatternTo(resGp));
					aRule = new Rule(ruleName, translateGraphPatternTo(argGp), resPattern,
							new ReactBindingSetHandler(kii));
				}

				store.addRule(aRule);
				LOG.debug("Adding REACT to store: {}", aRule);
			}

		}

	}

	@Override
	public void planAskInteraction(MyKnowledgeInteractionInfo aAKI) {
		this.myKnowledgeInteraction = aAKI;
		KnowledgeInteraction ki = this.myKnowledgeInteraction.getKnowledgeInteraction();
		if (this.myKnowledgeInteraction.getType().equals(Type.ASK)) {
			AskKnowledgeInteraction aki = (AskKnowledgeInteraction) ki;

			String ruleName = aAKI.getKnowledgeInteraction().getName() != null
					? aAKI.getKnowledgeInteraction().getName()
					: aAKI.getId().toString();

			ProactiveRule aRule = new ProactiveRule(ruleName, translateGraphPatternTo(aki.getPattern()),
					new HashSet<>());
			this.store.addRule(aRule);
			MatchStrategy aStrategy;
			if (aAKI.getKnowledgeInteraction().getMatchStrategy() == null)
				aStrategy = this.matchStrategy;
			else
				aStrategy = aki.getMatchStrategy();

			LOG.debug("Creating reasoner plan with strategy: {}", aStrategy);
			this.reasonerPlan = new ReasonerPlan(this.store, aRule, aStrategy.toConfig(true));
		} else {
			LOG.warn("Type should be Ask, not {}", this.myKnowledgeInteraction.getType());
			this.finalBindingSetFuture.complete(new eu.knowledge.engine.reasoner.api.BindingSet());
		}
	}

	@Override
	public CompletableFuture<AskResult> executeAskInteraction(BindingSet someBindings) {

		this.finalBindingSetFuture = new CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet>();
//		this.reasonerPlan.optimize();
		continueReasoningBackward(translateBindingSetTo(someBindings));

		return this.finalBindingSetFuture.thenApply((bs) -> {
			return new AskResult(translateBindingSetFrom(bs), this.askExchangeInfos, this.reasonerPlan);
		});
	}

	private void continueReasoningBackward(eu.knowledge.engine.reasoner.api.BindingSet incomingBS) {

		boolean isComplete;
		TaskBoard taskboard;
		final String msg = "Executing (scheduled) tasks for the reasoner should not result in problems.";
		taskboard = this.reasonerPlan.execute(incomingBS);
		isComplete = !taskboard.hasTasks();
		LOG.debug("ask:\n{}", this.reasonerPlan);
		taskboard.executeScheduledTasks().thenAccept(Void -> {
			LOG.debug("All ask tasks finished.");
			if (isComplete) {
				eu.knowledge.engine.reasoner.api.BindingSet bs = this.reasonerPlan.getResults();
				this.finalBindingSetFuture.complete(bs);
			} else
				continueReasoningBackward(incomingBS);
		}).exceptionally((Throwable t) -> {
			LOG.error(msg, t);
			return null;
		});

	}

	@Override
	public void planPostInteraction(MyKnowledgeInteractionInfo aPKI) {
		this.myKnowledgeInteraction = aPKI;
		KnowledgeInteraction ki = this.myKnowledgeInteraction.getKnowledgeInteraction();

		if (this.myKnowledgeInteraction.getType().equals(Type.POST)) {

			PostKnowledgeInteraction pki = (PostKnowledgeInteraction) ki;
			String ruleName = aPKI.getKnowledgeInteraction().getName() != null
					? aPKI.getKnowledgeInteraction().getName()
					: aPKI.getId().toString();

			if (pki.getResult() != null) {
				this.captureResultBindingSetHandler = new CaptureBindingSetHandler();

				store.addRule(new Rule(ruleName, translateGraphPatternTo(pki.getResult()),
						this.captureResultBindingSetHandler));
			}

			Set<TriplePattern> translatedGraphPattern = translateGraphPatternTo(pki.getArgument());

			ProactiveRule aRule = new ProactiveRule(ruleName, new HashSet<>(), new HashSet<>(translatedGraphPattern));
			store.addRule(aRule);

			MatchStrategy aStrategy;
			if (pki.getMatchStrategy() == null)
				aStrategy = this.matchStrategy;
			else
				aStrategy = pki.getMatchStrategy();

			LOG.debug("Creating reasoner plan with strategy: {}", aStrategy);
			this.reasonerPlan = new ReasonerPlan(this.store, aRule, aStrategy.toConfig(false));

		} else {
			LOG.warn("Type should be Post, not {}", this.myKnowledgeInteraction.getType());
			this.finalBindingSetFuture.complete(new eu.knowledge.engine.reasoner.api.BindingSet());
		}
	}

	@Override
	public CompletableFuture<PostResult> executePostInteraction(BindingSet someBindings) {

		this.finalBindingSetFuture = new CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet>();
		eu.knowledge.engine.reasoner.api.BindingSet translatedBindingSet = translateBindingSetTo(someBindings);
//		this.reasonerPlan.optimize();

		continueReasoningForward(translatedBindingSet, this.captureResultBindingSetHandler);

		return this.finalBindingSetFuture.thenApply((bs) -> {
			return new PostResult(translateBindingSetFrom(bs), this.postExchangeInfos, this.reasonerPlan);
		});
	}

	private void continueReasoningForward(eu.knowledge.engine.reasoner.api.BindingSet incomingBS,
			CaptureBindingSetHandler aBindingSetHandler) {

		String msg = "Executing (scheduled) tasks for the reasoner should not result in errors.";
		boolean isComplete;
		TaskBoard taskboard;
		taskboard = this.reasonerPlan.execute(incomingBS);
		isComplete = !taskboard.hasTasks();
		LOG.debug("post:\n{}", this.reasonerPlan);
		taskboard.executeScheduledTasks().thenAccept(Void -> {
			LOG.debug("All post tasks finished.");
			if (isComplete) {
				eu.knowledge.engine.reasoner.api.BindingSet resultBS = new eu.knowledge.engine.reasoner.api.BindingSet();
				if (aBindingSetHandler != null) {
					resultBS = aBindingSetHandler.getBindingSet();
				}
				this.finalBindingSetFuture.complete(resultBS);
			} else {
				continueReasoningForward(incomingBS, aBindingSetHandler);
			}

		}).exceptionally((Throwable t) -> {
			LOG.error(msg, t);
			return null;
		});

	}

	/**
	 * Translate bindingset from the reasoner bindingsets to the ke bindingsets.
	 * 
	 * @param bs a reasoner bindingset
	 * @return a ke bindingset
	 */
	protected BindingSet translateBindingSetFrom(eu.knowledge.engine.reasoner.api.BindingSet bs) {
		BindingSet newBS = new BindingSet();
		Binding newB;

		SerializationContext context = new SerializationContext();
		context.setUsePlainLiterals(false);

		for (eu.knowledge.engine.reasoner.api.Binding b : bs) {
			newB = new Binding();
			for (Map.Entry<Var, Node> entry : b.entrySet()) {
				newB.put(entry.getKey().getName(), FmtUtils.stringForNode(entry.getValue(), context));
			}
			newBS.add(newB);
		}
		return newBS;
	}

	/**
	 * Translate bindingset from the ke bindingset to the reasoner bindingset.
	 * 
	 * @param someBindings a ke bindingset
	 * @return a reasoner bindingset
	 */
	protected eu.knowledge.engine.reasoner.api.BindingSet translateBindingSetTo(BindingSet someBindings) {

		eu.knowledge.engine.reasoner.api.BindingSet newBindingSet = new eu.knowledge.engine.reasoner.api.BindingSet();
		eu.knowledge.engine.reasoner.api.Binding newBinding;
		for (Binding b : someBindings) {

			newBinding = new eu.knowledge.engine.reasoner.api.Binding();
			for (String var : b.getVariables()) {
				newBinding.put(var, b.get(var));
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

	public static class CaptureBindingSetHandler implements SinkBindingSetHandler {

		private eu.knowledge.engine.reasoner.api.BindingSet bs;

		@Override
		public CompletableFuture<Void> handle(eu.knowledge.engine.reasoner.api.BindingSet bs) {

			this.bs = bs;
			var future = new CompletableFuture<Void>();

			future.handle((r, e) -> {

				if (e != null) {
					LOG.error("An exception has occured while capturing binging set", e);
					return null;
				} else {
					return r;
				}
			});
			future.complete((Void) null);
			return future;
		}

		public eu.knowledge.engine.reasoner.api.BindingSet getBindingSet() {
			return bs;
		}

	}

	private AskExchangeInfo convertMessageToExchangeInfo(BindingSet someConvertedBindings, AnswerMessage aMessage,
			Instant aPreviousSend) {
		Status status = Status.SUCCEEDED;
		String failedMessage = aMessage.getFailedMessage();

		if (failedMessage != null) {
			status = Status.FAILED;
		}

		return new AskExchangeInfo(Initiator.KNOWLEDGEBASE, aMessage.getFromKnowledgeBase(),
				aMessage.getFromKnowledgeInteraction(), someConvertedBindings, aPreviousSend, Instant.now(), status,
				failedMessage);
	}

	private PostExchangeInfo convertMessageToExchangeInfo(BindingSet someConvertedArgumentBindings,
			BindingSet someConvertedResultBindings, ReactMessage aMessage, Instant aPreviousSend) {
		Status status = Status.SUCCEEDED;
		String failedMessage = aMessage.getFailedMessage();

		if (failedMessage != null) {
			status = Status.FAILED;
		}

		return new PostExchangeInfo(Initiator.KNOWLEDGEBASE, aMessage.getFromKnowledgeBase(),
				aMessage.getFromKnowledgeInteraction(), someConvertedArgumentBindings, someConvertedResultBindings,
				aPreviousSend, Instant.now(), status, failedMessage);
	}

	/**
	 * A binding set handler to send an ask message to a particular knowledge base.
	 * 
	 * @author nouwtb
	 *
	 */
	public class AnswerBindingSetHandler implements TransformBindingSetHandler {

		private KnowledgeInteractionInfo kii;

		public AnswerBindingSetHandler(KnowledgeInteractionInfo aKii) {
			this.kii = aKii;
		}

		public KnowledgeInteractionInfo getKnowledgeInteractionInfo() {
			return this.kii;
		}

		@Override
		public CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet> handle(
				eu.knowledge.engine.reasoner.api.BindingSet bs) {
			CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet> bsFuture;
			BindingSet newBS = translateBindingSetFrom(bs);

			AskMessage askMessage = new AskMessage(ReasonerProcessor.this.myKnowledgeInteraction.getKnowledgeBaseId(),
					ReasonerProcessor.this.myKnowledgeInteraction.getId(), this.kii.getKnowledgeBaseId(),
					this.kii.getId(), newBS);

			try {
				CompletableFuture<AnswerMessage> sendAskMessage = ReasonerProcessor.this.messageRouter
						.sendAskMessage(askMessage);
				Instant aPreviousSend = Instant.now();

				bsFuture = sendAskMessage.exceptionally((Throwable t) -> {

					String failedMessage = MessageFormatter
							.basicArrayFormat("Error '{}' occurred while waiting for response to message: {}",
									new String[] {
											t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName(),
											askMessage.getMessageId().toString() });

					LOG.warn(failedMessage);
					LOG.debug("", t);
					return ReasonerProcessor.this
							.<AskMessage, AnswerMessage>createFailedResponseMessageFromRequestMessage(askMessage,
									failedMessage);
				}).thenApply((answerMessage) -> {
					assert answerMessage != null;
					LOG.debug("Received ANSWER message from KI '{}'", answerMessage.getFromKnowledgeInteraction());
					BindingSet resultBindingSet = answerMessage.getBindings();

					ReasonerProcessor.this.askExchangeInfos
							.add(convertMessageToExchangeInfo(resultBindingSet, answerMessage, aPreviousSend));

					return translateBindingSetTo(resultBindingSet);
				});

			} catch (IOException e) {
				LOG.warn("Error '{}' occurred while sending {}",
						e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
						askMessage.getClass().getSimpleName());
				LOG.debug("", e);
				bsFuture = new CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet>();
				bsFuture.complete(new eu.knowledge.engine.reasoner.api.BindingSet());
			}
			return bsFuture;
		}

	}

	@SuppressWarnings("unchecked")
	private <T extends KnowledgeMessage, S extends KnowledgeMessage> S createFailedResponseMessageFromRequestMessage(
			T incomingMessage, String failedMessage) {

		S outgoingMessage = null;
		if (incomingMessage instanceof AskMessage) {

			outgoingMessage = (S) new AnswerMessage(incomingMessage.getToKnowledgeBase(),
					incomingMessage.getToKnowledgeInteraction(), incomingMessage.getFromKnowledgeBase(),
					incomingMessage.getFromKnowledgeInteraction(), incomingMessage.getMessageId(), failedMessage);

		} else if (incomingMessage instanceof PostMessage) {
			outgoingMessage = (S) new AnswerMessage(incomingMessage.getToKnowledgeBase(),
					incomingMessage.getToKnowledgeInteraction(), incomingMessage.getFromKnowledgeBase(),
					incomingMessage.getFromKnowledgeInteraction(), incomingMessage.getMessageId(), failedMessage);
		}

		assert outgoingMessage != null;
		return outgoingMessage;
	}

	/**
	 * A bindingsethandler to send a post message to a particular knowledge base.
	 * 
	 * @author nouwtb
	 *
	 */
	public class ReactBindingSetHandler implements TransformBindingSetHandler {

		private KnowledgeInteractionInfo kii;

		public ReactBindingSetHandler(KnowledgeInteractionInfo aKii) {
			this.kii = aKii;
		}

		public KnowledgeInteractionInfo getKnowledgeInteractionInfo() {
			return this.kii;
		}

		@Override
		public CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet> handle(
				eu.knowledge.engine.reasoner.api.BindingSet bs) {

			CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet> bsFuture;

			BindingSet newBS = translateBindingSetFrom(bs);

			PostMessage postMessage = new PostMessage(
					ReasonerProcessor.this.myKnowledgeInteraction.getKnowledgeBaseId(),
					ReasonerProcessor.this.myKnowledgeInteraction.getId(), kii.getKnowledgeBaseId(), kii.getId(),
					newBS);

			try {
				CompletableFuture<ReactMessage> sendPostMessage = ReasonerProcessor.this.messageRouter
						.sendPostMessage(postMessage);
				Instant aPreviousSend = Instant.now();
				bsFuture = sendPostMessage.exceptionally((Throwable t) -> {
					String failedMessage = MessageFormatter
							.basicArrayFormat("Error '{}' occurred while waiting for response to message: {}",
									new String[] {
											t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName(),
											postMessage.getMessageId().toString() });
					LOG.warn(failedMessage);
					LOG.debug("", t);
					return ReasonerProcessor.this
							.<PostMessage, ReactMessage>createFailedResponseMessageFromRequestMessage(postMessage,
									failedMessage);
				}).thenApply((reactMessage) -> {
					assert reactMessage != null;
					BindingSet resultBindingSet = reactMessage.getResult();

					ReasonerProcessor.this.postExchangeInfos.add(
							convertMessageToExchangeInfo(newBS, reactMessage.getResult(), reactMessage, aPreviousSend));

					return translateBindingSetTo(resultBindingSet);
				});

			} catch (IOException e) {
				LOG.warn("Error '{}' occurred while sending {}",
						e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
						postMessage.getClass().getSimpleName());
				LOG.debug("", e);
				bsFuture = new CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet>();
				bsFuture.complete(new eu.knowledge.engine.reasoner.api.BindingSet());
			}
			return bsFuture;
		}

	}

	/**
	 * A bindingsethandler to send a post message to a particular knowledge base.
	 * 
	 * @author nouwtb
	 *
	 */
	public class ReactVoidBindingSetHandler implements SinkBindingSetHandler {

		private KnowledgeInteractionInfo kii;

		public ReactVoidBindingSetHandler(KnowledgeInteractionInfo aKii) {
			this.kii = aKii;
		}

		public KnowledgeInteractionInfo getKnowledgeInteractionInfo() {
			return this.kii;
		}

		@Override
		public CompletableFuture<Void> handle(eu.knowledge.engine.reasoner.api.BindingSet bs) {

			CompletableFuture<Void> bsFuture;

			BindingSet newBS = translateBindingSetFrom(bs);

			PostMessage postMessage = new PostMessage(
					ReasonerProcessor.this.myKnowledgeInteraction.getKnowledgeBaseId(),
					ReasonerProcessor.this.myKnowledgeInteraction.getId(), kii.getKnowledgeBaseId(), kii.getId(),
					newBS);

			try {
				CompletableFuture<ReactMessage> sendPostMessage = ReasonerProcessor.this.messageRouter
						.sendPostMessage(postMessage);
				Instant aPreviousSend = Instant.now();
				bsFuture = sendPostMessage.exceptionally((Throwable t) -> {
					LOG.error("A problem occurred while handling a bindingset.", t);
					return null; // TODO when some error happens, what do we return?
				}).thenApply((reactMessage) -> {

					ReasonerProcessor.this.postExchangeInfos
							.add(convertMessageToExchangeInfo(newBS, new BindingSet(), reactMessage, aPreviousSend));

					return (Void) null;
				});

			} catch (IOException e) {
				LOG.error("No errors should occur while sending an PostMessage.", e);
				bsFuture = new CompletableFuture<Void>();
				bsFuture.complete((Void) null);
			}
			return bsFuture;
		}

	}

	public ReasonerPlan getReasonerPlan() {
		return this.reasonerPlan;
	}

	public void setMatchStrategy(MatchStrategy aStrategy) {
		this.matchStrategy = aStrategy;
	}
}
