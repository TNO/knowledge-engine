package eu.interconnectproject.knowledge_engine.knowledgedirectory.api.factories;

import eu.interconnectproject.knowledge_engine.knowledgedirectory.ScrApiImpl;
import eu.interconnectproject.knowledge_engine.knowledgedirectory.api.ScrApiService;

public class ScrApiServiceFactory {
	private static final ScrApiService service = new ScrApiImpl();

	public static ScrApiService getScrApi() {
		return service;
	}
}
