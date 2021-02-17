package eu.interconnectproject.knowledge_engine.examples.keo;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KEODemo implements MqttCallback {
	private static final Logger LOG = LoggerFactory.getLogger(KEODemo.class);

	private MqttConnectionOptions mqttConnectionOptions;
	private MqttAsyncClient mqttClient;

	public KEODemo(String mqttURI, String topic) throws MqttException {
		this.setupMQTT(mqttURI);
		this.connectMQTT();
		this.subscribe(topic);
	}

	private void setupMQTT(String mqttURI) throws MqttException {
		// Issue client ID based on thread id.
		String clientID = "mqtt-client-" + Thread.currentThread().getId();

		LOG.info("Setting up MQTT client with id '{}'.", clientID);

		this.mqttClient = new MqttAsyncClient(mqttURI, clientID, null);
		this.mqttClient.setCallback(this);

		this.mqttConnectionOptions = new MqttConnectionOptions();
		this.mqttConnectionOptions.setServerURIs(new String[] { mqttURI });
	}

	private void connectMQTT() throws MqttException {
		LOG.info("Connecting to MQTT at {}", this.mqttConnectionOptions.getServerURIs()[0]);
		IMqttToken connectToken = this.mqttClient.connect(this.mqttConnectionOptions);
		connectToken.waitForCompletion(500);
	}

	private void subscribe(String topic) throws MqttException {
		LOG.info("Subscribing to topic '{}'.", topic);
		IMqttToken subToken = this.mqttClient.subscribe(topic, 0);
		subToken.waitForCompletion(500);
	}

	@Override
	public void authPacketArrived(int reasonCode, MqttProperties properties) {
		LOG.info("AUTH PACKET ARRIVED");
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		LOG.info("CONNECTED");
	}
	
	@Override
	public void deliveryComplete(IMqttToken token) {
		LOG.info("DELIVERED");
	}

	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		LOG.warn("DISCONNECTED");
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		LOG.info("MESSAGE ARRIVED in topic {}: {}", topic, message);
	}

	@Override
	public void mqttErrorOccurred(MqttException exception) {
		LOG.error("MQTT ERROR", exception);
	}

	public static void main(String[] args) throws MqttException {
		var demo = new KEODemo("tcp://127.0.0.1:1883", "keo/json_api/from_eebus");
	}
}
