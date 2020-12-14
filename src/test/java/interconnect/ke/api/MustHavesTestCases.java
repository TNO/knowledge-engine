package interconnect.ke.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MustHavesTestCases {

	private static final Logger LOG = LoggerFactory.getLogger(MustHavesTestCases.class);

	@Test
	void testCaseM1() {
		//TODO do we want to start a service directory?
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
				String value = argument.iterator().next().get("v");
				assertEquals(value, TestUtils.getIntegerValueFromString(value));
				return new ReactResult();
			}

		});

		sc1.post(pKI, TestUtils.getSingleBinding("v",
				TestUtils.getStringValueFromInteger(value), "m", TestUtils.getWithPrefix("m1")));
		
		
		sc2.stop();
		sc1.stop();
	}
}
