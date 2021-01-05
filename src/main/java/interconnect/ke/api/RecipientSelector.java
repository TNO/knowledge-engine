package interconnect.ke.api;

import java.net.URI;

import interconnect.ke.api.interaction.KnowledgeInteraction;

/**
 * An object of this class is responsible for deciding which
 * {@link KnowledgeBase}(s) is/are valid recipient(s) of a particular message.
 */
public class RecipientSelector {
	private final GraphPattern pattern;

	/**
	 * Create a new *wildcard* {@link RecipientSelector}. Which means that the
	 * message will be send to any knowledge base that has corresponding
	 * {@link KnowledgeInteraction}s.
	 */
	public RecipientSelector() {
		this.pattern = new GraphPattern("..."); // TODO
	}

	/**
	 * Create a new {@link RecipientSelector} with a specific {@link GraphPattern}
	 * that uses terminologie from the KnowledgeBase ontology to specify which
	 * {@link KnowledgeBase}s should receive this message.
	 * 
	 * @param aPattern The {@link GraphPattern} that selects {@link KnowledgeBase}s
	 *                 as recipients.
	 */
	public RecipientSelector(GraphPattern aPattern) {
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
		this.pattern = new GraphPattern("..."); // TODO
	}

	/**
	 * @return the {@link GraphPattern} that this {@link RecipientSelector} uses to
	 *         determine who to send a particular message to.
	 */
	public GraphPattern getPattern() {
		return pattern;
	}
}
