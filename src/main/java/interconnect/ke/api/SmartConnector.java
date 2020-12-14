package interconnect.ke.api;

import java.util.Set;

import interconnect.ke.api.binding.Binding;
import interconnect.ke.api.binding.SolutionSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;

public class SmartConnector {

	private KnowledgeBase knowledgeBase;

	public SmartConnector(KnowledgeBase aKnowledgeBase) {
		knowledgeBase = aKnowledgeBase;
	}

	public void register(AskKnowledgeInteraction anAskKI) {

	}

	public void unregister(AskKnowledgeInteraction anAskKI) {

	}

	public void register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler aAnswerHandler) {

	}

	public void unregister(AnswerKnowledgeInteraction anAnswerKI) {

	}

	public void register(PostKnowledgeInteraction aPostKI) {

	}

	public void unregister(PostKnowledgeInteraction aPostKI) {

	}

	public void register(ReactKnowledgeInteraction anReactKI, ReactHandler aReactHandler) {

	}

	public void unregister(ReactKnowledgeInteraction anReactKI) {

	}

	public AskResult ask(AskKnowledgeInteraction ki, RecipientSelector r, SolutionSet bindings) {
		return null;
	}

	public AskResult ask(AskKnowledgeInteraction ki, SolutionSet bindings) {
		return ask(ki, null, bindings);
	}

	public PostResult post(PostKnowledgeInteraction ki, RecipientSelector r, SolutionSet arguments) {
		return null;
	}

	public PostResult post(PostKnowledgeInteraction ki, SolutionSet argument) {
		return post(ki, null, argument);
	}

	public void stop() {

	}
}
