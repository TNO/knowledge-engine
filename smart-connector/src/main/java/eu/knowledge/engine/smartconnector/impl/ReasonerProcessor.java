package eu.knowledge.engine.smartconnector.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

import eu.knowledge.engine.reasonerprototype.BindingSetHandler;
import eu.knowledge.engine.reasonerprototype.KeReasoner;
import eu.knowledge.engine.reasonerprototype.ReasoningNode;
import eu.knowledge.engine.reasonerprototype.Rule;
import eu.knowledge.engine.reasonerprototype.Rule.MatchStrategy;
import eu.knowledge.engine.reasonerprototype.TaskBoard;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Literal;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Variable;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostResult;
import eu.knowledge.engine.smartconnector.impl.KnowledgeInteractionInfo.Type;
import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.util.GraphPatternSerialization;

public class ReasonerProcessor extends SingleInteractionProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(ReasonerProcessor.class);

	private KeReasoner reasoner;
	private MyKnowledgeInteractionInfo myKnowledgeInteraction;

	public ReasonerProcessor(Set<KnowledgeInteractionInfo> knowledgeInteractions, MessageRouter messageRouter) {
		super(knowledgeInteractions, messageRouter);

		reasoner = new KeReasoner();

		for (KnowledgeInteractionInfo kii : knowledgeInteractions) {
			KnowledgeInteraction ki = kii.getKnowledgeInteraction();
			if (kii.getType().equals(Type.ANSWER)) {
				AnswerKnowledgeInteraction aki = (AnswerKnowledgeInteraction) ki;
				GraphPattern gp = aki.getPattern();
				reasoner.addRule(new Rule(new HashSet<>(), new HashSet<>(translateGraphPatternTo(gp)),
						new BindingSetHandler() {

							@Override
							public eu.knowledge.engine.reasonerprototype.api.BindingSet handle(
									eu.knowledge.engine.reasonerprototype.api.BindingSet bs) {

								BindingSet newBS = translateBindingSetFrom(bs);

								AskMessage askMessage = new AskMessage(
										ReasonerProcessor.this.myKnowledgeInteraction.getKnowledgeBaseId(),
										ReasonerProcessor.this.myKnowledgeInteraction.getId(), kii.getKnowledgeBaseId(),
										kii.getId(), newBS);

								CompletableFuture<AnswerMessage> sendAskMessage = new CompletableFuture<AnswerMessage>();
								try {
									LOG.info("Eek before...");
									sendAskMessage = ReasonerProcessor.this.messageRouter.sendAskMessage(askMessage);
									AnswerMessage answerMessage = sendAskMessage.get();
									LOG.info("Eek after: {}", answerMessage);
									return translateBindingSetTo(answerMessage.getBindings());
								} catch (IOException | InterruptedException | ExecutionException e) {
									LOG.error("No errors should occur while sending an AskMessage.", e);
									sendAskMessage.completeExceptionally(e);
								}
								return new eu.knowledge.engine.reasonerprototype.api.BindingSet();
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

		eu.knowledge.engine.reasonerprototype.api.BindingSet bs = null;
		if (aAKI.getType().equals(Type.ASK)) {
			AskKnowledgeInteraction aki = (AskKnowledgeInteraction) ki;

			ReasoningNode node = this.reasoner.plan(translateGraphPatternTo(aki.getPattern()), ki.fullMatchOnly() ? MatchStrategy.FIND_ONLY_FULL_MATCHES : MatchStrategy.FIND_ONLY_BIGGEST_MATCHES);

			while ((bs = node.executeBackward(translateBindingSetTo(someBindings))) == null) {
//				System.out.println(node);

				TaskBoard.instance().executeScheduledTasks();

			}

		} else {
			LOG.info("Type should be Ask, not {}", aAKI.getType());
		}

		CompletableFuture<AskResult> result = new CompletableFuture<AskResult>();
		result.complete(new AskResult(translateBindingSetFrom(bs), new HashSet<>()));
		return result;
	}

	private BindingSet translateBindingSetFrom(eu.knowledge.engine.reasonerprototype.api.BindingSet bs) {
		BindingSet newBS = new BindingSet();
		Binding newB;
		for (eu.knowledge.engine.reasonerprototype.api.Binding b : bs) {
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

	private eu.knowledge.engine.reasonerprototype.api.BindingSet translateBindingSetTo(BindingSet someBindings) {

		eu.knowledge.engine.reasonerprototype.api.BindingSet newBindingSet = new eu.knowledge.engine.reasonerprototype.api.BindingSet();
		eu.knowledge.engine.reasonerprototype.api.Binding newBinding;
		for (Binding b : someBindings) {

			newBinding = new eu.knowledge.engine.reasonerprototype.api.Binding();
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

	@Override
	CompletableFuture<PostResult> processPostInteraction(MyKnowledgeInteractionInfo aPKI, BindingSet someBindings) {
		// TODO Auto-generated method stub
		return null;
	}

}
