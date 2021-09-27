package eu.knowledge.engine.knowledgedirectory.api.factories;

import eu.knowledge.engine.knowledgedirectory.api.KerApiService;
import eu.knowledge.engine.knowledgedirectory.KerApiImpl;

public class KerApiServiceFactory {
	private static final KerApiService service = new KerApiImpl();

	public static KerApiService getKerApi() {
		return service;
	}
}
