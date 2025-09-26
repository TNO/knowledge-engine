package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.reasoner.util.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.util.JenaRules;
import eu.knowledge.engine.reasoner.util.Table;

public class JenaRDFSRulesTest {

	private static final Logger LOG = LoggerFactory.getLogger(JenaRDFSRulesTest.class);

	/**
	 * Test RDF inference with our reasoner.
	 * 
	 * Example RDF from:
	 * https://notebook.community/nicholsn/niquery/notebooks/RDFS-Inference
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ParseException
	 */
	
	@Test
	public void test() throws InterruptedException, ExecutionException, ParseException {

		Set<BaseRule> rdfsRules = JenaRules.convertJenaToKeRules(readRuleFile());

		for (BaseRule br : rdfsRules) {
			LOG.info("{}", br);
		}

		RuleStore rs = new RuleStore();
		rs.addRules(rdfsRules);

		// sample data rule
		String[] sampleDataSplitted = readRDF("/example.rdf");
		DataBindingSetHandler aBindingSetHandler = new DataBindingSetHandler(
				new Table(new String[] { "s", "p", "o" }, sampleDataSplitted));
		String genericTriple = "?s ?p ?o";
		Rule r = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern(genericTriple))),
				aBindingSetHandler);
		rs.addRule(r);

		// prov data rule
		String[] provDataSplitted = readRDF("/prov.ttl");
		DataBindingSetHandler aBindingSetHandler2 = new DataBindingSetHandler(
				new Table(new String[] { "s", "p", "o" }, provDataSplitted));
		Rule r2 = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern(genericTriple))),
				aBindingSetHandler2);
		rs.addRule(r2);

		String query = "<http://openfmri.s3.amazonaws.com/nidm.ttl#openfmri> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o";
		ProactiveRule startRule = new ProactiveRule(new HashSet<>(Arrays.asList(new TriplePattern(genericTriple))),
				new HashSet<>());
		rs.addRule(startRule);

		ReasonerPlan rp = new ReasonerPlan(rs, startRule);

