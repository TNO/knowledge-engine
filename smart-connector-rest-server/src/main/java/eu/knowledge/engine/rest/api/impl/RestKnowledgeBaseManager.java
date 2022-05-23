package eu.knowledge.engine.rest.api.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestKnowledgeBaseManager {

	private static final Logger LOG = LoggerFactory.getLogger(RestKnowledgeBaseManager.class);

	private Map<String, RestKnowledgeBase> restKnowledgeBases = new ConcurrentHashMap<>();
	private Map<String, RestKnowledgeBase> suspendedRestKnowledgeBases = new ConcurrentHashMap<>();

	private final ScheduledThreadPoolExecutor leaseExpirationExecutor;

	private final static int CHECK_LEASES_PERIOD = 10;
	private final static int CHECK_LEASES_INITIAL_DELAY = 5;

	private static final Object instanceLock = new Object();
	private static volatile RestKnowledgeBaseManager instance;

	private RestKnowledgeBaseManager() {
		LOG.info("RestKnowledgeBaseManager initialized!");

		// Schedule the removal of expired smart connectors periodically.
		this.leaseExpirationExecutor = new ScheduledThreadPoolExecutor(1);
		this.leaseExpirationExecutor.scheduleAtFixedRate(() -> {
			this.removeExpiredSmartConnectors();
		}, CHECK_LEASES_INITIAL_DELAY, CHECK_LEASES_PERIOD, TimeUnit.SECONDS);
	}

	public static RestKnowledgeBaseManager newInstance() {
		// See:
		// - https://stackoverflow.com/a/11165926/2501474
		// - https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
		RestKnowledgeBaseManager r = instance;
		if (r == null) {
			synchronized (instanceLock) { // While we were waiting for the lock, another
				r = instance; // thread may have instantiated the object.
				if (r == null) {
					r = new RestKnowledgeBaseManager();
					instance = r;
				}
			}
		}
		return r;
	}

	public boolean hasKB(String knowledgeBaseId) {
		this.checkSuspendedSmartConnectors();
		return restKnowledgeBases.containsKey(knowledgeBaseId);
	}

	public boolean hasSuspendedKB(String knowledgeBaseId) {
		this.checkSuspendedSmartConnectors();
		return suspendedRestKnowledgeBases.containsKey(knowledgeBaseId);
	}

	public void removeSuspendedKB(String knowledgeBaseId) {
		this.suspendedRestKnowledgeBases.remove(knowledgeBaseId);
	}

	/**
	 * Creates a new KB with a smart connector. Once the smart connector has
	 * received the 'ready' signal, the future is completed.
	 * 
	 * @param scModel
	 * @return
	 */
	public CompletableFuture<Void> createKB(eu.knowledge.engine.rest.model.SmartConnector scModel) {
		this.checkSuspendedSmartConnectors();

		// Make sure we don't keep a suspended KB while we also have a valid one.
		this.removeSuspendedKB(scModel.getKnowledgeBaseId());

		var f = new CompletableFuture<Void>();
		this.restKnowledgeBases.put(scModel.getKnowledgeBaseId(), new RestKnowledgeBase(scModel, () -> {
			f.complete(null);
		}));
		LOG.info("Added KB {}", scModel.getKnowledgeBaseId());
		return f.handle((r, e) -> {

			if (r == null) {
				LOG.error("An exception has occured", e);
				return null;
			} else {
				return r;
			}
		});
	}

	public RestKnowledgeBase getKB(String knowledgeBaseId) {
		this.checkSuspendedSmartConnectors();
		return this.restKnowledgeBases.get(knowledgeBaseId);
	}

	public Set<RestKnowledgeBase> getKBs() {
		this.checkSuspendedSmartConnectors();
		return Collections.unmodifiableSet(new HashSet<>(this.restKnowledgeBases.values()));
	}

	public boolean deleteKB(String knowledgeBaseId) {
		this.checkSuspendedSmartConnectors();

		// Note: We first stop the knowledge base before removing it from our list.
		// (Because in the meantime (while stopping) we cannot have that someone
		// tries to register the same ID)
		var rkb = this.restKnowledgeBases.get(knowledgeBaseId);

		boolean success = false;
		try {
			if (rkb != null)
				rkb.stop();
			success = true;
		} catch (IllegalStateException e) {
			success = false;
		}
		this.restKnowledgeBases.remove(knowledgeBaseId);
		LOG.info("Removed KB {}", knowledgeBaseId);
		return success;
	}

	private void removeExpiredSmartConnectors() {
		this.restKnowledgeBases.entrySet().stream().filter(entry -> entry.getValue().leaseExpired())
				.map(entry -> entry.getKey()).collect(Collectors.toSet()) // Collect it so we don't mutate the list
																			// while iterating over it.
				.forEach(kbId -> {
					LOG.warn("Deleting KB with ID {}, because its lease expired.", kbId);
					this.deleteKB(kbId);
				});
	}

	private void checkSuspendedSmartConnectors() {
		this.restKnowledgeBases.entrySet().stream().filter(entry -> entry.getValue().isSuspended())
				.collect(Collectors.toSet()) // Collect it so we don't mutate the list while iterating over it.
				.forEach(entry -> {
					LOG.info("Moving suspended KB {} to the suspended list.", entry.getKey());
					this.restKnowledgeBases.remove(entry.getKey());
					this.suspendedRestKnowledgeBases.put(entry.getKey(), entry.getValue());
				});
	}
}
