package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;
import eu.interconnectproject.knowledge_engine.reasonerprototype.ki.AnswerKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.reasonerprototype.ki.KnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.reasonerprototype.ki.ReactKnowledgeInteraction;

public class KeReasoner implements ReasoningNode {

	private final Map<URI, KnowledgeInteraction> knowledgeInteractions = new HashMap<>();
	private final List<LocalRule> localRules = new ArrayList<>();
	private final List<Rule> rules = new ArrayList<>();
	private final RemoteTaskBoard remoteTaskBoard;
	private BindingSet resultBindingSet;;

	public KeReasoner() {
		remoteTaskBoard = new RemoteTaskBoard(this);
	}

	public void addKnowledgeInteraction(KnowledgeInteraction knowledgeInteraction) {
		this.knowledgeInteractions.put(knowledgeInteraction.getId(), knowledgeInteraction);
	}

	public void addLocalRule(LocalRule localRule) {
		this.localRules.add(localRule);
	}

	public BindingSet reason(List<Triple> objective, Binding binding) {
		System.out.println("Reasoning objective: " + objective + " with binding " + binding + "\n");

		createRules();

		MultiObjectiveReasoningNode root = new MultiObjectiveReasoningNode(this, this, objective, binding);

		boolean success = root.plan();

		if (success) {
			System.out.println("Successfully planned reasoning\n");
		} else {
			System.out.println("Failed to plan reasoning\n");
			return null;
		}

		System.out.println(this.remoteTaskBoard);

		while (remoteTaskBoard.hasMoreTasks()) {
			remoteTaskBoard.executeTasksForSingleKnowledgeInteraction();
		}

		// With all remote tasks executed the method processResultingBindingSet should
		// be called by now

		return resultBindingSet;
	}

	private void createRules() {
		rules.clear();
		for (KnowledgeInteraction knowledgeInteraction : knowledgeInteractions.values()) {
			if (knowledgeInteraction instanceof AnswerKnowledgeInteraction) {
				rules.add(new RemoteRule(Collections.emptyList(),
						((AnswerKnowledgeInteraction) knowledgeInteraction).getGraphPattern(),
						knowledgeInteraction.getId()));
			} else if (knowledgeInteraction instanceof ReactKnowledgeInteraction) {
				// TODO filter based communicative act
				rules.add(new RemoteRule(((ReactKnowledgeInteraction) knowledgeInteraction).getArgumentGraphPattern(),
						((ReactKnowledgeInteraction) knowledgeInteraction).getResultGraphPattern(),
						knowledgeInteraction.getId()));
			}
		}
		rules.addAll(localRules);
		System.out.println("Rules:");
		for (Rule rule : rules) {
			System.out.println(rule);
		}
		System.out.println("");
	}

	List<Rule> findRulesFor(Triple objective, Binding binding) {
		return rules.stream().filter(r -> r.rhsMatches(objective, binding)).collect(Collectors.toList());
	}

	List<Rule> findRulesFor(List<Triple> objective, Binding binding) {
		return rules.stream().filter(r -> r.rhsMatches(objective, binding)).collect(Collectors.toList());
	}

	public RemoteTaskBoard getRemoteTaskBoard() {
		return remoteTaskBoard;
	}

	public KnowledgeInteraction getKnowledgeInteraction(URI id) {
		return knowledgeInteractions.get(id);
	}

	@Override
	public void processResultingBindingSet(ReasoningNode child, BindingSet bindingSet) {
		this.resultBindingSet = bindingSet;
	}

	@Override
	public boolean plan() {
		// Not used
		return false;
	}

}
