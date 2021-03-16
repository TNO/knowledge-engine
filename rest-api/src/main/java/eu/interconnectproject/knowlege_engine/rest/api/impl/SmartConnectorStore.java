package eu.interconnectproject.knowlege_engine.rest.api.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;

@Singleton
@Path("singleton-configuration-service")
public class SmartConnectorStore {

	private static final Logger LOG = LoggerFactory.getLogger(SmartConnectorStore.class);

	private Map<URI, SmartConnector> connectors = new HashMap<>();
	private Map<URI, eu.interconnectproject.knowlege_engine.rest.model.SmartConnector> models = new HashMap<>();

	public SmartConnectorStore() {
		LOG.info("Store initialized!");
	}

	public boolean containsSC(URI knowledgeBaseId) {
		return connectors.containsKey(knowledgeBaseId);
	}

	public void putSC(URI knowledgeBaseId, SmartConnector sc) {
		this.connectors.put(knowledgeBaseId, sc);
	}
	
	public SmartConnector getSC(URI knowledgeBaseId)
	{
		return this.connectors.get(knowledgeBaseId);
	}

	public boolean hasKB(URI knowledgeBaseId) {
		return this.models.containsKey(knowledgeBaseId);
	}

	public void putKB(URI knowledgeBaseId, eu.interconnectproject.knowlege_engine.rest.model.SmartConnector scModel) {
		this.models.put(knowledgeBaseId, scModel);
	}

	public Set<eu.interconnectproject.knowlege_engine.rest.model.SmartConnector> getKBs() {
		return new HashSet<>(this.models.values());
	}
	
	public eu.interconnectproject.knowlege_engine.rest.model.SmartConnector getKB(URI knowledgeBaseId) {
		return this.models.get(knowledgeBaseId);
	}

}
