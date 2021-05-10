package eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.factories;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.SmartConnectorManagementApiService;
import eu.interconnectproject.knowledge_engine.smartconnector.runtime.KeRuntime;

public class SmartConnectorManagementApiServiceFactory {

	public static SmartConnectorManagementApiService getSmartConnectorManagementApi() {
		return KeRuntime.getMessageDispatcher().getRemoteSmartConnectorConnectionsManager();
	}
}
