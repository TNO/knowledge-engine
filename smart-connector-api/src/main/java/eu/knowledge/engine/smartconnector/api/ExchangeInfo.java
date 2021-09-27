package eu.knowledge.engine.smartconnector.api;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

public class ExchangeInfo {

	public static enum Initiator {
		KNOWLEDGEBASE, REASONER
	}

	public static enum Status {
		SUCCEEDED, FAILED
	}

	/**
	 * Was this ExchangeInfo object initiated by the {@link Initiator#REASONER} or
	 * by the {@link Initiator#KNOWLEDGEBASE}
	 */
	protected final Initiator initiator;
	/**
	 * The KnowledgeBaseId from the knowledgebase who reacted. Via the
	 * KnowledgeBaseId (in the future) you have access to version info about the
	 * smart connector and protocol.
	 */
	protected final URI knowledgeBaseId;
	/**
	 * The KnowledgeInteractionId that reacted.
	 */
	protected final URI knowledgeInteractionId;

	/**
	 * The start time for this exchange.
	 */
	protected final Instant exchangeStart;

	/**
	 * The endtime for this exchange.
	 */
	protected final Instant exchangeEnd;
	/**
	 * Whether this exchange {@link Status#SUCCEEDED} or {@link Status#FAILED}. If
	 * {@link Status#FAILED}, the failedMessage string is instantiated, {@code null}
	 * otherwise.
	 */
	protected final Status status;
	/**
	 * Only non-null if the status is {@link Status#FAILED}
	 */
	protected final String failedMessage;

	public ExchangeInfo(Initiator initiator, URI knowledgeBaseId, URI knowledgeInteractionId, Instant exchangeStart,
			Instant exchangeEnd, Status status, String failedMessage) {
		this.initiator = initiator;
		this.knowledgeBaseId = knowledgeBaseId;
		this.knowledgeInteractionId = knowledgeInteractionId;
		this.exchangeStart = exchangeStart;
		this.exchangeEnd = exchangeEnd;
		this.status = status;
		this.failedMessage = failedMessage;
	}

	public Initiator getInitiator() {
		return initiator;
	}

	public URI getKnowledgeBaseId() {
		return knowledgeBaseId;
	}

	public URI getKnowledgeInteractionId() {
		return knowledgeInteractionId;
	}

	public Duration getExchangeTime() {
		return Duration.between(exchangeStart, exchangeEnd);
	}

	public Status getStatus() {
		return status;
	}

	public String getFailedMessage() {
		return failedMessage;
	}

	public Instant getExchangeStart() {
		return exchangeStart;
	}

	public Instant getExchangeEnd() {
		return exchangeEnd;
	}

	@Override
	public String toString() {
		return "ExchangeInfo [initiator=" + initiator + ", knowledgeBaseId=" + knowledgeBaseId
				+ ", knowledgeInteractionId=" + knowledgeInteractionId + ", exchangeStart=" + exchangeStart
				+ ", exchangeEnd=" + exchangeEnd + ", status=" + status + ", failedMessage=" + failedMessage + "]";
	}

}