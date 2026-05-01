package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.util.JenaRules;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

/**
 * This test is similar to the dcat example, which gives out of memory
 * exceptions due to enormous amounts of binding sets. With this test we hope to
 * investigate and maybe even resolve some of the performance issues.
 */
public class TestDCATAskAnswer {

	private static final Logger LOG = LoggerFactory.getLogger(TestDCATAskAnswer.class);

	private KnowledgeBaseImpl dcatKb;
	private AskKnowledgeInteraction askKI;

	private KnowledgeBaseImpl otherKb;

	private static KnowledgeNetwork kn;

	@Test
	public void test() throws InterruptedException, ExecutionException {

		kn = new KnowledgeNetwork();

		createDCATKb();
		createOtherKb();
		kn.addKB(this.dcatKb);
		kn.addKB(this.otherKb);

		kn.sync();

		AskResult result = dcatKb.ask(askKI, new BindingSet()).get();

		LOG.info("Result: {}", result.getBindings());

		assertFalse(result.getBindings().isEmpty());
	}

	public void createDCATKb() {

		this.dcatKb = new KnowledgeBaseImpl("dcat-kb");

		var prefixes = new PrefixMappingMem();
		prefixes.setNsPrefix("dcat", "http://www.w3.org/ns/dcat#");
		prefixes.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		prefixes.setNsPrefix("dcterms", "http://purl.org/dc/terms/");

		this.askKI = new AskKnowledgeInteraction(new CommunicativeAct(), new GraphPattern(prefixes, """
				?ds rdf:type dcat:DataService .
				?ds dcterms:description ?description .
				?ds dcterms:title ?title .
						"""), "dcat-ask", false, true, false, MatchStrategy.ULTRA_LEVEL);
		this.dcatKb.register(this.askKI);

		String jenaRules = """
				     @prefix dcat: <http://www.w3.org/ns/dcat#> .
				     @prefix ke: <https://w3id.org/knowledge-engine/> .
				     @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
				     @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
				     @prefix dcterms: <http://purl.org/dc/terms/> .
				     @prefix skos: <http://www.w3.org/2004/02/skos/core#> .

					 // DCAT facts
				     -> (ke:KnowledgeBase rdfs:subClassOf dcat:DataService ) .
				     -> (ke:hasName skos:exactMatch dcterms:title ) .
				     -> (ke:hasDescription skos:exactMatch dcterms:description ) .

					 // RDFS rules
				     [exactMatch: (?i ?p1 ?v) (?p1 skos:exactMatch ?p2) -> (?i ?p2 ?v)]
				     [subClass: (?i rdf:type ?t1) (?t1 rdfs:subClassOf ?t2) -> (?i rdf:type ?t2)]
				""";

// 		alternative less general (and probably faster) way of representing the same domain knowledge  
//		String jenaRules = """
//			     @prefix dcat: <http://www.w3.org/ns/dcat#> .
//			     @prefix ke: <https://w3id.org/knowledge-engine/> .
//			     @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
//			     @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
//			     @prefix dcterms: <http://purl.org/dc/terms/> .
//			     @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
//
//				 // KE DCAT rules
//			     [DCATDataService: (?i rdf:type ke:KnowledgeBase) -> (?i rdf:type dcat:DataService)]
//			     [DCATTitle: (?i ke:hasName ?v) -> (?i dcterms:title ?v)]
//			     [DCATDesc: (?i ke:hasDescription ?v) -> (?i dcterms:description ?v)]
//			""";

		var rules = JenaRules.convertJenaToKeRules(jenaRules);

		Set<Rule> theRules = new HashSet<>();
		for (BaseRule r : rules) {
			theRules.add((Rule) r);
		}

		this.dcatKb.setDomainKnowledge(theRules);

	}

	public void createOtherKb() {

		this.otherKb = new KnowledgeBaseImpl("other-kb");

		var postKI = new PostKnowledgeInteraction(new CommunicativeAct(), new GraphPattern("?s <test> ?p ."), null);

		this.otherKb.register(postKI);
	}

	@AfterAll
	public static void cleanup() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", TestAskAnswer5.class.getSimpleName());
		kn.stop().get();
	}

}
