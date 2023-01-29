package eu.knowledge.engine.reasoner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JenaRDFSRulesTest {

	private static final Logger LOG = LoggerFactory.getLogger(JenaRDFSRulesTest.class);

	@Test
	public void test() {

		Set<BaseRule> rdfsRules = JenaRuleTest.convertRules(readFile());

		for (BaseRule br : rdfsRules) {
			LOG.info("{}", br);
		}
	}

	public String readFile() {
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
