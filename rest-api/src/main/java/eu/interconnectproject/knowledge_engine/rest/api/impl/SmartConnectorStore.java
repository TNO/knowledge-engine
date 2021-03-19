package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;

public class SmartConnectorStore {

	private static final Logger LOG = LoggerFactory.getLogger(SmartConnectorStore.class);

	private Map<URI, SmartConnector> connectors = new HashMap<>();
	private Map<URI, eu.interconnectproject.knowledge_engine.rest.model.SmartConnector> models = new HashMap<>();

	private static SmartConnectorStore instance;

	private SmartConnectorStore() {
		LOG.info("Store initialized!");
	}

	public static SmartConnectorStore newInstance() {
		if (instance == null) {
			instance = new SmartConnectorStore();
		}
		return instance;
	}

	public boolean containsSC(URI knowledgeBaseId) {
		return connectors.containsKey(knowledgeBaseId);
	}

	public void putSC(URI knowledgeBaseId, SmartConnector sc, eu.interconnectproject.knowledge_engine.rest.model.SmartConnector scModel) {
		this.connectors.put(knowledgeBaseId, sc);
		this.models.put(knowledgeBaseId, scModel);
	}

	public SmartConnector getSC(URI knowledgeBaseId) {
		return this.connectors.get(knowledgeBaseId);
	}

	public boolean hasSC(URI knowledgeBaseId) {
		return this.models.containsKey(knowledgeBaseId);
	}

	public Set<eu.interconnectproject.knowledge_engine.rest.model.SmartConnector> getSCModels() {
		return new HashSet<>(this.models.values());
	}

	public eu.interconnectproject.knowledge_engine.rest.model.SmartConnector getSCModel(URI knowledgeBaseId) {
		return this.models.get(knowledgeBaseId);
	}

	public boolean deleteSC(URI knowledgeBaseId) {
		SmartConnector sc = this.connectors.remove(knowledgeBaseId);
		if (sc == null) {
			return false;
		}
		sc.stop();
		eu.interconnectproject.knowledge_engine.rest.model.SmartConnector model = this.models.remove(knowledgeBaseId);
		if (model == null) {
			return false;
		}
		return true;
	}
}
