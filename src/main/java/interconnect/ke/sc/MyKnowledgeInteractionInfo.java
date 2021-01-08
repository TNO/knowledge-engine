package interconnect.ke.sc;

import java.net.URI;

import interconnect.ke.api.AnswerHandler;
import interconnect.ke.api.ReactHandler;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;

public class MyKnowledgeInteractionInfo {

	public enum Type {
		ASK, ANSWER, POST, REACT
	}

	private final URI id;
	private final MyKnowledgeInteractionInfo.Type type;
	private final KnowledgeInteraction knowledgeInteraction;
	private final AnswerHandler answerHandler;
	private final ReactHandler reactHandler;

	public MyKnowledgeInteractionInfo(URI id, KnowledgeInteraction knowledgeInteraction, AnswerHandler answerHandler,
			ReactHandler reactHandler) {
		super();
		this.id = id;
		if (knowledgeInteraction instanceof AskKnowledgeInteraction) {
			this.type = Type.ASK;
		} else if (knowledgeInteraction instanceof AnswerKnowledgeInteraction) {
			this.type = Type.ANSWER;
		} else if (knowledgeInteraction instanceof PostKnowledgeInteraction) {
			this.type = Type.POST;
		} else {
			this.type = Type.REACT;
		}
		this.knowledgeInteraction = knowledgeInteraction;
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

	public URI getId() {
		return this.id;
	}

	public MyKnowledgeInteractionInfo.Type getType() {
		return this.type;
	}

	public KnowledgeInteraction getKnowledgeInteraction() {
		return this.knowledgeInteraction;
	}

	public AnswerHandler getAnswerHandler() {
		return this.answerHandler;
	}

	public ReactHandler getReactHandler() {
		return this.reactHandler;
	}

}
