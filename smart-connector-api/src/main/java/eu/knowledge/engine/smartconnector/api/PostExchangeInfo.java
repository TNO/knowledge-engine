package eu.knowledge.engine.smartconnector.api;

import java.net.URI;
import java.time.Instant;

public class PostExchangeInfo extends ExchangeInfo {

	/**
	 * The bindings that were the arguments for this exchange. Note that the
	 * variable names used are those occuring in the graph pattern of the react
	 * knowledge base (previously these were the names occurring in the graph
	 * pattern of the posting knowledge base).
	 */
	private final BindingSet argument;

	/**
	 * The bindings that were the partial results of this exchange. Note that this
	 * only contains the bindings that the given knowledgebase reacted to and the
	 * variable names used are those known to the reacting knowledge base
	 * (previously it were the names of the posting knowledge base).
	 */
	private final BindingSet result;

	public PostExchangeInfo(Initiator initiator, URI knowledgeBaseId, URI knowledgeInteractionId, BindingSet argument,
			BindingSet result, Instant exchangeStart, Instant exchangeEnd, Status status, String failedMessage) {
		super(initiator, knowledgeBaseId, knowledgeInteractionId, exchangeStart, exchangeEnd, status, failedMessage);

		this.argument = argument;
		this.result = result;
	}

	public BindingSet getArgument() {
		return argument;
	}

	public BindingSet getResult() {
		return result;
	}

	@Override
	public String toString() {
		return "PostExchangeInfo [argument=" + argument + ", result=" + result + ", initiator=" + initiator
				+ ", knowledgeBaseId=" + knowledgeBaseId + ", knowledgeInteractionId=" + knowledgeInteractionId
				+ ", exchangeStart=" + exchangeStart + ", exchangeEnd=" + exchangeEnd + ", status=" + status
				+ ", failedMessage=" + failedMessage + "]";
	}

}
