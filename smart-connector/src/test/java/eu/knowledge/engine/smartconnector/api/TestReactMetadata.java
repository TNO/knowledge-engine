package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.javacc.ParseException;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.impl.Util;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;

public class TestReactMetadata {
	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;

	private static final Logger LOG = LoggerFactory.getLogger(TestReactMetadata.class);
	private static KnowledgeNetwork kn;

	@Test
	public void testRequestMetadata() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");

		kn = new KnowledgeNetwork();
		kb1 = new KnowledgeBaseImpl("kb1");
		kn.addKB(kb1);
		kb2 = new KnowledgeBaseImpl("kb2");
		kn.addKB(kb2);

		kn.sync();

		GraphPattern gp2 = new GraphPattern(prefixes, """
					?kb rdf:type kb:KnowledgeBase .
					?kb kb:hasName ?name .
					?kb kb:hasDescription ?description .
					?kb kb:hasKnowledgeInteraction ?ki .
					?ki rdf:type ?kiType .
					?ki kb:isMeta ?isMeta .
					?ki kb:hasCommunicativeAct ?act .
					?act rdf:type kb:CommunicativeAct .
					?act kb:hasRequirement ?req .
					?act kb:hasSatisfaction ?sat .
					?req rdf:type ?reqType .
					?sat rdf:type ?satType .
					?ki kb:hasGraphPattern ?gp .
					?gp rdf:type ?patternType .
					?gp kb:hasPattern ?pattern .
				""");

		var ki2 = new ReactKnowledgeInteraction(new CommunicativeAct(), gp2, null);
		kb2.register(ki2, new ReactHandler() {

			@Override
			public BindingSet react(ReactKnowledgeInteraction anRKI, ReactExchangeInfo aReactExchangeInfo) {
				var argument = aReactExchangeInfo.getArgumentBindings();
				Model m;
				try {
					m = Util.generateModel(anRKI.getArgument(), argument);
					m.setNsPrefixes(prefixes);
					var kbIter = m.listStatements(null, RDF.type, m.getResource(m.expandPrefix("kb:KnowledgeBase")));

					assertTrue(kbIter.hasNext());
					var kb = kbIter.next().getSubject();

					var kiIter = kb.listProperties(m.getProperty(m.expandPrefix("kb:hasKnowledgeInteraction")));

					assertTrue(kiIter.hasNext());

					boolean receivedNewKI = false;

					while (kiIter.hasNext()) {
						var ki = kiIter.next().getObject().asResource();

						var prop = m.getProperty(m.expandPrefix("kb:hasGraphPattern"));

						LOG.debug("KI: {}", ki);

						var gp = ki.getProperty(prop);

						if (gp != null) {
							var p = gp.getObject().asResource()
									.getRequiredProperty(m.getProperty(m.expandPrefix("kb:hasPattern"))).getObject();

							assertTrue(p.isLiteral());

							if (p.asLiteral().getString().contains("Measurement")) {

								LOG.info("Found KI: {}", p.asLiteral().getString());

								receivedNewKI = true;
							}
						}
					}

					assertTrue(receivedNewKI);
				} catch (ParseException e) {
					LOG.error("{}", e);
				}

				return new BindingSet();
			}

		});

		GraphPattern gp1 = new GraphPattern(prefixes, "?obs rdf:type saref:Measurement .",
				"?obs saref:hasTemp ?temp .");
		PostKnowledgeInteraction ki1 = new PostKnowledgeInteraction(new CommunicativeAct(), gp1, null);
		kb1.register(ki1);

		LOG.info("Waiting for up-to-date...");
		kn.sync();

		LOG.info("Finished, now closing!");
	}

	@AfterAll
	public static void cleanup() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", TestReactMetadata.class.getSimpleName());
		kn.stop().get();
	}
}
