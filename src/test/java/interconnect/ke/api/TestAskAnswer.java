package interconnect.ke.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import interconnect.ke.api.binding.Binding;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.runtime.JvmOnlyMessageDispatcher;

public class TestAskAnswer {

	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;

	@Test
	public void testAskAnswer() throws InterruptedException {

		new JvmOnlyMessageDispatcher();

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		int wait = 2;
		final CountDownLatch kb1Done = new CountDownLatch(1);
		final CountDownLatch kb2ReceivedData = new CountDownLatch(1);

		kb1 = new MockedKnowledgeBase("kb1") {
			private AnswerKnowledgeInteraction ki;

			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				GraphPattern gp = new GraphPattern(prefixes, "?a ex:b ?c.");
				this.ki = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp);
				aSC.register(this.ki, new AnswerHandler() {
					@Override
					public BindingSet answer(AnswerKnowledgeInteraction anAKI, BindingSet aBindingSet) {
						assertTrue(aBindingSet.isEmpty(), "Should not have bindings in this binding set.");

						BindingSet bindingSet = new BindingSet();
						Binding binding = new Binding();
						binding.put("a", "<https://www.tno.nl/example/a>");
						binding.put("c", "<https://www.tno.nl/example/c>");
						bindingSet.add(binding);

						return bindingSet;
					}
				});
				kb1Done.countDown();
			};
		};
		assertTrue(kb1Done.await(wait, TimeUnit.SECONDS), "KB1 should have initialized within " + wait + " seconds.");

		kb2 = new MockedKnowledgeBase("kb2") {
			private AskKnowledgeInteraction ki;

			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				GraphPattern gp = new GraphPattern(prefixes, "?a ex:b ?c.");
				this.ki = new AskKnowledgeInteraction(new CommunicativeAct(), gp);

				aSC.register(this.ki);

				this.askForKnowledge();
			};

			private void askForKnowledge() {
				BindingSet result = null;
				try {
					result = this.getSmartConnector().ask(this.ki, new BindingSet()).get().getBindings();
				} catch (InterruptedException | ExecutionException e) {
					fail();
				}

				Iterator<Binding> iter = result.iterator();
				Binding b = iter.next();

				assertEquals("https://www.tno.nl/example/a", b.get("a"), "Binding of 'a' is incorrect.");
				assertEquals("https://www.tno.nl/example/c", b.get("c"), "Binding of 'c' is incorrect.");

				assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");
				kb2ReceivedData.countDown();
			}
		};

		assertTrue(kb2ReceivedData.await(wait, TimeUnit.SECONDS),
				"KB2 should have initialized within " + wait + " seconds.");

	}

	@AfterAll
	public static void cleanup() {

		if (kb1 != null) {
			kb1.stop();
		}

		if (kb2 != null) {
			kb2.stop();
		}
	}
}
