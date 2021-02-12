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
		CompletableFuture<Void> future = this.updateStore();
		this.scheduledFuture = KeRuntime.executorService().scheduleWithFixedDelay(() -> {
			this.updateStore();
		}, this.delay, this.delay, TimeUnit.SECONDS);
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
		this.scheduledFuture.cancel(false);
	}

}
