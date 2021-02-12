package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerHandler;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactHandler;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AskKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.PostKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.KnowledgeInteractionInfo.Type;

public class KnowledgeBaseStoreImpl implements KnowledgeBaseStore {

	private static final String ASK_SUFFIX = "/meta/knowledgeinteractions/ask";
	private static final String ANSWER_SUFFIX = "/meta/knowledgeinteractions/answer";

	private final Logger LOG;
	private final KnowledgeBase knowledgeBase;
	private final Map<URI, MyKnowledgeInteractionInfo> kiis = new ConcurrentHashMap<>();
	private final List<KnowledgeBaseStoreListener> listeners = new CopyOnWriteArrayList<>();

	public KnowledgeBaseStoreImpl(LoggerProvider loggerProvider, KnowledgeBase knowledgeBase) {
		this.LOG = loggerProvider.getLogger(KnowledgeBaseStoreImpl.class);
		this.knowledgeBase = knowledgeBase;
	}

	@Override
	public URI getKnowledgeBaseId() {
		return this.knowledgeBase.getKnowledgeBaseId();
	}

	@Override
	public String getKnowledgeBaseName() {
		return this.knowledgeBase.getKnowledgeBaseName();
	}

	@Override
	public String getKnowledgeBaseDescription() {
		return this.knowledgeBase.getKnowledgeBaseDescription();
	}

	@Override
	public KnowledgeInteractionInfo getKnowledgeInteractionById(URI id) {
		return this.kiis.get(id);
	}

	@Override
	public MyKnowledgeInteractionInfo getKnowledgeInteractionByObject(KnowledgeInteraction ki) {

		return this.kiis.values().stream().filter((storedKI) -> storedKI.getKnowledgeInteraction() == ki).findFirst()
				.orElse(null);
	}

	@Override
	public Set<MyKnowledgeInteractionInfo> getKnowledgeInteractions() {
		return new HashSet<>(this.kiis.values());
	}

	@Override
	public Set<MyKnowledgeInteractionInfo> getAskKnowledgeInteractions() {
		return this.kiis.values().stream().filter(ki -> ki.getType() == Type.ASK).collect(Collectors.toSet());
	}

	@Override
	public Set<MyKnowledgeInteractionInfo> getAnswerKnowledgeInteractions() {
		return this.kiis.values().stream().filter(ki -> ki.getType() == Type.ANSWER).collect(Collectors.toSet());
	}

	@Override
	public Set<MyKnowledgeInteractionInfo> getPostKnowledgeInteractions() {
		return this.kiis.values().stream().filter(ki -> ki.getType() == Type.POST).collect(Collectors.toSet());
	}

	@Override
	public Set<MyKnowledgeInteractionInfo> getReactKnowledgeInteractions() {
		return this.kiis.values().stream().filter(ki -> ki.getType() == Type.REACT).collect(Collectors.toSet());
	}

	@Override
	public AnswerHandler getAnswerHandler(URI anAnswerKiId) {
		MyKnowledgeInteractionInfo info = this.kiis.get(anAnswerKiId);
		if (info == null) {
			return null;
		} else {
			return info.getAnswerHandler();
		}
	}

	@Override
	public ReactHandler getReactHandler(URI anReactKiId) {
		MyKnowledgeInteractionInfo info = this.kiis.get(anReactKiId);
		if (info == null) {
			return null;
		} else {
			return info.getReactHandler();
		}
	}

