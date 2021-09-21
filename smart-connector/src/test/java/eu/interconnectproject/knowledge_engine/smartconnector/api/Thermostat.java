package eu.interconnectproject.knowledge_engine.smartconnector.api;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.runtime.KeRuntime;

public class Thermostat {

	private static final Logger LOG = LoggerFactory.getLogger(Thermostat.class);

	private MockedKnowledgeBase sensor;
	private MockedKnowledgeBase thermostat;
	private MockedKnowledgeBase heating;
	private Room r;

	ExecutorService es = Executors.newFixedThreadPool(4);

	public static void main(String[] args) throws InterruptedException {

		Thermostat t = new Thermostat();

		t.start();

	}

	public void start() throws InterruptedException {
		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("sosa", "http://www.w3.org/ns/sosa/");
		prefixes.setNsPrefix("ic", "https://www.tno.nl/energy/ontology/interconnect/");
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		r = new Room();
		es.execute(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						r.temperature.decrementAndGet();
						Thread.sleep(7000);
					}
				} catch (InterruptedException e) {
					LOG.debug("", e);
				}
			}
		});

		// first add the relevant knowledge bases
		var kn = new KnowledgeNetwork();
		sensor = new MockedKnowledgeBase("temperatureSensor");
		kn.addKB(sensor);
		thermostat = new MockedKnowledgeBase("thermostat");
		kn.addKB(thermostat);
		heating = new MockedKnowledgeBase("heatingSource");
		kn.addKB(heating);
		LOG.info("Waiting for ready...");
		kn.startAndWaitForReady();

		// then register the relevant knowledge interactions
		GraphPattern obsGraphPattern = new GraphPattern(prefixes,
				"?id rdf:type ic:Room . \n" + "?id ic:hasName ?room . \n" + "?obs rdf:type sosa:Observation . \n"
						+ "?obs sosa:hasFeatureOfInterest ?id . \n" + "?obs sosa:observedProperty ic:Temperature . \n"
						+ "?obs sosa:hasSimpleResult ?temp .");
		PostKnowledgeInteraction sensorKI = new PostKnowledgeInteraction(new CommunicativeAct(), obsGraphPattern, null);
		sensor.register(sensorKI);

		GraphPattern actGraphPattern = new GraphPattern(prefixes,
				"?room rdf:type ic:Room . \n" + "?room ic:hasName ?roomName . \n" + "?act rdf:type sosa:Actuation . \n"
						+ "?act sosa:hasFeatureOfInterest ?room . \n" + "?act sosa:actsOnProperty ic:OnOffState . \n"
						+ "?act sosa:hasSimpleResult ?state .");
		PostKnowledgeInteraction thermostatPostKI = new PostKnowledgeInteraction(new CommunicativeAct(),
				actGraphPattern, null);
		thermostat.register(thermostatPostKI);

		ReactKnowledgeInteraction thermostatReactKI = new ReactKnowledgeInteraction(new CommunicativeAct(),
				obsGraphPattern, null);
		thermostat.register(thermostatReactKI, new ReactHandler() {
			@Override
			public BindingSet react(ReactKnowledgeInteraction anRKI, ReactExchangeInfo aReactExchangeInfo) {
				var argument = aReactExchangeInfo.getArgumentBindings();
				LOG.info("Reacting to sensor value.");
				Iterator<Binding> iterator = argument.iterator();
				assert iterator.hasNext();

				Binding b = iterator.next();
				String temp = b.get("temp");
				double tempDouble = Double.parseDouble(temp);

				double threshold = 20.5;

				BindingSet args = new BindingSet();
				Binding i = new Binding();
				i.put("room", "<https://www.tno.nl/example/room1>");
				i.put("roomName", "\"room1\"");
				i.put("act", "<https://www.tno.nl/example/act1>");
				if (tempDouble < threshold) {
					i.put("state", "\"on\"");
					LOG.info("Thermostat: posting state: {}", "on");
				} else if (tempDouble > threshold) {
					i.put("state", "\"off\"");
					LOG.info("Thermostat: posting state: {}", "off");
				}
				args.add(i);
				Thermostat.this.thermostat.post(thermostatPostKI, args);

				return null;
			}
		});

		HeatingRunnable heatingRunnable = new HeatingRunnable(this.r);
		ReactKnowledgeInteraction heatingReactKI = new ReactKnowledgeInteraction(new CommunicativeAct(),
				actGraphPattern, null);
		heating.register(heatingReactKI, new ReactHandler() {
			@Override
			public BindingSet react(ReactKnowledgeInteraction anRKI, ReactExchangeInfo aReactExchangeInfo) {
				var argument = aReactExchangeInfo.getArgumentBindings();
				Iterator<Binding> i = argument.iterator();
				assert i.hasNext();
				Binding b = i.next();
				String state = b.get("state");
				LOG.info("Heating: Received state: {}", state);
				heatingRunnable.turnedOn.set(state.equals("\"on\"") ? true : false);
				return null;
			}
		});
		kn.waitForUpToDate();

		// start the data exchange

		es.execute(heatingRunnable);

		es.execute(new Runnable() {
			BindingSet args = new BindingSet();
			Binding b = new Binding();

			@Override
			public void run() {

				try {
					while (true) {

						b.put("id", "<https://www.tno.nl/example/room1>");
						b.put("room", "\"room1\"");
						b.put("obs", "<https://www.tno.nl/example/obs1>");
						String temp = Integer.toString(Thermostat.this.r.temperature.get());
						b.put("temp", temp);
						args.add(b);
						Thermostat.this.sensor.post(sensorKI, args);
						LOG.info("Sensor: Posted temperature: {}", temp);

						Thread.sleep(5000);

					}
				} catch (InterruptedException e) {
					LOG.debug("", e);
				}
			}

		});

		es.awaitTermination(100, TimeUnit.SECONDS);
		LOG.info("Shutting down now.");
		es.shutdownNow();
		this.sensor.stop();
		this.thermostat.stop();
		this.heating.stop();

	}

	private static class Room {
		public volatile AtomicInteger temperature = new AtomicInteger(18);

	}

	private static class HeatingRunnable implements Runnable {

		public AtomicBoolean turnedOn = new AtomicBoolean(false);
		public Room room;

		public HeatingRunnable(Room r) {
			this.room = r;
		}

		@Override
		public void run() {
			try {
				while (true) {
					if (turnedOn.get()) {
						room.temperature.incrementAndGet();
					}
					Thread.sleep(3000);
				}
			} catch (InterruptedException e) {
				LOG.debug("", e);
			}
		}

	}
}
