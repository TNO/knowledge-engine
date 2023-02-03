package eu.knowledge.engine.examples.keo;

import java.net.URISyntaxException;

import org.eclipse.paho.mqttv5.common.MqttException;

import eu.knowledge.engine.smartconnector.api.KnowledgeBase;

public class KEODemo {
	public static void main(String[] args) throws MqttException, URISyntaxException {
		KnowledgeBase powerGateway = new PowerGateway("tcp://127.0.0.1:1883");
		KnowledgeBase powerUI = new PowerUI();

		// Next steps:
		// 1. Sending action (power limit) to device instead of reading events from device (communicative act validation)
		// 2. Send other simple messages such as frequency.
		// 3. More complex messages (lists, phases)
		// 4. Auto generate graph pattern from thing description
	}
}