//		rs.printGraphVizCode(rp);

		BindingSet bs = new BindingSet();
		Binding b = new Binding();
		bs.add(b);

		TaskBoard tb;
		while ((tb = rp.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		BindingSet results2 = rp.getResults();

		Model m = ModelFactory.createDefaultModel();
		for (Binding binding : results2) {
			m.add(m.asStatement(Triple.create(binding.get("s"), binding.get("p"), binding.get("o"))));
		}

		StmtIterator iter = m.listStatements();
		LOG.info("------------------------");
		while (iter.hasNext()) {
			Statement st = iter.next();
			LOG.info("{}", st);
		}
		iter.close();

		StmtIterator iter2 = m.listStatements(
				ResourceFactory.createResource("http://openfmri.s3.amazonaws.com/nidm.ttl#openfmri"), RDF.type,
				(Resource) null);

		Set<Statement> actualStatements = new HashSet<>();
		while (iter2.hasNext()) {
			actualStatements.add(iter2.next());
		}

		Set<Statement> expectedStatements = new HashSet<>();
		expectedStatements.add(
				m.createStatement(ResourceFactory.createResource("http://openfmri.s3.amazonaws.com/nidm.ttl#openfmri"),
						RDF.type, ResourceFactory.createResource("http://purl.org/dc/dcmitype/Dataset")));
		expectedStatements.add(
				m.createStatement(ResourceFactory.createResource("http://openfmri.s3.amazonaws.com/nidm.ttl#openfmri"),
						RDF.type, ResourceFactory.createResource("http://www.incf.org/ns/nidash/nidm#Database")));
		expectedStatements.add(
				m.createStatement(ResourceFactory.createResource("http://openfmri.s3.amazonaws.com/nidm.ttl#openfmri"),
						RDF.type, ResourceFactory.createResource("http://www.w3.org/2000/01/rdf-schema#Resource")));
		expectedStatements.add(
				m.createStatement(ResourceFactory.createResource("http://openfmri.s3.amazonaws.com/nidm.ttl#openfmri"),
						RDF.type, ResourceFactory.createResource("http://www.w3.org/ns/prov#Collection")));
		expectedStatements.add(
				m.createStatement(ResourceFactory.createResource("http://openfmri.s3.amazonaws.com/nidm.ttl#openfmri"),
						RDF.type, ResourceFactory.createResource("http://www.w3.org/ns/prov#Entity")));

		assertEquals(expectedStatements, actualStatements);

//		Model m = Util.generateModel(new TriplePattern(genericTriple), results2);
//
//		StmtIterator iter = m.listStatements();
//		LOG.info("------------------------");
//		while (iter.hasNext()) {
//			Statement st = iter.next();
//			LOG.info("{}", st);
//		}
//		iter.close();
//
//		Model m2 = ModelFactory.createDefaultModel();
//		m2.read(JenaRDFSRulesTest.class.getResourceAsStream("/example.rdf"), null, "TTL");
//		m2.read(JenaRDFSRulesTest.class.getResourceAsStream("/prov.ttl"), null, "TTL");
//		InfModel im2 = ModelFactory.createInfModel(new RDFSForwardRuleReasoner(null), m2);
//		StmtIterator iter3 = im2.listStatements();
//		LOG.info("------------------------");
//		while (iter3.hasNext()) {
//			Statement st = iter3.next();
//			LOG.info("{} {} {} .", st.getSubject().asNode(), st.getPredicate().asNode(), st.getObject().asNode());
//		}
//		iter3.close();
//
//		// compare two models
//		Model m3 = m.difference(im2);
//		Model m4 = im2.difference(m);
//
//		LOG.info("Difference m<->im2: {}", m3.size());
//		LOG.info("Difference im2<->m: {}", m4.size());
//
//		LOG.info("------------------------");
//		StmtIterator iter2 = m3.listStatements(
//				ResourceFactory.createResource("http://openfmri.s3.amazonaws.com/nidm.ttl#openfmri"), RDF.type,
//				(Resource) null);
////		StmtIterator iter2 = m4.listStatements((Resource) null, null, (RDFNode) null);
//		int teller = 0;
//		while (iter2.hasNext()) {
//			LOG.info("{}", iter2.next());
//			teller = teller + 1;
//		}
//		assertEquals(5, teller);
	}

	public String[] readRDF(String name) {
		Model m = ModelFactory.createDefaultModel();
		m.read(JenaRDFSRulesTest.class.getResourceAsStream(name), null, "TTL");

		StmtIterator iter = m.listStatements();

		StringBuilder triples = new StringBuilder();
		while (iter.hasNext()) {
			Statement next = iter.next();
			String str = pNode(next.getSubject().asNode()) + "," + pNode(next.getPredicate().asNode()) + ","
					+ pNode(next.getObject().asNode());
			triples.append(str + "|");
		}
		return triples.toString().split("\\|");
	}

	public String pNode(Node n) {
		if (n.isURI()) {
			return "<" + n.getURI() + ">";
		} else if (n.isLiteral()) {

			String value = n.getLiteralLexicalForm().replaceAll("\n", "");
			value = value.replaceAll(",", "");
			value = value.replaceAll("\"", "\\\\\"");
			value = "\"" + value + "\"";
			if (n.getLiteralDatatypeURI() != null) {
				value = value + "^^<" + n.getLiteralDatatypeURI() + ">";
			}
			return value;
		} else if (n.isBlank()) {
			return "<" + n.getBlankNodeLabel() + ">";
		}
		return n.toString();
	}

	public static String readRuleFile() {
		BufferedReader br = new BufferedReader(
				new InputStreamReader(JenaRDFSRulesTest.class.getResourceAsStream("/rdfs.rules")));

		StringBuilder sb = new StringBuilder();
		String line;
		try {
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sb.toString();
	}

}
