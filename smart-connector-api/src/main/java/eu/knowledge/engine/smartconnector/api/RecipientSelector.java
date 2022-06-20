package eu.knowledge.engine.smartconnector.api;

import java.net.URI;

import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;

/**
 * An object of this class is responsible for deciding which
 * {@link KnowledgeBase}(s) is/are valid recipient(s) of a particular message.
 */
public class RecipientSelector {
	private final GraphPattern pattern;
	private final BindingSet bindings;

	/**
	 * Create a new *wildcard* {@link RecipientSelector}. Which means that the
	 * message will be send to any knowledge base that has corresponding
	 * {@link KnowledgeInteraction}s.
	 */
	public RecipientSelector() {
		this.bindings = new BindingSet();
		this.pattern = new GraphPattern(
				"?kb <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://w3id.org/knowledge-engine/KnowledgeBase> .");
	}

	/**
	 * Create a new {@link RecipientSelector} with a specific {@link GraphPattern}
	 * that uses terminologie from the KnowledgeBase ontology to specify which
	 * {@link KnowledgeBase}s should receive this message.
	 * 
	 * @param aPattern The {@link GraphPattern} that selects {@link KnowledgeBase}s
	 *                 as recipients.
	 */
	private RecipientSelector(GraphPattern aPattern) {
		this.bindings = new BindingSet();
		this.pattern = aPattern;
	}

	/**
	 * Create a new {@link RecipientSelector} with a specific {@link KnowledgeBase}
	 * as recipient. This allows a message to be send to a specific
	 * {@link KnowledgeBase}, assuming it is available in the network.
	 * 
	 * @param knowledgeBase The URI of the {@link KnowledgeBase} that should receive
	 *                      the message.
	 */
	public RecipientSelector(URI knowledgeBase) {
		this.bindings = new BindingSet();
		Binding binding = new Binding();
		binding.put("kb", "<" + knowledgeBase.toString() + ">");
		bindings.add(binding);
		this.pattern = new GraphPattern(
				"?kb <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://w3id.org/knowledge-engine/KnowledgeBase> .");
	}

	/**
	 * @return the {@link GraphPattern} that this {@link RecipientSelector} uses to
	 *         determine who to send a particular message to.
	 */
	public GraphPattern getPattern() {
		return pattern;
	}

	/**
	 * @return The {@link BindingSet} that is being used to instantiate the
	 *         variables in the {@link GraphPattern} returned by
	 *         {@link #getPattern()}.
	 */
	public BindingSet getBindingSet() {
		return bindings;
	}
}
