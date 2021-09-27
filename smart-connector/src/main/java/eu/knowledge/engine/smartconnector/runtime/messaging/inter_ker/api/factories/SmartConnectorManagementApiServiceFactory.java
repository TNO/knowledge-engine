package eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.factories;

import java.util.HashMap;

import javax.servlet.ServletConfig;

import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.SmartConnectorManagementApiService;

public class SmartConnectorManagementApiServiceFactory {

	private static HashMap<Integer, SmartConnectorManagementApiService> services = new HashMap<>();

	public static void registerSmartConnectorManagementApiService(int port,
			SmartConnectorManagementApiService service) {
		services.put(port, service);
	}

	public static void unregisterSmartConnectorManagementApiService(int port) {
		services.remove(port);
	}

	public static SmartConnectorManagementApiService getSmartConnectorManagementApi(ServletConfig servletContext) {
		String port = servletContext.getInitParameter("port");
		return services.get(Integer.parseInt(port));
	}
}
