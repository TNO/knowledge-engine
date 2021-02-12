package interconnect.ke.api;

import java.net.URI;
import java.time.Instant;

import interconnect.ke.api.binding.BindingSet;

public class PostExchangeInfo extends ExchangeInfo {

	/**
	 * The bindings that were the arguments for this exchange.
	 */
	private final BindingSet argument;

	/**
	 * The bindings that were the partial results of this exchange. Note that this
	 * only contains the bindings that the given knowledgebase reacted.
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
