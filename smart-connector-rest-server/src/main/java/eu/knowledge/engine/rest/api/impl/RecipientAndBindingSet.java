package eu.knowledge.engine.rest.api.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import eu.knowledge.engine.smartconnector.api.RecipientSelector;

public class RecipientAndBindingSet {

	private static final String BINDING_SET = "bindingSet";
	private static final String RECIPIENT_SELECTOR = "recipientSelector";
	public RecipientSelector recipient;
	public List<Map<String, String>> bindingSet;

	public RecipientAndBindingSet(JsonNode aRecipientAndBindingSet) throws IllegalArgumentException {
		JsonNode recipientSelector = null;
		JsonNode bindingSet = null;

		if (aRecipientAndBindingSet.isObject()) {

			// we have a recipient selector and bindingset
			recipientSelector = aRecipientAndBindingSet.get(RECIPIENT_SELECTOR);
			bindingSet = aRecipientAndBindingSet.get(BINDING_SET);

			if (recipientSelector == null || bindingSet == null) {
				throw new IllegalArgumentException(
						"The JSON Object should contain both a recipientSelector and bindingSet key.");
			}
		} else if (aRecipientAndBindingSet.isArray()) {
			// we have only a binding set
			bindingSet = aRecipientAndBindingSet;
		} else {
			// we have neither, which is incorrect.
			throw new IllegalArgumentException("The JSON should be either an JSON Object or an JSON Array.");
		}

		try {
			// parse recipient
			if (recipientSelector != null) {
				this.recipient = parseRecipientSelectorJson(recipientSelector);
			} else {
				// there is no recipient given. We default to the wildcard!
				recipient = new RecipientSelector();
			}

			// parse bindingset
			this.bindingSet = parseBindingSetJson(bindingSet);
		} catch (IOException e) {
			throw new IllegalArgumentException("The BindingSet should be a valid JSON array with Bindings.");
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"The RecipientSelector should be a valid JSON object with a singleKnowledgeBase key that leads to a string value of a KnowledgeBaseId.");
		}
	}

	private RecipientSelector parseRecipientSelectorJson(JsonNode recipientSelector) {

		RecipientSelector recipient = null;
		if (recipientSelector.isObject()) {
			if (recipientSelector.has("singleKnowledgeBase")) {
				JsonNode singleKB = recipientSelector.get("singleKnowledgeBase");
				if (singleKB.isTextual()) {
					recipient = new RecipientSelector(URI.create(singleKB.asText()));
				} else {
					throw new IllegalArgumentException("singleKnowledgeBase key should lead to a JSON String.");
				}
			} else if (recipientSelector.has("knowledgeBases")) {
				JsonNode knowledgeBases = recipientSelector.get("knowledgeBases");
				if (knowledgeBases.isArray()) {
					List<URI> recipientUris = new ArrayList<>();
					knowledgeBases.elements().forEachRemaining(receivingKbNode -> {
						if (receivingKbNode.isTextual()) {
							recipientUris.add(URI.create(receivingKbNode.asText()));
						} else {
							throw new IllegalArgumentException("Elements in in the 'knowledgeBases' array in RecipientSelector should be JSON Strings.");
						}
					});
					recipient = new RecipientSelector(recipientUris);
				} else {
					throw new IllegalArgumentException("'knowledgeBases' property in RecipientSelector should be a JSON Array.");
				}
			} else {
				throw new IllegalArgumentException(
					"RecipientSelector should be a JSON Object with a 'knowledgeBases' or 'singleKnowledgeBase' (deprecated) property.");	
			}
		} else {
			throw new IllegalArgumentException(
					"RecipientSelector should be a JSON Object.");
		}

		return recipient;
	}

	private List<Map<String, String>> parseBindingSetJson(JsonNode bindingSet) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectReader listReader = mapper.readerFor(new TypeReference<List<Map<String, String>>>() {
		});
		return listReader.readValue(bindingSet);
	}

}
