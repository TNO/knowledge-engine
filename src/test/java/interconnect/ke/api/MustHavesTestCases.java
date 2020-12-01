package interconnect.ke.api;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MustHavesTestCases {

	private static final Logger LOG = LoggerFactory.getLogger(MustHavesTestCases.class);

	@Test
	void testCaseM1() {
		SmartConnector sc1 = TestUtils.getSmartConnector("kb1");
		SmartConnector sc2 = TestUtils.getSmartConnector("kb2");

		final int value = 23;

		PostKnowledgeInteraction pKI = new PostKnowledgeInteraction(new CommunicativeAct(),
				TestUtils.SAREF_MEASUREMENT_PATTERN, null);
		ReactKnowledgeInteraction rKI = new ReactKnowledgeInteraction(new CommunicativeAct(),
				TestUtils.SAREF_MEASUREMENT_PATTERN, null);

		sc1.register(pKI);
		sc2.register(rKI, new ReactHandler() {

			public ReactResult react(ReactKnowledgeInteraction aReactKnowledgeInteraction, Set<Binding> argument) {

				return new ReactResult();
			}

		});

		sc1.post(pKI, new HashSet<Binding>());
	}
}
