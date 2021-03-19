package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

	public void createKB(eu.interconnectproject.knowledge_engine.rest.model.SmartConnector scModel) {
		this.restKnowledgeBases.put(scModel.getKnowledgeBaseId(), new RestKnowledgeBase(scModel));
	}

	public RestKnowledgeBase getKB(String knowledgeBaseId) {
		return this.restKnowledgeBases.get(knowledgeBaseId);
	}

	public Set<RestKnowledgeBase> getKBs() {
		return Collections.unmodifiableSet(new HashSet<>(this.restKnowledgeBases.values()));
	}

	public void deleteKB(String knowledgeBaseId) {
		this.restKnowledgeBases.remove(knowledgeBaseId);
	}
}
