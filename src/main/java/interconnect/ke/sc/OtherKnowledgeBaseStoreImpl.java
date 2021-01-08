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

import interconnect.ke.runtime.KeRuntime;

public class OtherKnowledgeBaseStoreImpl implements OtherKnowledgeBaseStore {

	private long initialDelay = 0;
	private long delay = 3;
	private MetaKnowledgeBase metaKnowledgeBase;
	private ScheduledFuture<?> scheduledFuture;

	private Map<URI, OtherKnowledgeBase> otherKnowledgeBases;

	public OtherKnowledgeBaseStoreImpl(MetaKnowledgeBase metaKnowledgeBase) {
		this.metaKnowledgeBase = metaKnowledgeBase;
		this.otherKnowledgeBases = new ConcurrentHashMap<>();

		scheduledFuture = KeRuntime.executorService().scheduleWithFixedDelay(() -> {
			// retrieve ids from knowledge directory
			Set<URI> ids = KeRuntime.knowledgeDirectory().getKnowledgeBaseIds();

			for (URI id : ids) {

				// retrieve metadata about other knowledge base
				CompletableFuture<OtherKnowledgeBase> otherKnowledgeBaseFuture = this.metaKnowledgeBase
						.getOtherKnowledgeBase(id);

				// when finished, add it to the store.
				otherKnowledgeBaseFuture.thenAccept((otherKnowledgeBase) -> {

					this.otherKnowledgeBases.put(otherKnowledgeBase.getId(), otherKnowledgeBase);
				});
			}
		}, initialDelay, delay, TimeUnit.SECONDS);
	}

	@Override
	public Set<OtherKnowledgeBase> getOtherKnowledgeBases() {
		return Collections.unmodifiableSet(new HashSet<OtherKnowledgeBase>(this.otherKnowledgeBases.values()));
	}

}
