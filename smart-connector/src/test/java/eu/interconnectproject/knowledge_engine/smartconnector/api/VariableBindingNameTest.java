package eu.interconnectproject.knowledge_engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class VariableBindingNameTest {

	private static final Logger LOG = LoggerFactory.getLogger(VariableBindingNameTest.class);

	private static MockedKnowledgeBase sensor;
	private static MockedKnowledgeBase thermostat;

	@Test
	void test() {
		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("sosa", "http://www.w3.org/ns/sosa/");
		prefixes.setNsPrefix("ic", "https://www.tno.nl/energy/ontology/interconnect/");
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		// first add the relevant knowledge bases
		var kn = new KnowledgeNetwork();
		sensor = new MockedKnowledgeBase("temperatureSensor");
		kn.addKB(sensor);
		thermostat = new MockedKnowledgeBase("thermostat");
		kn.addKB(thermostat);
		LOG.info("Waiting for ready...");
		kn.startAndWaitForReady();

		// then register the relevant knowledge interactions
		GraphPattern argGraphPattern1 = new GraphPattern(prefixes,
				"?id1 rdf:type ic:Room . \n" + "?id1 ic:hasName ?room1 . \n" + "?obs1 rdf:type sosa:Observation . \n"
						+ "?obs1 sosa:hasFeatureOfInterest ?id1 . \n"
						+ "?obs1 sosa:observedProperty ic:Temperature . \n" + "?obs1 sosa:hasSimpleResult ?temp1 .");
		GraphPattern resGraphPattern1 = new GraphPattern(prefixes, "?s1 ?p1 ?o1");
		PostKnowledgeInteraction sensorPostKI = new PostKnowledgeInteraction(new CommunicativeAct(), argGraphPattern1,
				resGraphPattern1);
		sensor.register(sensorPostKI);

		GraphPattern argGraphPattern2 = new GraphPattern(prefixes,
				"?id2 rdf:type ic:Room . \n" + "?id2 ic:hasName ?room2 . \n" + "?obs2 rdf:type sosa:Observation . \n"
						+ "?obs2 sosa:hasFeatureOfInterest ?id2 . \n"
						+ "?obs2 sosa:observedProperty ic:Temperature . \n" + "?obs2 sosa:hasSimpleResult ?temp2 .");

		GraphPattern resGraphPattern2 = new GraphPattern(prefixes, "?s2 ?p2 ?o2");

		ReactKnowledgeInteraction thermostatReactKI = new ReactKnowledgeInteraction(new CommunicativeAct(),
				argGraphPattern2, resGraphPattern2);
		thermostat.register(thermostatReactKI, new ReactHandler() {
			@Override
			public BindingSet react(ReactKnowledgeInteraction anRKI, BindingSet argument) {

				LOG.info("Reacting to sensor value.");
				Iterator<Binding> iterator = argument.iterator();
				assert iterator.hasNext();

				Binding b = iterator.next();

				assertFalse(b.containsKey("temp1"));
				assertTrue(b.containsKey("temp2"));

				String temp = b.get("temp2");

				assertEquals("21.5", temp);

				BindingSet bs = new BindingSet();
				Binding binding = new Binding();
				binding.put("s2", "<https://www.tno.nl/example/subject>");
				binding.put("p2", "<https://www.tno.nl/example/predicate>");
				binding.put("o2", "<https://www.tno.nl/example/object>");

				bs.add(binding);

				return bs;
			}
		});

		kn.waitForUpToDate();

		// data exchange

		BindingSet bs = new BindingSet();
		Binding b = new Binding();
		b.put("id1", "<https://www.tno.nl/example/room1>");
		b.put("room1", "\"room1\"");
		b.put("obs1", "<https://www.tno.nl/example/obs1>");
		String temp = Double.toString(21.5);
		b.put("temp1", temp);
		bs.add(b);
		try {
			PostResult postResult = this.sensor.post(sensorPostKI, bs).get();

			Set<PostExchangeInfo> infos = postResult.getExchangeInfoPerKnowledgeBase();

			PostExchangeInfo info = infos.stream()
					.filter(ei -> ei.getKnowledgeBaseId().equals(thermostat.getKnowledgeBaseId())).findFirst().get();

			BindingSet bindingSet = info.getArgument();

			var iter = bindingSet.iterator();

			assertTrue(iter.hasNext());

			Binding binding = iter.next();
			// the exchange infos should use the variable names from the receiving end.

			assertTrue(binding.containsKey("id1"));
			assertTrue(binding.containsKey("room1"));
			assertTrue(binding.containsKey("obs1"));
			assertTrue(binding.containsKey("temp1"));

			assertFalse(binding.containsKey("id2"));
			assertFalse(binding.containsKey("room2"));
			assertFalse(binding.containsKey("obs2"));
			assertFalse(binding.containsKey("temp2"));

			BindingSet bindingSet2 = info.getResult();
			var iter2 = bindingSet2.iterator();
			assertTrue(iter2.hasNext());
			Binding b2 = iter2.next();

			assertTrue(b2.containsKey("s1"));
			assertTrue(b2.containsKey("p1"));
			assertTrue(b2.containsKey("o1"));
			assertFalse(b2.containsKey("s2"));
			assertFalse(b2.containsKey("p2"));
			assertFalse(b2.containsKey("o2"));

			assertEquals("<https://www.tno.nl/example/subject>", b2.get("s1"));
			assertEquals("<https://www.tno.nl/example/predicate>", b2.get("p1"));
			assertEquals("<https://www.tno.nl/example/object>", b2.get("o1"));
		} catch (InterruptedException | ExecutionException e) {
			LOG.error("{}", e);
		}

	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", VariableBindingNameTest.class.getSimpleName());
		if (sensor != null) {
			sensor.stop();
		} else {
			fail("Sensor should not be null!");
		}

		if (thermostat != null) {

			thermostat.stop();
		} else {
			fail("Thermostat should not be null!");
		}
	}
}
