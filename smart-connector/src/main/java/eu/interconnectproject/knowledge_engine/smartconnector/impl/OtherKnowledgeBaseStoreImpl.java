package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import eu.interconnectproject.knowledge_engine.smartconnector.runtime.KeRuntime;

public class OtherKnowledgeBaseStoreImpl implements OtherKnowledgeBaseStore {

	private final long delay = 3;
	private final MetaKnowledgeBase metaKnowledgeBase;
	private ScheduledFuture<?> scheduledFuture;
	private final SmartConnectorImpl sc;

	private final Logger LOG;

	private final Map<URI, OtherKnowledgeBase> otherKnowledgeBases;

	public OtherKnowledgeBaseStoreImpl(SmartConnectorImpl sc, MetaKnowledgeBase metaKnowledgeBase) {
		this.sc = sc;
		this.LOG = this.sc.getLogger(this.getClass());
		this.metaKnowledgeBase = metaKnowledgeBase;
		this.otherKnowledgeBases = new ConcurrentHashMap<>();
	}

	@Override
	public Set<OtherKnowledgeBase> getOtherKnowledgeBases() {
		return Collections.unmodifiableSet(new HashSet<>(this.otherKnowledgeBases.values()));
	}

	@Override
	public CompletableFuture<Void> start() {
		// Do a first update to get to know all peers in the network.
		CompletableFuture<Void> future = this.updateStore();

		// When it is done with the first update, schedule subsequent updates with a
		// `this.delay` seconds delay between them.
		future.thenRun(() -> {
			// For this branch, the goal is to remove the following statement, and
			// have the tests still pass...
			// this.scheduledFuture = KeRuntime.executorService().scheduleWithFixedDelay(() -> {
			// 	this.updateStore();
			// }, this.delay, this.delay, TimeUnit.SECONDS);

			// Tell the meta knowledge base to POST information about our novel
			// knowledge base to the peers
			// TODO: Do we want to skip ourselves?
			this.metaKnowledgeBase.postNewKnowledgeBase(this.getOtherKnowledgeBases());
		});

		return future;
	}

	private CompletableFuture<Void> updateStore() {
		// retrieve ids from knowledge directory
		Set<URI> newIds = KeRuntime.knowledgeDirectory().getKnowledgeBaseIds();

		Set<CompletableFuture<?>> futures = new HashSet<>();

		// remove other knowledgebases that are no longer available.
		Set<URI> noLongerAvailableIds = new HashSet<>(this.otherKnowledgeBases.keySet());
		noLongerAvailableIds.removeAll(newIds);

		for (URI id : noLongerAvailableIds) {
			this.otherKnowledgeBases.remove(id);
		}

		// update the information with new or already existing other knowledge bases.
		for (URI id : newIds) {

			if (!id.equals(this.sc.getKnowledgeBaseId())) {

				// retrieve metadata about other knowledge base
				CompletableFuture<OtherKnowledgeBase> otherKnowledgeBaseFuture = this.metaKnowledgeBase
						.getOtherKnowledgeBase(id);

				futures.add(otherKnowledgeBaseFuture);

				// when finished, add it to the store.
				otherKnowledgeBaseFuture.thenAccept(otherKnowledgeBase -> {

					assert otherKnowledgeBase != null : "The other knowledge base should be non-null.";

					try {
						this.otherKnowledgeBases.put(otherKnowledgeBase.getId(), otherKnowledgeBase);
					} catch (Throwable t) {
						this.LOG.error("Adding an other knowledgebase should succeed.", t);
					}
				});
			} else {
				this.LOG.trace("Skipping myself: {}", this.sc.getKnowledgeBaseId());
			}
		}

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()]));
	}

	@Override
	public void stop() {
		// this.scheduledFuture.cancel(false);
	}

	@Override
	public void addKnowledgeBase(OtherKnowledgeBase kb) {
		if (this.otherKnowledgeBases.containsKey(kb.getId())) {
			LOG.warn("Tried to add a knowledge base {}, but it is already in my store! Skipped it.", kb.getId());
			return;
		}

		try {
			this.otherKnowledgeBases.put(kb.getId(), kb);
		} catch (Throwable t) {
			this.LOG.error("Adding an other knowledgebase should succeed.", t);
		}
	}
	
	@Override
	public void updateKnowledgeBase(OtherKnowledgeBase kb) {
		if (!this.otherKnowledgeBases.containsKey(kb.getId())) {
			LOG.warn("Tried to update knowledge base {}, but it is not in my store! Skipped it.", kb.getId());
			return;
		}

		try {
			this.otherKnowledgeBases.put(kb.getId(), kb);
		} catch (Throwable t) {
			this.LOG.error("Updating an other knowledgebase should succeed.", t);
		}
	}

	@Override
	public void removeKnowledgeBase(OtherKnowledgeBase kb) {
		if (!this.otherKnowledgeBases.containsKey(kb.getId())) {
			LOG.warn("Tried to remove knowledge base {}, but it isn't even in my store! Skipped it.", kb.getId());
			return;
		}

		try {
			this.otherKnowledgeBases.remove(kb.getId());
		} catch (Throwable t) {
			this.LOG.error("Removing an other knowledgebase should succeed.", t);
		}
	}
}
