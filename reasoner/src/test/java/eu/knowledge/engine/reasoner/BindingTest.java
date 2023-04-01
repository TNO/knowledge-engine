package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class BindingTest {

	private static final Logger LOG = LoggerFactory.getLogger(BindingTest.class);

	@Test
	public void testBindingEqual() {
		Binding a = new Binding();
		Binding b = new Binding();

		assertEquals(a, b);

		a.put("x", "<some>");
		a.put("y", "<some>");
		b.put("x", "<some>");
		b.put("y", "<some>");

		assertEquals(a, b);
	}

	@Test
	public void testFruitfulVsActualMatches() {

		String gp1 = """
				?e <http://example.org/type> <http://example.org/Expression>
				?e ?hasFirstOrSecondNumber ?n1
				?n1 <http://example.org/type> <http://example.org/Number>
				?n1 <http://example.org/hasDigit> ?d1
				?d1 <http://example.org/hasPlace> ?p
				?d1 <http://example.org/hasActualDigit> ?ad1
				""";
		String gp2 = """
				?e <http://example.org/type> <http://example.org/Expression>
				?e <http://example.org/hasFirstNr> ?n1
				?n1 <http://example.org/type> <http://example.org/Number>
				?n1 <http://example.org/hasDigit> ?d1
				?d1 <http://example.org/hasPlace> \"1\"
				?d1 <http://example.org/hasActualDigit> ?ad1
				?e <http://example.org/hasSecondNr> ?n2
				?n2 <http://example.org/type> <http://example.org/Number>
				?n2 <http://example.org/hasDigit> ?d2
				?d2 <http://example.org/hasPlace> \"1\"
				?d2 <http://example.org/hasActualDigit> ?ad2
				""";

		BaseRule r = new BaseRule("test", toTriplePattern(gp2), new HashSet<>());

		Set<Match> matches = r.antecedentMatches(toTriplePattern(gp1), MatchStrategy.FIND_ALL_MATCHES);

		LOG.info("matches size: {}", matches.size());

		for (Match m1 : matches) {
			for (Match m2 : matches) {
				if (m1.getMatchingPatterns().size() == 6) {
					LOG.info("Found full match: {}", m1.getMatchingPatterns());
					break;
				}
			}
		}
	}

	private Set<TriplePattern> toTriplePattern(String gp) {
		String[] tps = gp.split("\n");
		Set<TriplePattern> newGp = new HashSet<>();
		for (String tp : tps) {
			newGp.add(new TriplePattern(tp));
		}
		return newGp;
	}

	@Test
	public void testTriplePatternMatch() {

		var tp1 = new TriplePattern("?e ?hasFirstOrSecondNumber ?n1");
		var tp2 = new TriplePattern("?n2 <http://example.org/hasDigit> ?d2");

		var tp3 = new TriplePattern("?n1 <http://example.org/hasDigit> ?d1");
		var tp4 = new TriplePattern("?n1 <http://example.org/hasDigit> ?d1");

		var m1 = new Match(tp1, tp2, tp1.findMatches(tp2));
		var m2 = new Match(tp3, tp4, tp3.findMatches(tp4));

		LOG.info("M1: {}", m1);
		LOG.info("M2: {}", m2);

		LOG.info("Merged: {}", m1.merge(m2));

		assertNull(m1.merge(m2));

	}
}
