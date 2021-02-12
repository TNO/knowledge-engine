package interconnect.ke.api;

import java.net.URI;
import java.time.Instant;

import interconnect.ke.api.binding.BindingSet;

public class AskExchangeInfo extends ExchangeInfo {

	/**
	 * The bindings that were the results of this exchange.
	 */
	private final BindingSet bindings;

	public AskExchangeInfo(Initiator initiator, URI knowledgeBaseId, URI knowledgeInteractionId, BindingSet bindings,
			Instant exchangeStart, Instant exchangeEnd, Status status, String failedMessage) {
		super(initiator, knowledgeBaseId, knowledgeInteractionId, exchangeStart, exchangeEnd, status, failedMessage);

		this.bindings = bindings;
	}

	public BindingSet getBindings() {
		return this.bindings;
	}

	@Override
	public String toString() {
		return "AskExchangeInfo [bindings=" + bindings + ", initiator=" + initiator + ", knowledgeBaseId="
				+ knowledgeBaseId + ", knowledgeInteractionId=" + knowledgeInteractionId + ", exchangeStart="
				+ exchangeStart + ", exchangeEnd=" + exchangeEnd + ", status=" + status + ", failedMessage="
				+ failedMessage + "]";
	}
}
