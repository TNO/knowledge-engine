package eu.interconnectproject.knowledge_engine.knowledgedirectory.api.factories;

import eu.interconnectproject.knowledge_engine.knowledgedirectory.KerApiImpl;
import eu.interconnectproject.knowledge_engine.knowledgedirectory.api.KerApiService;

public class KerApiServiceFactory {
	private static final KerApiService service = new KerApiImpl();

	public static KerApiService getKerApi() {
		return service;
	}
}
