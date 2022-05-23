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

import org.apache.jena.graph.Node_Concrete;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.util.FmtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BindingSetHandler;
import eu.knowledge.engine.reasoner.KeReasoner;
import eu.knowledge.engine.reasoner.ReasoningNode;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.TaskBoard;
import eu.knowledge.engine.reasoner.api.TriplePattern;
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
import eu.knowledge.engine.smartconnector.api.PostExchangeInfo;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostResult;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
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
	private Set<Rule> additionalDomainKnowledge;
	private ReasoningNode rootNode;
	private TaskBoard taskBoard;

	/**
	 * These two bindingset handler are a bit dodgy. We need them to make the post
	 * interactions work correctly in the reasoner.
	 */
	private CaptureBindingSetHandler captureResultBindingSetHandler;
	private StoreBindingSetHandler rememberIncomingBindingSetHandler;

	/**
	 * The future returned to the caller of this class which keeps track of when all
	 * the futures of outstanding messages are completed.
	 */
	private CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet> finalBindingSetFuture;

	public ReasonerProcessor(Set<KnowledgeInteractionInfo> knowledgeInteractions, MessageRouter messageRouter,
			Set<Rule> someDomainKnowledge) {
		super(knowledgeInteractions, messageRouter);
		taskBoard = new TaskBoard();

		this.additionalDomainKnowledge = someDomainKnowledge;

		reasoner = new KeReasoner();

		for (Rule r : this.additionalDomainKnowledge) {
			reasoner.addRule(r);
		}

		this.askExchangeInfos = Collections.newSetFromMap(new ConcurrentHashMap<AskExchangeInfo, Boolean>());
		this.postExchangeInfos = Collections.newSetFromMap(new ConcurrentHashMap<PostExchangeInfo, Boolean>());

		for (KnowledgeInteractionInfo kii : knowledgeInteractions) {
			KnowledgeInteraction ki = kii.getKnowledgeInteraction();
			if (kii.getType().equals(Type.ANSWER)) {
				AnswerKnowledgeInteraction aki = (AnswerKnowledgeInteraction) ki;
				GraphPattern gp = aki.getPattern();
				reasoner.addRule(new Rule(new HashSet<>(), new HashSet<>(translateGraphPatternTo(gp)),
						new AnswerBindingSetHandler(kii)));
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

				reasoner.addRule(new Rule(translateGraphPatternTo(argGp), resPattern, new ReactBindingSetHandler(kii)));
			}

		}

	}

	@Override
	public void planAskInteraction(MyKnowledgeInteractionInfo aAKI) {
		this.myKnowledgeInteraction = aAKI;
		KnowledgeInteraction ki = this.myKnowledgeInteraction.getKnowledgeInteraction();
		if (this.myKnowledgeInteraction.getType().equals(Type.ASK)) {
			AskKnowledgeInteraction aki = (AskKnowledgeInteraction) ki;
			this.rootNode = this.reasoner.backwardPlan(translateGraphPatternTo(aki.getPattern()),
					ki.fullMatchOnly() ? MatchStrategy.FIND_ONLY_FULL_MATCHES : MatchStrategy.FIND_ONLY_BIGGEST_MATCHES,
					this.taskBoard);
		} else {
			LOG.warn("Type should be Ask, not {}", this.myKnowledgeInteraction.getType());
			this.finalBindingSetFuture.complete(new eu.knowledge.engine.reasoner.api.BindingSet());
		}
	}

	@Override
	public CompletableFuture<AskResult> executeAskInteraction(BindingSet someBindings) {

		this.finalBindingSetFuture = new CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet>();
		this.rootNode.prune();
		continueReasoningBackward(translateBindingSetTo(someBindings));

		return this.finalBindingSetFuture.thenApply((bs) -> {
			return new AskResult(translateBindingSetFrom(bs), this.askExchangeInfos, this.rootNode);
		});
	}

	private void continueReasoningBackward(eu.knowledge.engine.reasoner.api.BindingSet incomingBS) {

		eu.knowledge.engine.reasoner.api.BindingSet bs = null;
		// TODO think about incorporating the futures into the continueBackward method,
		// i.e. returning a Future<BindingSet> (with child future<bindingsets>).
		if ((bs = this.rootNode.continueBackward(incomingBS)) == null) {

			LOG.debug("ask:\n{}", this.rootNode);
			this.taskBoard.executeScheduledTasks().thenAccept(Void -> {
				LOG.debug("All tasks finished.");
				continueReasoningBackward(incomingBS);
			}).exceptionally((Throwable t) -> {
				LOG.error("Executing scheduled tasks for the reasoner should not result in problems.", t);
				return null;
			});
		} else {
			this.finalBindingSetFuture.complete(bs);
		}
	}

	@Override
	public void planPostInteraction(MyKnowledgeInteractionInfo aPKI) {
		this.myKnowledgeInteraction = aPKI;
		KnowledgeInteraction ki = this.myKnowledgeInteraction.getKnowledgeInteraction();

		if (this.myKnowledgeInteraction.getType().equals(Type.POST)) {

			PostKnowledgeInteraction pki = (PostKnowledgeInteraction) ki;

			if (pki.getResult() != null) {
				this.captureResultBindingSetHandler = new CaptureBindingSetHandler();
				reasoner.addRule(new Rule(translateGraphPatternTo(pki.getResult()), new HashSet<>(),
						this.captureResultBindingSetHandler));
			}

			Set<TriplePattern> translatedGraphPattern = translateGraphPatternTo(pki.getArgument());

			this.rememberIncomingBindingSetHandler = new StoreBindingSetHandler();
			reasoner.addRule(new Rule(new HashSet<>(), new HashSet<>(translatedGraphPattern),
					this.rememberIncomingBindingSetHandler));

			this.rootNode = this.reasoner.forwardPlan(translatedGraphPattern,
					pki.fullMatchOnly() ? MatchStrategy.FIND_ONLY_FULL_MATCHES
							: MatchStrategy.FIND_ONLY_BIGGEST_MATCHES,
					this.taskBoard);

		} else {
			LOG.warn("Type should be Post, not {}", this.myKnowledgeInteraction.getType());
			this.finalBindingSetFuture.complete(new eu.knowledge.engine.reasoner.api.BindingSet());
		}
	}

	@Override
	public CompletableFuture<PostResult> executePostInteraction(BindingSet someBindings) {

		this.finalBindingSetFuture = new CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet>();
		eu.knowledge.engine.reasoner.api.BindingSet translatedBindingSet = translateBindingSetTo(someBindings);
		this.rootNode.prune();
		this.rememberIncomingBindingSetHandler.setBindingSet(translatedBindingSet);

		continueReasoningForward(translatedBindingSet, this.captureResultBindingSetHandler);

		return this.finalBindingSetFuture.thenApply((bs) -> {
			return new PostResult(translateBindingSetFrom(bs), this.postExchangeInfos, this.rootNode);
		});
	}

	private void continueReasoningForward(eu.knowledge.engine.reasoner.api.BindingSet incomingBS,
			CaptureBindingSetHandler aBindingSetHandler) {

		// TODO think about incorporating the futures into the continueBackward method,
		// i.e. returning a Future<BindingSet> (with child future<bindingsets>).
		if (!this.rootNode.continueForward(incomingBS)) {

			LOG.debug("post:\n{}", this.rootNode);
			this.taskBoard.executeScheduledTasks().thenAccept(Void -> {
				continueReasoningForward(incomingBS, aBindingSetHandler);
			}).exceptionally((Throwable t) -> {
				LOG.error("Executing scheduled tasks for the reasoner should not result in errors.", t);
				return null;
			});
		} else {
			eu.knowledge.engine.reasoner.api.BindingSet resultBS = new eu.knowledge.engine.reasoner.api.BindingSet();
			if (aBindingSetHandler != null) {
				resultBS = aBindingSetHandler.getBindingSet();
			}
			this.finalBindingSetFuture.complete(resultBS);
		}
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
			for (Map.Entry<Var, Node_Concrete> entry : b.entrySet()) {
				newB.put(entry.getKey().getName(), FmtUtils.stringForNode(entry.getValue()));
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

	public static class CaptureBindingSetHandler implements BindingSetHandler {

		private eu.knowledge.engine.reasoner.api.BindingSet bs;

		@Override
		public CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet> handle(
				eu.knowledge.engine.reasoner.api.BindingSet bs) {

			this.bs = bs;
			var future = new CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet>();

			future.handle((r, e) -> {

				if (r == null) {
					LOG.error("An exception has occured", e);
					return null;
				} else {
					return r;
				}
			});
			future.complete(new eu.knowledge.engine.reasoner.api.BindingSet());
			return future;
		}

		public eu.knowledge.engine.reasoner.api.BindingSet getBindingSet() {
			return bs;
		}

	}

	private AskExchangeInfo convertMessageToExchangeInfo(BindingSet someConvertedBindings, AnswerMessage aMessage,
			Instant aPreviousSend) {

		return new AskExchangeInfo(Initiator.KNOWLEDGEBASE, aMessage.getFromKnowledgeBase(),
				aMessage.getFromKnowledgeInteraction(), someConvertedBindings, aPreviousSend, Instant.now(),
				Status.SUCCEEDED, null);
	}

	private PostExchangeInfo convertMessageToExchangeInfo(BindingSet someConvertedArgumentBindings,
			BindingSet someConvertedResultBindings, ReactMessage aMessage, Instant aPreviousSend) {

		return new PostExchangeInfo(Initiator.KNOWLEDGEBASE, aMessage.getFromKnowledgeBase(),
				aMessage.getFromKnowledgeInteraction(), someConvertedArgumentBindings, someConvertedResultBindings,
				aPreviousSend, Instant.now(), Status.SUCCEEDED, null);
	}

	/**
	 * A binding set handler to send an ask message to a particular knowledge base.
	 * 
	 * @author nouwtb
	 *
	 */
	public class AnswerBindingSetHandler implements BindingSetHandler {

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
					LOG.error("A problem occurred while handling a bindingset.", t);
					return null; // TODO when some error happens, what do we return?
				}).thenApply((answerMessage) -> {
					LOG.debug("Received ANSWER message from KI '{}'", answerMessage.getFromKnowledgeInteraction());
					BindingSet resultBindingSet = null;
					if (answerMessage != null)
						resultBindingSet = answerMessage.getBindings();

					if (resultBindingSet == null)
						resultBindingSet = new BindingSet();

					ReasonerProcessor.this.askExchangeInfos
							.add(convertMessageToExchangeInfo(resultBindingSet, answerMessage, aPreviousSend));

					return translateBindingSetTo(resultBindingSet);
				});

			} catch (IOException e) {
				LOG.error("No errors should occur while sending an AskMessage.", e);
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
	public class ReactBindingSetHandler implements BindingSetHandler {

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
					LOG.error("A problem occurred while handling a bindingset.", t);
					return null; // TODO when some error happens, what do we return?
				}).thenApply((reactMessage) -> {
					BindingSet resultBindingSet = null;
					if (reactMessage != null)
						resultBindingSet = reactMessage.getResult();

					if (resultBindingSet == null)
						resultBindingSet = new BindingSet();

					ReasonerProcessor.this.postExchangeInfos.add(
							convertMessageToExchangeInfo(newBS, reactMessage.getResult(), reactMessage, aPreviousSend));

					return translateBindingSetTo(resultBindingSet);
				});

			} catch (IOException e) {
				LOG.error("No errors should occur while sending an PostMessage.", e);
				bsFuture = new CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet>();
				bsFuture.complete(new eu.knowledge.engine.reasoner.api.BindingSet());
			}
			return bsFuture;
		}

	}

	static class StoreBindingSetHandler implements BindingSetHandler {

		private eu.knowledge.engine.reasoner.api.BindingSet b = null;

		public StoreBindingSetHandler() {
		}

		public void setBindingSet(eu.knowledge.engine.reasoner.api.BindingSet bs) {
			this.b = bs;
		}

		@Override
		public CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet> handle(
				eu.knowledge.engine.reasoner.api.BindingSet bs) {
			CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet> future = new CompletableFuture<>();

			future.handle((r, e) -> {

				if (r == null) {
					LOG.error("An exception has occured", e);
					return null;
				} else {
					return r;
				}
			});
			future.complete(this.b);
			return future;
		}
	}

	public ReasoningNode getReasoningNode() {
		return this.rootNode;
	}
}
