package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestKnowledgeBaseManager {

	private static final Logger LOG = LoggerFactory.getLogger(RestKnowledgeBaseManager.class);

	private Map<String, RestKnowledgeBase> restKnowledgeBases = new HashMap<>();

	private static RestKnowledgeBaseManager instance;

	private RestKnowledgeBaseManager() {
		LOG.info("RestKnowledgeBaseManager initialized!");
	}

	public static RestKnowledgeBaseManager newInstance() {
		if (instance == null) {
			instance = new RestKnowledgeBaseManager();
		}
		return instance;
	}

	public boolean hasKB(String knowledgeBaseId) {
		return restKnowledgeBases.containsKey(knowledgeBaseId);
	}

	/**
	 * Creates a new KB with a smart connector. Once the smart connector has
	 * received the 'ready' signal, the future is completed.
	 * @param scModel
	 * @return
	 */
	public CompletableFuture<Void> createKB(eu.interconnectproject.knowledge_engine.rest.model.SmartConnector scModel) {
		var f = new CompletableFuture<Void>();
		this.restKnowledgeBases.put(scModel.getKnowledgeBaseId(), new RestKnowledgeBase(scModel, () -> {
			f.complete(null);
		}));
		return f;
	}

	public RestKnowledgeBase getKB(String knowledgeBaseId) {
		return this.restKnowledgeBases.get(knowledgeBaseId);
	}

	public Set<RestKnowledgeBase> getKBs() {
		return Collections.unmodifiableSet(new HashSet<>(this.restKnowledgeBases.values()));
	}

	public void deleteKB(String knowledgeBaseId) {
		// Note: We first stop the knowledge base before removing it from our list.
		// (Because in the meantime (while stopping) we cannot have that someone
		// tries to register the same ID)
		var rkb = this.restKnowledgeBases.get(knowledgeBaseId);
		rkb.stop();
		this.restKnowledgeBases.remove(knowledgeBaseId);
	}
}
