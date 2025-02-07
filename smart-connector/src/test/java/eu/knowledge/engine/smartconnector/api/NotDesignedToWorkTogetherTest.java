package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;

/**
 * This test tries to illustrate <i>forward compatibility</i>. We instantiate an
 * App KB and a Lamp1 KB that exchange data about the on/off state of the lamp.
 * When a new Lamp2 KB is introduced that does not support on/off because it is
 * dimmable, we should how the reasoner+domain knowledge combination is able to
 * make the app work with the new dimmable type of lamp although it was not
 * designed to work with those types.
 * 
 * @author nouwtb
 *
 */
public class NotDesignedToWorkTogetherTest {

	private static final Logger LOG = LoggerFactory.getLogger(NotDesignedToWorkTogetherTest.class);

	private static KnowledgeNetwork kn = new KnowledgeNetwork();
	private KnowledgeBaseImpl appKb = new KnowledgeBaseImpl("AppKB");
	private KnowledgeBaseImpl lamp1Kb = new KnowledgeBaseImpl("Lamp1KB");
	private KnowledgeBaseImpl lamp2Kb = new KnowledgeBaseImpl("Lamp2KB");
	private PrefixMapping prefixes = new PrefixMappingMem().setNsPrefix("ex", "http://example.org/")
			.setNsPrefix("time", "https://www.w3.org/TR/owl-time/")
			.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

	@Test
	public void test() throws InterruptedException {

		kn.addKB(appKb);
		kn.addKB(lamp1Kb);
		kn.addKB(lamp2Kb);

		final CountDownLatch latch = new CountDownLatch(2);

		GraphPattern lampGP = new GraphPattern(prefixes, "?l rdf:type ex:OnOffLamp .", "?l ex:isOn ?o .");
		PostKnowledgeInteraction appKbPost = new PostKnowledgeInteraction(new CommunicativeAct(), lampGP, null);
		appKb.register(appKbPost);

		HashSet<TriplePattern> rule1ant = new HashSet<>(Arrays.asList(
				new TriplePattern(
						"?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.org/OnOffLamp>"),
				new TriplePattern(
						"?s <http://example.org/isOn> \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean>")));
		HashSet<TriplePattern> rule1con = new HashSet<>(Arrays.asList(
				new TriplePattern(
						"?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.org/DimmableLamp>"),
				new TriplePattern("?s <http://example.org/hasBrightness> \"100\"")));
		HashSet<TriplePattern> rule2ant = new HashSet<>(Arrays.asList(
				new TriplePattern(
						"?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.org/OnOffLamp>"),
				new TriplePattern(
						"?s <http://example.org/isOn> \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean>")));
		HashSet<TriplePattern> rule2con = new HashSet<>(Arrays.asList(
				new TriplePattern(
						"?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.org/DimmableLamp>"),
				new TriplePattern("?s <http://example.org/hasBrightness> \"0\"")));

		Rule rule1 = new Rule(rule1ant, rule1con);
		Rule rule2 = new Rule(rule2ant, rule2con);
		appKb.setDomainKnowledge(new HashSet<>(Arrays.asList(rule1, rule2)));

		ReactKnowledgeInteraction lamp1KbReact = new ReactKnowledgeInteraction(new CommunicativeAct(), lampGP, null);
		lamp1Kb.register(lamp1KbReact, (ReactHandler) (aKI, aReactInfo) -> {

			Iterator<Binding> iterator = aReactInfo.getArgumentBindings().iterator();
			while (iterator.hasNext()) {
				Binding b = iterator.next();
				if (b.containsKey("l") && b.get("l").equals("<lamp1>")) {
					LOG.info("Turned lamp1 '{}'", b.get("o"));
					latch.countDown();
				}
			}
			return new BindingSet();
		});

		GraphPattern lamp2GP = new GraphPattern(prefixes, "?l rdf:type <http://example.org/DimmableLamp> .",
				"?l <http://example.org/hasBrightness> ?b .");
		ReactKnowledgeInteraction lamp2KbReact = new ReactKnowledgeInteraction(new CommunicativeAct(), lamp2GP, null);
		lamp2Kb.register(lamp2KbReact, (ReactHandler) (aKI, aReactInfo) -> {

			Iterator<Binding> iterator = aReactInfo.getArgumentBindings().iterator();
			while (iterator.hasNext()) {
				Binding b = iterator.next();
				if (b.containsKey("l") && b.get("l").equals("<lamp2>")) {
					LOG.info("Turned lamp2 '{}'", b.get("b"));
					latch.countDown();
				}
			}

			return new BindingSet();
		});

		kn.sync();

		BindingSet argument = new BindingSet();
		Binding binding = new Binding();
		binding.put("l", "<lamp1>");
		binding.put("o", "\"true\"^^<http://www.w3.org/2001/XMLSchema#boolean>");
		argument.add(binding);
		Binding binding2 = new Binding();
		binding2.put("l", "<lamp2>");
		binding2.put("o", "\"true\"^^<http://www.w3.org/2001/XMLSchema#boolean>");
		argument.add(binding2);
		PostPlan pp = appKb.planPost(appKbPost, new RecipientSelector());

		pp.getReasonerPlan().getStore().printGraphVizCode(pp.getReasonerPlan());
		pp.execute(argument);

		boolean allTouched = latch.await(3000, TimeUnit.MILLISECONDS);

		assertTrue(allTouched);

	}

	@AfterAll
	public static void close() {
		LOG.info("Clean up: {}", NotDesignedToWorkTogetherTest.class.getSimpleName());
		try {
			kn.stop().get();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error("Stopping the Knowledge Network should succeed: {}", e);
		}
	}

}
