package interconnect.ke.sc;

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

import interconnect.ke.runtime.KeRuntime;

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
	public void start() {
		this.updateStore();
		this.scheduledFuture = KeRuntime.executorService().scheduleWithFixedDelay(() -> {
			this.updateStore();
		}, this.delay, this.delay, TimeUnit.SECONDS);

	}

	private void updateStore() {
		// retrieve ids from knowledge directory
		Set<URI> ids = KeRuntime.knowledgeDirectory().getKnowledgeBaseIds();

		for (URI id : ids) {

			if (!id.equals(this.sc.getKnowledgeBaseId())) {

				// retrieve metadata about other knowledge base
				CompletableFuture<OtherKnowledgeBase> otherKnowledgeBaseFuture = this.metaKnowledgeBase
						.getOtherKnowledgeBase(id);

				// when finished, add it to the store.
				otherKnowledgeBaseFuture.thenAccept((otherKnowledgeBase) -> {

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
	}

	@Override
	public void stop() {
		this.scheduledFuture.cancel(false);
	}

}
