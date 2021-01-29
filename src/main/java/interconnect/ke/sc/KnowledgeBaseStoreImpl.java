package interconnect.ke.sc;

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

import interconnect.ke.api.AnswerHandler;
import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.api.ReactHandler;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;
import interconnect.ke.sc.KnowledgeInteractionInfo.Type;

public class KnowledgeBaseStoreImpl implements KnowledgeBaseStore {

	private final Logger LOG;
	private final KnowledgeBase knowledgeBase;
	private final Map<URI, MyKnowledgeInteractionInfo> kiis = new ConcurrentHashMap<>();
	private final List<KnowledgeBaseStoreListener> listeners = new CopyOnWriteArrayList<>();

	public KnowledgeBaseStoreImpl(LoggerProvider loggerProvider, KnowledgeBase knowledgeBase) {
		this.LOG = loggerProvider.getLogger(KnowledgeBaseStoreImpl.class);
		this.knowledgeBase = knowledgeBase;
	}

	private URI generateId(KnowledgeInteraction anAskKI) {
		try {
			return new URI(this.knowledgeBase.getKnowledgeBaseId().toString() + "/interaction/"
					+ UUID.randomUUID().toString());
		} catch (URISyntaxException e) {
			// This should not happen in knowledgeBaseId is correct
			assert false : "Could not generate URI for KnowledeInteraction";
			return null;
		}
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
	public URI register(AskKnowledgeInteraction anAskKI) {
		URI id = this.generateId(anAskKI);
		MyKnowledgeInteractionInfo kii = new MyKnowledgeInteractionInfo(id, this.getKnowledgeBaseId(), anAskKI, null,
				null, false);
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
	public URI register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler anAnswerHandler) {
		URI id = this.generateId(anAnswerKI);
		MyKnowledgeInteractionInfo kii = new MyKnowledgeInteractionInfo(id, this.getKnowledgeBaseId(), anAnswerKI,
				anAnswerHandler, null, false);
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
	public URI register(PostKnowledgeInteraction aPostKI) {
		URI id = this.generateId(aPostKI);
		MyKnowledgeInteractionInfo kii = new MyKnowledgeInteractionInfo(id, this.getKnowledgeBaseId(), aPostKI, null,
				null, false);
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
	public URI register(ReactKnowledgeInteraction anReactKI, ReactHandler aReactHandler) {
		URI id = this.generateId(anReactKI);
		MyKnowledgeInteractionInfo kii = new MyKnowledgeInteractionInfo(id, this.getKnowledgeBaseId(), anReactKI, null,
				aReactHandler, false);
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

	@Override
	public void register(MyKnowledgeInteractionInfo aKI) {
		this.kiis.put(aKI.getId(), aKI);
	}

}