	@Override
	public void addListener(KnowledgeBaseStoreListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(KnowledgeBaseStoreListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public URI register(AskKnowledgeInteraction anAskKI, boolean isMeta) {
		URI id = this.generateId(anAskKI, isMeta);
		MyKnowledgeInteractionInfo kii = new MyKnowledgeInteractionInfo(id, this.getKnowledgeBaseId(), anAskKI, null,
				null, isMeta);
		this.kiis.put(id, kii);
		this.listeners.forEach(l -> l.knowledgeInteractionRegistered(kii));
		return id;
	}

	@Override
	public void unregister(AskKnowledgeInteraction anAskKI) {
		this.kiis.entrySet().stream().filter(e -> e.getValue().getKnowledgeInteraction() == anAskKI).findFirst()
				.ifPresent(e -> {
					this.kiis.remove(e.getKey());
					this.listeners.forEach(l -> l.knowledgeInteractionUnregistered(e.getValue()));
				});
	}

	@Override
	public URI register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler anAnswerHandler, boolean isMeta) {
		URI id = this.generateId(anAnswerKI, isMeta);
		MyKnowledgeInteractionInfo kii = new MyKnowledgeInteractionInfo(id, this.getKnowledgeBaseId(), anAnswerKI,
				anAnswerHandler, null, isMeta);
		this.kiis.put(id, kii);
		this.listeners.forEach(l -> l.knowledgeInteractionRegistered(kii));
		return id;
	}

	@Override
	public void unregister(AnswerKnowledgeInteraction anAnswerKI) {
		this.kiis.entrySet().stream().filter(e -> e.getValue().getKnowledgeInteraction() == anAnswerKI).findFirst()
				.ifPresent(e -> {
					this.kiis.remove(e.getKey());
					this.listeners.forEach(l -> l.knowledgeInteractionUnregistered(e.getValue()));
				});
	}

	@Override
	public URI register(PostKnowledgeInteraction aPostKI, boolean isMeta) {
		URI id = this.generateId(aPostKI, isMeta);
		MyKnowledgeInteractionInfo kii = new MyKnowledgeInteractionInfo(id, this.getKnowledgeBaseId(), aPostKI, null,
				null, isMeta);
		this.kiis.put(id, kii);
		this.listeners.forEach(l -> l.knowledgeInteractionRegistered(kii));
		return id;
	}

	@Override
	public void unregister(PostKnowledgeInteraction aPostKI) {
		this.kiis.entrySet().stream().filter(e -> e.getValue().getKnowledgeInteraction() == aPostKI).findFirst()
				.ifPresent(e -> {
					this.kiis.remove(e.getKey());
					this.listeners.forEach(l -> l.knowledgeInteractionUnregistered(e.getValue()));
				});
	}

	@Override
	public URI register(ReactKnowledgeInteraction anReactKI, ReactHandler aReactHandler, boolean isMeta) {
		URI id = this.generateId(anReactKI, isMeta);
		MyKnowledgeInteractionInfo kii = new MyKnowledgeInteractionInfo(id, this.getKnowledgeBaseId(), anReactKI, null,
				aReactHandler, isMeta);
		this.kiis.put(id, kii);
		this.listeners.forEach(l -> l.knowledgeInteractionRegistered(kii));
		return id;
	}

	@Override
	public void unregister(ReactKnowledgeInteraction anReactKI) {
		this.kiis.entrySet().stream().filter(e -> e.getValue().getKnowledgeInteraction() == anReactKI).findFirst()
				.ifPresent(e -> {
					this.kiis.remove(e.getKey());
					this.listeners.forEach(l -> l.knowledgeInteractionUnregistered(e.getValue()));
				});
	}

	private URI generateId(KnowledgeInteraction aKI, boolean isMeta) {
		try {
			if (isMeta) {
				if (aKI instanceof AskKnowledgeInteraction) {
					return this.getMetaId(this.knowledgeBase.getKnowledgeBaseId(), KnowledgeInteractionInfo.Type.ASK);
				} else if (aKI instanceof AnswerKnowledgeInteraction) {
					return this.getMetaId(this.knowledgeBase.getKnowledgeBaseId(), KnowledgeInteractionInfo.Type.ANSWER);
				} else {
					assert false : "Meta KI IDs for POST/REACT are currently not implemented.";
					return null;
				}
			} else {
				return new URI(this.knowledgeBase.getKnowledgeBaseId().toString() + "/interaction/"
					+ UUID.randomUUID().toString());
			}
		} catch (URISyntaxException e) {
			// This should not happen if knowledgeBaseId is correct
			assert false : "Could not generate URI for KnowledgeInteraction";
			return null;
		}
	}

	@Override
	public URI getMetaId(URI knowledgeBaseId, KnowledgeInteractionInfo.Type kiType) {
		try {
			switch (kiType) {
			case ASK:
				return new URI(knowledgeBaseId.toString() + ASK_SUFFIX);
			case ANSWER:
				return new URI(knowledgeBaseId.toString() + ANSWER_SUFFIX);
			case POST:
			case REACT:
			default:
				assert false : "Meta KI IDs for POST/REACT are currently not implemented.";
				return null;
			}
		} catch (URISyntaxException e) {
			assert false : "Could not generate URI for KnowledgeInteraction";
			return null;
		}
	}
}
