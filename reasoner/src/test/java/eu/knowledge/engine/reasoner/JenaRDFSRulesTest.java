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
import eu.knowledge.engine.reasoner.api.Util;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

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

		Set<BaseRule> rdfsRules = JenaRuleTest.convertRules(readRuleFile());

		for (BaseRule br : rdfsRules) {
			LOG.info("{}", br);
		}

		RuleStore rs = new RuleStore();
		rs.addRules(rdfsRules);

		String[] dataSplitted = readExampleRDF();

		// data rule
		DataBindingSetHandler aBindingSetHandler = new DataBindingSetHandler(
				new Table(new String[] { "s", "p", "o" }, dataSplitted));
		Rule r = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern("?s ?p ?o"))),
				aBindingSetHandler);
		rs.addRule(r);

		ProactiveRule startRule = new ProactiveRule(new HashSet<>(Arrays.asList(new TriplePattern("?s ?p ?o"))),
				new HashSet<>());
		rs.addRule(startRule);

		ReasonerPlan rp = new ReasonerPlan(rs, startRule);

		rs.printGraphVizCode(rp);

		BindingSet bs = new BindingSet();
		Binding b = new Binding();
		bs.add(b);

		TaskBoard tb;
		while ((tb = rp.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		BindingSet results2 = rp.getResults();
		Model m = Util.generateModel(new TriplePattern("?s ?p ?o"), results2);

		StmtIterator iter = m.listStatements();
		LOG.info("------------------------");
		while (iter.hasNext()) {
			Statement st = iter.next();
			LOG.info("{}", st);
		}

		LOG.info("------------------------");
		StmtIterator iter2 = m.listStatements(
				ResourceFactory.createResource("http://openfmri.s3.amazonaws.com/nidm.ttl#openfmri"), RDF.type,
				(Resource) null);
		int teller = 0;
		while (iter2.hasNext()) {
			LOG.info("{}", iter2.next());
			teller = teller + 1;
		}
		assertEquals(4, teller);
	}

	public String[] readExampleRDF() {
		Model m = ModelFactory.createDefaultModel();
		m.read(JenaRDFSRulesTest.class.getResourceAsStream("/example.rdf"), null, "TTL");

		StmtIterator iter = m.listStatements();

		StringBuilder triples = new StringBuilder();
		while (iter.hasNext()) {
			Statement next = iter.next();
			String str = pNode(next.getSubject().asNode()) + "," + pNode(next.getPredicate().asNode()) + ","
					+ pNode(next.getObject().asNode());
			triples.append(str + "|");
			LOG.info("{}", str);
		}
		return triples.toString().split("\\|");
	}

	public String pNode(Node n) {
		if (n.isURI()) {
			return "<" + n.getURI() + ">";
		} else if (n.isLiteral()) {
			return n.toString().replaceAll("\"", "\\\"");
		}
		return n.toString();
	}

	public String readRuleFile() {
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
