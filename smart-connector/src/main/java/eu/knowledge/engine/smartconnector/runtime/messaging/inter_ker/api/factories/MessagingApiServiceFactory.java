package eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.factories;

import java.util.HashMap;

import jakarta.servlet.ServletConfig;

import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.MessagingApiService;

public class MessagingApiServiceFactory {

	private static HashMap<Integer, MessagingApiService> services = new HashMap<>();

	public static void registerMessagingApiService(int port, MessagingApiService service) {
		services.put(port, service);
	}

	public static void unregisterMessagingApiService(int port) {
		services.remove(port);
	}

	public static MessagingApiService getMessagingApi(ServletConfig servletContext) {
		String port = servletContext.getInitParameter("port");
		return services.get(Integer.parseInt(port));
	}
}
