package eu.interconnectproject.knowledge_engine.examples.keo;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
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

import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.CommunicativeAct;
import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.PostKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorBuilder;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.Vocab;

public class KEODemo implements MqttCallback, KnowledgeBase {
	private static final Logger LOG = LoggerFactory.getLogger(KEODemo.class);

	private MqttConnectionOptions mqttConnectionOptions;
	private MqttAsyncClient mqttClient;

	private final URI knowledgeBaseId;
	private final SmartConnector sc;

	private PostKnowledgeInteraction pkiPower;

	private PrefixMappingMem prefixes;

	public KEODemo(String mqttURI, String topic) throws MqttException, URISyntaxException {
		this.setupMQTT(mqttURI);
		this.connectMQTT();
		this.subscribe(topic);

		this.prefixes = new PrefixMappingMem();
		this.prefixes.setNsPrefixes(PrefixMapping.Standard);
		this.prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		this.prefixes.setNsPrefix("sosa", "http://www.w3.org/ns/sosa/");
		this.prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");
		this.knowledgeBaseId = new URI("https://www.interconnectproject.eu/knowledgebase/example/keo");
		this.sc = SmartConnectorBuilder.newSmartConnector(this).create();
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


		var bindings = new BindingSet();

		// TODO: Parse the `message` and fill in the bindings for:
		//  - ?observation (can we use the message id?)
		//  - ?value (use message.data.power.number and message.data.power.scale)

		this.sc.post(this.pkiPower, bindings);
	}

	@Override
	public void mqttErrorOccurred(MqttException exception) {
		LOG.error("MQTT ERROR", exception);
	}

	@Override
	public URI getKnowledgeBaseId() {
		return this.knowledgeBaseId;
	}

	@Override
	public String getKnowledgeBaseName() {
		return "InterConnect KEO demo knowledge base";
	}

	@Override
	public String getKnowledgeBaseDescription() {
		return "A knowledge base that publishes power measurements.";
	}

	@Override
	public void smartConnectorReady(SmartConnector aSC) {
		this.pkiPower = new PostKnowledgeInteraction(
			new CommunicativeAct(),
			new GraphPattern(
				this.prefixes,
				"?observation rdf:type sosa:Observation ." +
				"?observation sosa:madeBySensor <https://www.interconnectproject.eu/knowledgebase/example/keo/eebus-submeter-1> ." +
				"?observation sosa:hasFeatureOfInterest <https://www.interconnectproject.eu/knowledgebase/example/keo/something-1> ." +
				"?observation sosa:observedProperty saref:Power ." +
				"?observation sosa:hasSimpleResult ?value ."
			),
			null
		);
		aSC.register(this.pkiPower);
	}

	@Override
	public void smartConnectorConnectionLost(SmartConnector aSC) {
		LOG.warn("Connection lost with smart connector.");
	}

	@Override
	public void smartConnectorConnectionRestored(SmartConnector aSC) {
		LOG.info("Connection with smart connector restored.");
	}

	@Override
	public void smartConnectorStopped(SmartConnector aSC) {
		LOG.info("Smart connector stopped.");
	}

	public static void main(String[] args) throws MqttException, URISyntaxException {
		var demo = new KEODemo("tcp://127.0.0.1:1883", "keo/json_api/from_eebus");
	}
}
