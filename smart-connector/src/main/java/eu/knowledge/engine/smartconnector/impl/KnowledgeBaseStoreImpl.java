package eu.knowledge.engine.smartconnector.impl;

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

import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;

import eu.knowledge.engine.smartconnector.api.AnswerHandler;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.KnowledgeBase;
import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.ReactHandler;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.Vocab;
import eu.knowledge.engine.smartconnector.impl.KnowledgeInteractionInfo.Type;

public class KnowledgeBaseStoreImpl implements KnowledgeBaseStore {

	private static final String ASK_SUFFIX = "/meta/knowledgeinteractions/ask";
	private static final String ANSWER_SUFFIX = "/meta/knowledgeinteractions/answer";

	private static final String POST_NEW_SUFFIX = "/meta/knowledgeinteractions/post/new";
	private static final String POST_CHANGED_SUFFIX = "/meta/knowledgeinteractions/post/changed";
	private static final String POST_REMOVED_SUFFIX = "/meta/knowledgeinteractions/post/removed";

	private static final String REACT_NEW_SUFFIX = "/meta/knowledgeinteractions/react/new";
	private static final String REACT_CHANGED_SUFFIX = "/meta/knowledgeinteractions/react/changed";
	private static final String REACT_REMOVED_SUFFIX = "/meta/knowledgeinteractions/react/removed";

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
				null);
		this.tryPut(kii);
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
				anAnswerHandler, null);
		this.tryPut(kii);
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
				null);
		this.tryPut(kii);
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
				aReactHandler);
		this.tryPut(kii);
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

	private void tryPut(MyKnowledgeInteractionInfo kii) throws IllegalArgumentException {
		var existing = this.kiis.putIfAbsent(kii.id, kii);
		if (existing != null) {
			throw new IllegalArgumentException("A Knowledge Interaction with that URI was already registered.");
		}
	}

	private URI generateId(KnowledgeInteraction aKI, boolean isMeta) {
		try {
			if (isMeta) {
				if (aKI instanceof AskKnowledgeInteraction) {
					return this.getMetaId(this.knowledgeBase.getKnowledgeBaseId(), KnowledgeInteractionInfo.Type.ASK,
							null);
				} else if (aKI instanceof AnswerKnowledgeInteraction) {
					return this.getMetaId(this.knowledgeBase.getKnowledgeBaseId(), KnowledgeInteractionInfo.Type.ANSWER,
							null);
				} else if (aKI instanceof PostKnowledgeInteraction) {
					var satisfactionPurposes = aKI.getAct().getSatisfactionPurposes();
					assert satisfactionPurposes.size() == 1 : "POST KI must have exactly 1 satisfaction purpose.";
					var purpose = satisfactionPurposes.iterator().next();
					return this.getMetaId(this.knowledgeBase.getKnowledgeBaseId(), KnowledgeInteractionInfo.Type.POST,
							purpose);
				} else if (aKI instanceof ReactKnowledgeInteraction) {
					var requirementPurposes = aKI.getAct().getRequirementPurposes();
					assert requirementPurposes.size() == 1 : "REACT KI must have exactly 1 requirement purpose.";
					var purpose = requirementPurposes.iterator().next();
					return this.getMetaId(this.knowledgeBase.getKnowledgeBaseId(), KnowledgeInteractionInfo.Type.REACT,
							purpose);
				} else {
					assert false : "Meta KI IDs for POST/REACT are currently not implemented.";
					return null;
				}
			} else {
				if (aKI.getName() != null) {
					return new URI(this.getKnowledgeBaseId().toString() + "/interaction/" + aKI.getName());
				} else {
					return new URI(this.knowledgeBase.getKnowledgeBaseId().toString() + "/interaction/"
						+ UUID.randomUUID().toString());
				}
			}
		} catch (URISyntaxException e) {
			// This should not happen if knowledgeBaseId is correct
			assert false : "Could not generate URI for KnowledgeInteraction";
			return null;
		}
	}

	@Override
	public URI getMetaId(URI knowledgeBaseId, KnowledgeInteractionInfo.Type kiType, Resource purpose) {
		try {
			switch (kiType) {
			case ASK:
				return new URI(knowledgeBaseId.toString() + ASK_SUFFIX);
			case ANSWER:
				return new URI(knowledgeBaseId.toString() + ANSWER_SUFFIX);
			case POST:
				if (purpose.equals(Vocab.NEW_KNOWLEDGE_PURPOSE)) {
					return new URI(knowledgeBaseId.toString() + POST_NEW_SUFFIX);
				} else if (purpose.equals(Vocab.CHANGED_KNOWLEDGE_PURPOSE)) {
					return new URI(knowledgeBaseId.toString() + POST_CHANGED_SUFFIX);
				} else if (purpose.equals(Vocab.REMOVED_KNOWLEDGE_PURPOSE)) {
					return new URI(knowledgeBaseId.toString() + POST_REMOVED_SUFFIX);
				} else {
					assert false : "Invalid purpose for meta POST interaction: " + purpose;
					return null;
				}
			case REACT:
				if (purpose.equals(Vocab.NEW_KNOWLEDGE_PURPOSE)) {
					return new URI(knowledgeBaseId.toString() + REACT_NEW_SUFFIX);
				} else if (purpose.equals(Vocab.CHANGED_KNOWLEDGE_PURPOSE)) {
					return new URI(knowledgeBaseId.toString() + REACT_CHANGED_SUFFIX);
				} else if (purpose.equals(Vocab.REMOVED_KNOWLEDGE_PURPOSE)) {
					return new URI(knowledgeBaseId.toString() + REACT_REMOVED_SUFFIX);
				} else {
					assert false : "Invalid purpose for meta REACT interaction: " + purpose;
					return null;
				}
			default:
				assert false : "Unknown KnowledgeInteractionInfo.Type while generating meta ID.";
				return null;
			}
		} catch (URISyntaxException e) {
			assert false : "Could not generate URI for KnowledgeInteraction";
			return null;
		}
	}

	@Override
	public Resource getPurpose(URI knowledgeBaseId, URI knowledgeInteractionId) {
		String kbId = knowledgeBaseId.toString();
		String kiId = knowledgeInteractionId.toString();
		assert kiId.startsWith(kbId) : "Meta knowledge interaction should start with its knowledge base's ID.";

		String suffix = kiId.substring(kbId.length());

		if (suffix.equals(ASK_SUFFIX) || suffix.equals(ANSWER_SUFFIX)) {
			return Vocab.INFORM_PURPOSE;
		} else if (suffix.equals(POST_NEW_SUFFIX)) {
			return Vocab.NEW_KNOWLEDGE_PURPOSE;
		} else if (suffix.equals(POST_CHANGED_SUFFIX)) {
			return Vocab.CHANGED_KNOWLEDGE_PURPOSE;
		} else if (suffix.equals(POST_REMOVED_SUFFIX)) {
			return Vocab.REMOVED_KNOWLEDGE_PURPOSE;
		} else if (suffix.equals(REACT_NEW_SUFFIX)) {
			return Vocab.NEW_KNOWLEDGE_PURPOSE;
		} else if (suffix.equals(REACT_CHANGED_SUFFIX)) {
			return Vocab.CHANGED_KNOWLEDGE_PURPOSE;
		} else if (suffix.equals(REACT_REMOVED_SUFFIX)) {
			return Vocab.REMOVED_KNOWLEDGE_PURPOSE;
		}
		assert false : "Invalid suffix: " + suffix;
		return Vocab.PURPOSE;
	}

	@Override
	public void stop() {
		this.listeners.forEach(l -> l.smartConnectorStopping());
	}
}
