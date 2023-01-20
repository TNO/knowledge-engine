package eu.knowledge.engine.smartconnector.impl;

import java.net.URI;

import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;

public class KnowledgeInteractionInfo {

	public enum Type {
		ASK, ANSWER, POST, REACT
	}

	protected final URI id;
	protected final URI knowledgeBaseId;
	protected final KnowledgeInteractionInfo.Type type;
	protected final KnowledgeInteraction knowledgeInteraction;

	public KnowledgeInteractionInfo(URI id, URI knowledgeBaseId, KnowledgeInteraction knowledgeInteraction) {
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
		this.knowledgeBaseId = knowledgeBaseId;
		this.knowledgeInteraction = knowledgeInteraction;
	}

	public URI getId() {
		return this.id;
	}

	public KnowledgeInteractionInfo.Type getType() {
		return this.type;
	}

	public KnowledgeInteraction getKnowledgeInteraction() {
		return this.knowledgeInteraction;
	}

	public URI getKnowledgeBaseId() {
		return this.knowledgeBaseId;
	}

	@Override
	public String toString() {
		return "KnowledgeInteractionInfo [" + (this.id != null ? "id=" + this.id + ", " : "")
				+ (this.knowledgeBaseId != null ? "knowledgeBaseId=" + this.knowledgeBaseId + ", " : "")
				+ (this.type != null ? "type=" + this.type + ", " : "")
				+ (this.knowledgeInteraction != null ? "knowledgeInteraction=" + this.knowledgeInteraction + ", " : "")
				+ "isMeta=" + this.isMeta() + "]";
	}

	public boolean isMeta() {
		return this.knowledgeInteraction.isMeta();
	}

}