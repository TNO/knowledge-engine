package eu.knowledge.engine.smartconnector.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasonerprototype.KeReasonerAlt;
import eu.knowledge.engine.reasonerprototype.NodeAlt;
import eu.knowledge.engine.reasonerprototype.RuleAlt;
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

public class ReasonerProcessor extends SingleInteractionProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(ReasonerProcessor.class);

	private KeReasonerAlt reasoner;

	public ReasonerProcessor(Set<KnowledgeInteractionInfo> knowledgeInteractions, MessageRouter messageRouter) {
		super(knowledgeInteractions, messageRouter);

		reasoner = new KeReasonerAlt();

		for (KnowledgeInteractionInfo kii : knowledgeInteractions) {
			KnowledgeInteraction ki = kii.getKnowledgeInteraction();
			if (kii.getType().equals(Type.ANSWER)) {
				AnswerKnowledgeInteraction aki = (AnswerKnowledgeInteraction) ki;
				GraphPattern gp = aki.getPattern();
				reasoner.addRule(new RuleAlt(new HashSet<>(), new HashSet<>(translateGraphPatternTo(gp))));
			}
		}
	}

	@Override
	public CompletableFuture<AskResult> processAskInteraction(MyKnowledgeInteractionInfo aAKI,
			BindingSet someBindings) {

		KnowledgeInteraction ki = aAKI.getKnowledgeInteraction();
		eu.knowledge.engine.reasonerprototype.api.BindingSet bs = null;
		if (aAKI.getType().equals(Type.ASK)) {
			AskKnowledgeInteraction aki = (AskKnowledgeInteraction) ki;

			NodeAlt node = this.reasoner.plan(translateGraphPatternTo(aki.getPattern()));

			while ((bs = node.continueReasoning(translateBindingSetTo(someBindings))) == null) {
				System.out.println(node);
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
				newB.put(entry.getKey().getVariableName(), entry.getValue().getValue());
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
			triple = triplePath.asTriple().getSubject() + " " + triplePath.asTriple().getPredicate() + " "
					+ triplePath.asTriple().getObject();

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
