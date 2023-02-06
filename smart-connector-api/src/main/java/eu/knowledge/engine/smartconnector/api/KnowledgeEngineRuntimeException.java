package eu.knowledge.engine.smartconnector.api;

/**
 * A Knowledge Engine specific exception that can be used to wrap multiple other
 * exceptions.
 */
public class KnowledgeEngineRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public KnowledgeEngineRuntimeException(String msg) {
		super(msg);
	}

}
