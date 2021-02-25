package eu.interconnectproject.knowledge_engine.examples.keo;

import java.net.URISyntaxException;

import org.eclipse.paho.mqttv5.common.MqttException;

import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;

public class KEODemo {
	public static void main(String[] args) throws MqttException, URISyntaxException {
		KnowledgeBase powerGateway = new PowerGateway("tcp://127.0.0.1:1883", "keo/json_api/from_eebus");
		KnowledgeBase powerUI = new PowerUI();
	}
}
