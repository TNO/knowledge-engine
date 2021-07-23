package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import eu.interconnectproject.knowledge_engine.smartconnector.api.Binding;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AnswerMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AskMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.ErrorMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.KnowledgeMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.PostMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.ReactMessage;

/**
 * Good old boring and annoying static conversion class. Translates the
 * {@link KnowledgeMessage} classes from the Java API to the generated classes
 * from the OpenAPI specification and back.
 */
public class MessageConverter {

	public static AskMessage fromJson(
			eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.AskMessage msg)
			throws URISyntaxException {
		return new AskMessage(UUID.fromString(msg.getMessageId()), new URI(msg.getFromKnowledgeBase()),
				new URI(msg.getFromKnowledgeInteraction()), new URI(msg.getToKnowledgeBase()),
				new URI(msg.getToKnowledgeInteraction()), fromJson(msg.getBindingSet()));
	}

	public static eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.AskMessage toJson(
			AskMessage msg) {
		var result = new eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.AskMessage();
		result.setMessageId(msg.getMessageId().toString());
		result.setFromKnowledgeBase(msg.getFromKnowledgeBase().toString());
		result.setFromKnowledgeInteraction(msg.getFromKnowledgeInteraction().toString());
		result.setToKnowledgeBase(msg.getToKnowledgeBase().toString());
		result.setToKnowledgeInteraction(msg.getToKnowledgeInteraction().toString());
		result.setBindingSet(toJson(msg.getBindings()));
		result.setMessageNumber(0); // TODO Change when message numbers are implemented
		return result;
	}

	public static AnswerMessage fromJson(
			eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.AnswerMessage msg)
			throws URISyntaxException {
		return new AnswerMessage(UUID.fromString(msg.getMessageId()), new URI(msg.getFromKnowledgeBase()),
				new URI(msg.getFromKnowledgeInteraction()), new URI(msg.getToKnowledgeBase()),
				new URI(msg.getToKnowledgeInteraction()), UUID.fromString(msg.getReplyToAskMessage()),
				fromJson(msg.getBindingSet()));
	}

	public static eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.AnswerMessage toJson(
			AnswerMessage msg) {
		var result = new eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.AnswerMessage();
		result.setMessageId(msg.getMessageId().toString());
		result.setFromKnowledgeBase(msg.getFromKnowledgeBase().toString());
		result.setFromKnowledgeInteraction(msg.getFromKnowledgeInteraction().toString());
		result.setToKnowledgeBase(msg.getToKnowledgeBase().toString());
		result.setToKnowledgeInteraction(msg.getToKnowledgeInteraction().toString());
		result.setReplyToAskMessage(msg.getReplyToAskMessage().toString());
		result.setBindingSet(toJson(msg.getBindings()));
		result.setMessageNumber(0); // TODO Change when message numbers are implemented
		return result;
	}

	public static PostMessage fromJson(
			eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.PostMessage msg)
			throws URISyntaxException {
		return new PostMessage(UUID.fromString(msg.getMessageId()), new URI(msg.getFromKnowledgeBase()),
				new URI(msg.getFromKnowledgeInteraction()), new URI(msg.getToKnowledgeBase()),
				new URI(msg.getToKnowledgeInteraction()), fromJson(msg.getArgument()));
	}

	public static eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.PostMessage toJson(
			PostMessage msg) {
		var result = new eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.PostMessage();
		result.setMessageId(msg.getMessageId().toString());
		result.setFromKnowledgeBase(msg.getFromKnowledgeBase().toString());
		result.setFromKnowledgeInteraction(msg.getFromKnowledgeInteraction().toString());
		result.setToKnowledgeBase(msg.getToKnowledgeBase().toString());
		result.setToKnowledgeInteraction(msg.getToKnowledgeInteraction().toString());
		result.setArgument(toJson(msg.getArgument()));
		result.setMessageNumber(0); // TODO Change when message numbers are implemented
		return result;
	}

	public static ReactMessage fromJson(
			eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.ReactMessage msg)
			throws URISyntaxException {
		return new ReactMessage(UUID.fromString(msg.getMessageId()), new URI(msg.getFromKnowledgeBase()),
				new URI(msg.getFromKnowledgeInteraction()), new URI(msg.getToKnowledgeBase()),
				new URI(msg.getToKnowledgeInteraction()), UUID.fromString(msg.getReplyToPostMessage()),
				fromJson(msg.getResult()));
	}

	public static eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.ReactMessage toJson(
			ReactMessage msg) {
		var result = new eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.ReactMessage();
		result.setMessageId(msg.getMessageId().toString());
		result.setFromKnowledgeBase(msg.getFromKnowledgeBase().toString());
		result.setFromKnowledgeInteraction(msg.getFromKnowledgeInteraction().toString());
		result.setToKnowledgeBase(msg.getToKnowledgeBase().toString());
		result.setToKnowledgeInteraction(msg.getToKnowledgeInteraction().toString());
		result.setReplyToPostMessage(msg.getReplyToPostMessage().toString());
		result.setResult(toJson(msg.getResult()));
		result.setMessageNumber(0); // TODO Change when message numbers are implemented
		return result;
	}

	public static ErrorMessage fromJson(
			eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.ErrorMessage msg)
			throws URISyntaxException {
		return new ErrorMessage(UUID.fromString(msg.getMessageId()), new URI(msg.getFromKnowledgeBase()),
				new URI(msg.getFromKnowledgeInteraction()), new URI(msg.getToKnowledgeBase()),
				new URI(msg.getToKnowledgeInteraction()), UUID.fromString(msg.getReplyToMessage()),
				msg.getErrorMessage());
	}

	public static eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.ErrorMessage toJson(
			ErrorMessage msg) {
		var result = new eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.inter_ker.model.ErrorMessage();
		result.setMessageId(msg.getMessageId().toString());
		result.setFromKnowledgeBase(msg.getFromKnowledgeBase().toString());
		result.setFromKnowledgeInteraction(msg.getFromKnowledgeInteraction().toString());
		result.setToKnowledgeBase(msg.getToKnowledgeBase().toString());
		result.setToKnowledgeInteraction(msg.getToKnowledgeInteraction().toString());
		result.setReplyToMessage(msg.getReplyToMessage().toString());
		result.setErrorMessage(msg.getErrorMessage());
		return result;
	}

	public static Object toJson(KnowledgeMessage message) {
		if (message instanceof AskMessage) {
			return MessageConverter.toJson((AskMessage) message);
		} else if (message instanceof AnswerMessage) {
			return MessageConverter.toJson((AnswerMessage) message);
		} else if (message instanceof PostMessage) {
			return MessageConverter.toJson((PostMessage) message);
		} else if (message instanceof ReactMessage) {
			return MessageConverter.toJson((ReactMessage) message);
		} else if (message instanceof ErrorMessage) {
			return MessageConverter.toJson((ErrorMessage) message);
		} else {
			return null;
		}
	}

	private static BindingSet fromJson(List<Map<String, String>> input) {
		BindingSet bindingSet = new BindingSet();
		for (Map<String, String> map : input) {
			Binding binding = new Binding();
			for (Entry<String, String> e : map.entrySet()) {
				binding.put(e.getKey(), e.getValue());
			}
			bindingSet.add(binding);
		}
		return bindingSet;
	}

	private static List<Map<String, String>> toJson(BindingSet bindingSet) {
		var result = new ArrayList<Map<String, String>>();
		for (Binding binding : bindingSet) {
			HashMap<String, String> map = new HashMap<String, String>();
			for (String key : binding.getVariables()) {
				map.put(key, binding.get(key));
			}
			result.add(map);
		}
		return result;
	}
}
