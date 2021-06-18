package eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.factories;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.MessagingApiService;
import eu.interconnectproject.knowledge_engine.smartconnector.runtime.KeRuntime;

public class MessagingApiServiceFactory {

	public static MessagingApiService getMessagingApi() {
		return KeRuntime.getMessageDispatcher().getRemoteSmartConnectorConnectionsManager().getMessageReceiver();
	}
}
