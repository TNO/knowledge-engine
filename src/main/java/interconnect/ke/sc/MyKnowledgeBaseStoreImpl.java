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
import org.slf4j.LoggerFactory;

import interconnect.ke.api.AnswerHandler;
import interconnect.ke.api.ReactHandler;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;
import interconnect.ke.sc.MyKnowledgeInteractionInfo.Type;

public class MyKnowledgeBaseStoreImpl implements MyKnowledgeBaseStore {

	private static final Logger LOG = LoggerFactory.getLogger(MyKnowledgeBaseStoreImpl.class);

	private URI knowledgeBaseId;
	private Map<URI, MyKnowledgeInteractionInfo> kiis = new ConcurrentHashMap<>();
	private List<MyKnowledgeBaseStoreListener> listeners = new CopyOnWriteArrayList<>();

	public MyKnowledgeBaseStoreImpl(URI knowledgeBaseId) {
		this.knowledgeBaseId = knowledgeBaseId;
	}

	private URI generateId(KnowledgeInteraction anAskKI) {
		try {
			return new URI(knowledgeBaseId.toString() + "/interaction/" + UUID.randomUUID().toString());
		} catch (URISyntaxException e) {
			// This should not happen in knowledgeBaseId is correct
			assert false : "Could not generate URI for KnowledeInteraction";
			return null;
		}
	}

	@Override
	public MyKnowledgeInteractionInfo getKnowledgeInteractionById(URI id) {
		return kiis.get(id);
	}

	@Override
	public Set<MyKnowledgeInteractionInfo> getKnowledgeInteractions() {
		return new HashSet<>(kiis.values());
	}

	@Override
	public Set<MyKnowledgeInteractionInfo> getAskKnowledgeInteractions() {
		return kiis.values().stream().filter(ki -> ki.getType() == Type.ASK).collect(Collectors.toSet());
	}

	@Override
	public Set<MyKnowledgeInteractionInfo> getAnswerKnowledgeInteractions() {
		return kiis.values().stream().filter(ki -> ki.getType() == Type.ANSWER).collect(Collectors.toSet());
	}

	@Override
	public Set<MyKnowledgeInteractionInfo> getPostKnowledgeInteractions() {
		return kiis.values().stream().filter(ki -> ki.getType() == Type.POST).collect(Collectors.toSet());
	}

	@Override
	public Set<MyKnowledgeInteractionInfo> getReactKnowledgeInteractions() {
		return kiis.values().stream().filter(ki -> ki.getType() == Type.REACT).collect(Collectors.toSet());
	}

	@Override
	public AnswerHandler getAnswerHandler(URI anAnswerKiId) {
		MyKnowledgeInteractionInfo info = kiis.get(anAnswerKiId);
		if (info == null) {
			return null;
		} else {
			return info.getAnswerHandler();
		}
	}

	@Override
	public ReactHandler getReactHandler(URI anReactKiId) {
		MyKnowledgeInteractionInfo info = kiis.get(anReactKiId);
		if (info == null) {
			return null;
		} else {
			return info.getReactHandler();
		}
	}

	@Override
	public void addListener(MyKnowledgeBaseStoreListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(MyKnowledgeBaseStoreListener listener) {
		listeners.remove(listener);
	}

	@Override
	public URI register(AskKnowledgeInteraction anAskKI) {
		URI id = generateId(anAskKI);
		MyKnowledgeInteractionInfo kii = new MyKnowledgeInteractionInfo(id, anAskKI, null, null);
		kiis.put(id, kii);
		listeners.forEach(l -> l.knowledgeInteractionRegistered(kii));
		return id;
	}

	@Override
	public void unregister(AskKnowledgeInteraction anAskKI) {
		kiis.entrySet().stream().filter(e -> e.getValue().getKnowledgeInteraction() == anAskKI).findFirst()
				.ifPresent((e) -> {
					kiis.remove(e.getKey());
					listeners.forEach(l -> l.knowledgeInteractionUnregistered(e.getValue()));
				});
	}

	@Override
	public URI register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler anAnswerHandler) {
		URI id = generateId(anAnswerKI);
		MyKnowledgeInteractionInfo kii = new MyKnowledgeInteractionInfo(id, anAnswerKI, anAnswerHandler, null);
		kiis.put(id, kii);
		listeners.forEach(l -> l.knowledgeInteractionRegistered(kii));
		return id;
	}

	@Override
	public void unregister(AnswerKnowledgeInteraction anAnswerKI) {
		kiis.entrySet().stream().filter(e -> e.getValue().getKnowledgeInteraction() == anAnswerKI).findFirst()
				.ifPresent((e) -> {
					kiis.remove(e.getKey());
					listeners.forEach(l -> l.knowledgeInteractionUnregistered(e.getValue()));
				});
	}

	@Override
	public URI register(PostKnowledgeInteraction aPostKI) {
		URI id = generateId(aPostKI);
		MyKnowledgeInteractionInfo kii = new MyKnowledgeInteractionInfo(id, aPostKI, null, null);
		kiis.put(id, kii);
		listeners.forEach(l -> l.knowledgeInteractionRegistered(kii));
		return id;
	}

	@Override
	public void unregister(PostKnowledgeInteraction aPostKI) {
		kiis.entrySet().stream().filter(e -> e.getValue().getKnowledgeInteraction() == aPostKI).findFirst()
				.ifPresent((e) -> {
					kiis.remove(e.getKey());
					listeners.forEach(l -> l.knowledgeInteractionUnregistered(e.getValue()));
				});
	}

	@Override
	public URI register(ReactKnowledgeInteraction anReactKI, ReactHandler aReactHandler) {
		URI id = generateId(anReactKI);
		MyKnowledgeInteractionInfo kii = new MyKnowledgeInteractionInfo(id, anReactKI, null, aReactHandler);
		kiis.put(id, kii);
		listeners.forEach(l -> l.knowledgeInteractionRegistered(kii));
		return id;
	}

	@Override
	public void unregister(ReactKnowledgeInteraction anReactKI) {
		kiis.entrySet().stream().filter(e -> e.getValue().getKnowledgeInteraction() == anReactKI).findFirst()
				.ifPresent((e) -> {
					kiis.remove(e.getKey());
					listeners.forEach(l -> l.knowledgeInteractionUnregistered(e.getValue()));
				});
	}

}
