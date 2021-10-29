package eu.knowledge.engine.rest.api.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestKnowledgeBaseManager {

	private static final Logger LOG = LoggerFactory.getLogger(RestKnowledgeBaseManager.class);

	private Map<String, RestKnowledgeBase> restKnowledgeBases = new HashMap<>();

	private final ScheduledThreadPoolExecutor leaseExpirationExecutor;

	private final static int CHECK_LEASES_PERIOD = 10;
	private final static int CHECK_LEASES_INITIAL_DELAY = 5;

	private static RestKnowledgeBaseManager instance;

	private RestKnowledgeBaseManager() {
		LOG.info("RestKnowledgeBaseManager initialized!");

		// Schedule the removal of expired smart connectors periodically.
		this.leaseExpirationExecutor = new ScheduledThreadPoolExecutor(1);
		this.leaseExpirationExecutor.scheduleAtFixedRate(() -> {
			this.removeExpiredSmartConnectors();
		}, CHECK_LEASES_INITIAL_DELAY, CHECK_LEASES_PERIOD, TimeUnit.SECONDS);
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
	public CompletableFuture<Void> createKB(eu.knowledge.engine.rest.model.SmartConnector scModel) {
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

	public boolean deleteKB(String knowledgeBaseId) {
		// Note: We first stop the knowledge base before removing it from our list.
		// (Because in the meantime (while stopping) we cannot have that someone
		// tries to register the same ID)
		var rkb = this.restKnowledgeBases.get(knowledgeBaseId);

		boolean success = false;
		try {
			rkb.stop();
			success = true;
		} catch (IllegalStateException e) {
			success = false;
		}
		this.restKnowledgeBases.remove(knowledgeBaseId);
		return success;
	}

	private void removeExpiredSmartConnectors() {
		this.restKnowledgeBases.entrySet().stream()
			.filter(entry -> entry.getValue().leaseExpired())
			.map(entry -> entry.getKey())
			.collect(Collectors.toSet()) // Collect it so we don't mutate the list while iterating over it.
			.forEach(kbId -> {
				LOG.warn("Deleting KB with ID {}, because its lease expired.", kbId);
				this.deleteKB(kbId);
			});
	}
}
