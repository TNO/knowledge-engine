package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.MockedKnowledgeBase;

@Tag("Long")
public class TestMetadataFromNormalKnowledgeInteraction {
	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;

	private static final Logger LOG = LoggerFactory.getLogger(TestMetadataFromNormalKnowledgeInteraction.class);

	public boolean kb2Received = false;

	@Test
	public void testPostReact() throws InterruptedException {

		KnowledgeNetwork kn = new KnowledgeNetwork();
		kb1 = new MockedKnowledgeBase("kb1");
		kn.addKB(kb1);
		kb2 = new MockedKnowledgeBase("kb2");
		kn.addKB(kb2);
		LOG.info("Before everyone is ready!");
		kn.sync();
		LOG.info("Everyone is ready!");

		// start registering
		var prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ke", Vocab.ONTO_URI);
		var metaGraphPattern = new GraphPattern(prefixes, "?kb rdf:type ke:KnowledgeBase .", "?kb ke:hasName ?name .",
				"?kb ke:hasDescription ?description .", "?kb ke:hasKnowledgeInteraction ?ki .",
				"?ki rdf:type ?kiType .", "?ki ke:isMeta ?isMeta .", "?ki ke:hasCommunicativeAct ?act .",
				"?act rdf:type ke:CommunicativeAct .", "?act ke:hasRequirement ?req .",
				"?act ke:hasSatisfaction ?sat .", "?req rdf:type ?reqType .", "?sat rdf:type ?satType .",
				"?ki ke:hasGraphPattern ?gp .", "?gp rdf:type ?patternType .", "?gp ke:hasPattern ?pattern .");
		PostKnowledgeInteraction ki1 = new PostKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.NEW_KNOWLEDGE_PURPOSE))),
				metaGraphPattern, null);
		kb1.register(ki1);

		ReactKnowledgeInteraction ki2 = new ReactKnowledgeInteraction(new CommunicativeAct(), metaGraphPattern, null);
		kb2.register(ki2, (anRKI, aReactExchangeInfo) -> {

			LOG.info("KB2 Reacting...");

			if (aReactExchangeInfo.getPostingKnowledgeInteractionId().toString()
					.startsWith(kb1.getKnowledgeBaseId().toString() + "/meta")) {
				LOG.info("Ignoring real meta KI...");
				return new BindingSet();
			}

			TestMetadataFromNormalKnowledgeInteraction.this.kb2Received = true;

			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			assertTrue(iter.hasNext(), "There should be at least a single binding.");
			Binding b = iter.next();

			assertEquals("<https://example.org/kb/ki>", b.get("ki"), "Binding of 'ki' should be correct.");
			assertEquals("<https://example.org/gp>", b.get("gp"), "Binding of 'gp' should be correct.");

			assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");

			return new BindingSet();
		});

		kn.sync();
		LOG.info("Everyone is up-to-date!");

		// start exchanging
		BindingSet bindingSet = new BindingSet();
		Binding binding = new Binding();
		binding.put("kb", "<https://example.org/kb>");
		binding.put("name", "\"KB name\"");
		binding.put("description", "\"KB description\"");
		binding.put("ki", "<https://example.org/kb/ki>");
		binding.put("kiType", "<https://w3id.org/knowledge-engine/PostKnowledgeInteraction>");
		binding.put("isMeta", "true");
		binding.put("act", "<https://example.org/act>");
		binding.put("req", "<https://example.org/req>");
		binding.put("sat", "<https://example.org/sat>");
		binding.put("reqType", "<https://w3id.org/knowledge-engine/InformPurpose>");
		binding.put("satType", "<https://w3id.org/knowledge-engine/InformPurpose>");
		binding.put("gp", "<https://example.org/gp>");
		binding.put("patternType", "https://w3id.org/knowledge-engine/ArgumentGraphPattern");
		binding.put("pattern",
				"?kb rdf:type kb:KnowledgeBase . ?kb kb:hasName ?name . ?kb kb:hasDescription ?description . ?kb kb:hasKnowledgeInteraction ?ki . ?ki rdf:type ?kiType . ?ki kb:isMeta ?isMeta . ?ki kb:hasCommunicativeAct ?act . ?act rdf:type kb:CommunicativeAct . ?act kb:hasRequirement ?req . ?act kb:hasSatisfaction ?sat . ?req rdf:type ?reqType . ?sat rdf:type ?satType . ?ki kb:hasGraphPattern ?gp . ?gp rdf:type ?patternType . ?gp kb:hasPattern ?pattern .");
		bindingSet.add(binding);

		try {
			PostResult result = kb1.post(ki1, bindingSet).get();

			assertTrue(this.kb2Received, "KB2 should have received the posted data.");
			BindingSet bs = result.getBindings();
			assertTrue(bs.isEmpty());

			LOG.info("After post!");
		} catch (Exception e) {
			LOG.error("Erorr", e);
		}
	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestPostReact2.class.getSimpleName());
		if (kb1 != null) {
			kb1.stop();
		} else {
			fail("KB1 should not be null!");
		}

		if (kb2 != null) {
			kb2.stop();
		} else {
			fail("KB2 should not be null!");
		}
	}
}
