package eu.knowledge.engine.examples.keo;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.sse.SSE;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeBase;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.api.Vocab;
import eu.knowledge.engine.smartconnector.impl.SmartConnectorBuilder;

public class PowerGateway implements MqttCallback, KnowledgeBase {
	private static final Logger LOG = LoggerFactory.getLogger(PowerGateway.class);

	private MqttConnectionOptions mqttConnectionOptions;
	private MqttAsyncClient mqttClient;

	private final URI knowledgeBaseId;
	private final SmartConnector sc;

	private PostKnowledgeInteraction pkiPower;
	private ReactKnowledgeInteraction rkiPowerLimit;

	private PrefixMappingMem prefixes;

	private static final String SUB_TOPIC = "keo/json_api/from_eebus";
	private static final String PUB_TOPIC = "keo/json_api/to_eebus";

	private static final String EX_DATA = "https://www.interconnectproject.eu/knowledge-engine/data/example/keo/";

	public PowerGateway(String mqttURI) throws MqttException, URISyntaxException {
		this.setupMQTT(mqttURI);
		this.connectMQTT();
		this.subscribe(SUB_TOPIC);

		this.prefixes = new PrefixMappingMem();
		this.prefixes.setNsPrefixes(PrefixMapping.Standard);
		this.prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		this.prefixes.setNsPrefix("om", "http://www.ontology-of-units-of-measure.org/resource/om-2/");
		this.prefixes.setNsPrefix("sosa", "http://www.w3.org/ns/sosa/");
		this.prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");
		this.prefixes.setNsPrefix("interconnect", "http://ontology.tno.nl/Interconnect#");
		this.prefixes.setNsPrefix("ex-data", EX_DATA);

		this.knowledgeBaseId = new URI(
				"https://www.interconnectproject.eu/knowledge-engine/knowledgebase/example/power-gateway");

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

		var jsonObj = new JSONObject(message.toString());

		if (!jsonObj.getString("type").equals("de.keo-connectivity.generic.mpc.powerTotal")) {
			LOG.info("Ignoring message type '{}'", jsonObj.getString("type"));
			return;
		}

		String msgId = jsonObj.getString("id");
		
		int number = jsonObj.getJSONObject("data").getJSONObject("power").getInt("number");
		int scale = jsonObj.getJSONObject("data").getJSONObject("power").getInt("scale");
		String actorId = jsonObj.getJSONObject("data").getString("actorId");
		double value = number * Math.pow(10, scale);
		
		var bindings = new BindingSet();
		var binding = new Binding();

		binding.put("sensor", "<" + EX_DATA + "actor-" + actorId + ">");
		binding.put("observation", "<" + EX_DATA + "observation-" + msgId + ">");
		binding.put("result", "<" + EX_DATA + "result-" + msgId + ">");
		binding.put("value", "\"" + Double.toString(value) + "\"^^<http://www.w3.org/2001/XMLSchema#float>");
		// We record the current time. This is not necessarily the time of the
		// observation.
		binding.put("time", "\"" + ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
				+ "\"^^<http://www.w3.org/2001/XMLSchema#dateTime>");

		bindings.add(binding);

		LOG.info("Posting binding: {}", binding);

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
		LOG.info("Smart connector ready.");
		this.pkiPower = new PostKnowledgeInteraction(
			new CommunicativeAct(),
			new GraphPattern(this.prefixes,
				"?observation rdf:type sosa:Observation .",
				"?observation sosa:madeBySensor ?sensor .",
				"?observation sosa:observedProperty saref:Power .",
				"?observation sosa:hasResult ?result .",
				"?observation sosa:resultTime ?time .",
				"?result om:hasNumericalValue ?value .",
				"?result om:hasUnit om:watt ."
			),
			null
		);
		this.sc.register(this.pkiPower);

		// The following KI listens for actuation commands.
		this.rkiPowerLimit = new ReactKnowledgeInteraction(
			new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.PURPOSE)), new HashSet<>(Arrays.asList(Vocab.ACTUATION_PURPOSE))),
			new GraphPattern(this.prefixes,
				"?limit om:hasUnit om:watt .",
				"?command rdf:type saref:SetLevelCommand .",
				"?command saref:actsUpon saref:PowerLimit .",
				"?limit om:hasNumericalValue ?limitValue .",
				"?command interconnect:SetsValue ?limit ."
			),
			null
		);

		// When receiving an actuation command, send it to EEBUS via the queue
		this.sc.register(this.rkiPowerLimit, (rki, aReactExchangeInfo) -> {
			var argument = aReactExchangeInfo.getArgumentBindings();
			try {
				var b = argument.iterator().next();
				var limit = (Float) SSE.parseNode(b.get("limitValue")).getLiteralValue();
				LOG.info("Setting limit at {}", limit);
				this.sendPowerLimitMessage(Math.round(limit * 100), -2, 5);
			} catch (MqttException e) {
				LOG.error("Could not send message to MQTT.", e);
			}
			return new BindingSet();
		});
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

	private void sendPowerLimitMessage(int number, int scale, int ttl) throws MqttException {
		// Send a message back saying we want to limit the power.
		var msg = new JSONObject();
		msg.put("type", "de.keo-connectivity.generic.lpc.powerLimit");
		msg.put("source", "PowerGateway Knowledge Base");
		msg.put("id", UUID.randomUUID().toString());
		msg.put("specversion", "1.0");
		var data = new JSONObject();
		data.put("actorId", "d:_n:KEO_json_grid_server/1/");
		var limit = new JSONObject();
		var limitValue = new JSONObject();
		limitValue.put("number", number);
		limitValue.put("scale", scale);
		limit.put("value", limitValue);
		limit.put("active", true);
		limit.put("ttl", ttl);
		data.put("limit", limit);
		msg.put("data", data);

		this.mqttClient.publish(PUB_TOPIC, msg.toString().getBytes(StandardCharsets.UTF_8), 0, false);
	}
}
