package eu.knowledge.engine.smartconnector.impl;

import java.net.URI;

import eu.knowledge.engine.smartconnector.api.AnswerHandler;
import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.ReactHandler;

public class MyKnowledgeInteractionInfo extends KnowledgeInteractionInfo {

	private final AnswerHandler answerHandler;
	private final ReactHandler reactHandler;

	public MyKnowledgeInteractionInfo(URI id, URI knowledgeBaseId, KnowledgeInteraction knowledgeInteraction,
			AnswerHandler answerHandler, ReactHandler reactHandler, boolean isMeta) {
		super(id, knowledgeBaseId, knowledgeInteraction, isMeta);
		this.answerHandler = answerHandler;
		this.reactHandler = reactHandler;

		// validation
		switch (this.type) {
		case ANSWER:
			if (answerHandler == null) {
				throw new IllegalArgumentException("answerHandler cannot be null");
			}
			if (reactHandler != null) {
				throw new IllegalArgumentException("reactHandler must be null");
			}
			break;
		case ASK:
			if (answerHandler != null) {
				throw new IllegalArgumentException("answerHandler must be null");
			}
			if (reactHandler != null) {
				throw new IllegalArgumentException("reactHandler must be null");
			}
			break;
		case POST:
			if (answerHandler != null) {
				throw new IllegalArgumentException("answerHandler must be null");
			}
			if (reactHandler != null) {
				throw new IllegalArgumentException("reactHandler must be null");
			}
			break;
		case REACT:
			if (answerHandler != null) {
				throw new IllegalArgumentException("answerHandler must be null");
			}
			if (reactHandler == null) {
				throw new IllegalArgumentException("reactHandler cannot be null");
			}
			break;
		}

	}

	public AnswerHandler getAnswerHandler() {
		return this.answerHandler;
	}

	public ReactHandler getReactHandler() {
		return this.reactHandler;
	}

}
