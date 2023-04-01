package eu.knowledge.engine.smartconnector.api;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("Long")
public class AdditionForAudienceTest {

	private static final Logger LOG = LoggerFactory.getLogger(AdditionForAudienceTest.class);

	class MyReactHandler implements ReactHandler {

		private int place;

		public MyReactHandler(int aPlace) {
			this.place = aPlace;
		}

		public BindingSet react(ReactKnowledgeInteraction anRKI, ReactExchangeInfo aReactExchangeInfo) {
			BindingSet bs = aReactExchangeInfo.getArgumentBindings();

			System.out.println("In: " + bs);

			BindingSet bs2 = new BindingSet();
			for (Binding b : bs) {
				var expr = b.get("e");
				var n1 = b.get("n1");
				var d1 = b.get("d1");
				var p = "\"" + String.valueOf(place) + "\"";
				var ad1 = b.get("ad1");

				var n2 = b.get("n2");
				var d2 = b.get("d2");
				var ad2 = b.get("ad2");

				if (!n1.equals(n2)) {

					int actualD1 = Integer.parseInt(ad1.substring(1, 2));
					int actualD2 = Integer.parseInt(ad2.substring(1, 2));

					int actualD3 = actualD1 + actualD2;

					String n3 = "<" + removeFirstLastChar(n1) + "/" + removeFirstLastChar(n2) + ">";
					String d3 = "<" + removeFirstLastChar(n3) + "/digit/" + removeFirstLastChar(p) + ">";
					String ad3 = "\"" + ((actualD3 > 9) ? ("" + actualD3).substring(1) : actualD3) + "\"";
					String o = "\"" + (actualD3 > 9) + "\"";

					Binding e = new Binding();
					e.put("e", expr);
					e.put("n3", n3);
					e.put("d3", d3);
					e.put("ad3", ad3);
//					e.put("p", p);
					e.put("o", o);
					bs2.add(e);
				}
			}

			System.out.println("Out: " + bs2);

			return bs2;
		}
	};

	MockedKnowledgeBase kb1;
	MockedKnowledgeBase kb10;
	MockedKnowledgeBase kb100;
	MockedKnowledgeBase kbSum;
	MockedKnowledgeBase kbNum;
	MockedKnowledgeBase kbRule;
	private AskKnowledgeInteraction askKI;
	private static KnowledgeNetwork kn;

	public void beforeAll() {

		kn = new KnowledgeNetwork();

		// kb1
		kb1 = new MockedKnowledgeBase("kb1");
		kb1.setReasonerEnabled(true);
		kn.addKB(kb1);

		var reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), new GraphPattern(
				"?e <http://example.org/type> <http://example.org/Expression> . ?e <http://example.org/hasFirstNr> ?n1 . ?e <http://example.org/hasSecondNr> ?n2 . ?n1 <http://example.org/type> <http://example.org/Number> . ?n1 <http://example.org/hasDigit> ?d1 . ?d1 <http://example.org/hasPlace> \"1\" . ?d1 <http://example.org/hasActualDigit> ?ad1 . ?n2 <http://example.org/type> <http://example.org/Number> . ?n2 <http://example.org/hasDigit> ?d2 . ?d2 <http://example.org/hasPlace> \"1\" . ?d2 <http://example.org/hasActualDigit> ?ad2 ."),
				new GraphPattern(
						"?e <http://example.org/hasOutcome> ?n3 . ?n3 <http://example.org/type> <http://example.org/Number> . ?n3 <http://example.org/hasDigit> ?d3 . ?d3 <http://example.org/hasPlace> \"1\" . ?d3 <http://example.org/hasActualDigit> ?ad3 . ?d3 <http://example.org/hasOverflow> ?o ."));
		kb1.register(reactKI, new MyReactHandler(1));

		// kb10
		kb10 = new MockedKnowledgeBase("kb10");
		kb10.setReasonerEnabled(true);
		kn.addKB(kb10);
		reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), new GraphPattern(
				"?e <http://example.org/type> <http://example.org/Expression> . ?e <http://example.org/hasFirstNr> ?n1 . ?e <http://example.org/hasSecondNr> ?n2 . ?n1 <http://example.org/type> <http://example.org/Number> . ?n1 <http://example.org/hasDigit> ?d1 . ?d1 <http://example.org/hasPlace> \"2\" . ?d1 <http://example.org/hasActualDigit> ?ad1 . ?n2 <http://example.org/type> <http://example.org/Number> . ?n2 <http://example.org/hasDigit> ?d2 . ?d2 <http://example.org/hasPlace> \"2\" . ?d2 <http://example.org/hasActualDigit> ?ad2 ."),
				new GraphPattern(
						"?e <http://example.org/hasOutcome> ?n3 . ?n3 <http://example.org/type> <http://example.org/Number> . ?n3 <http://example.org/hasDigit> ?d3 . ?d3 <http://example.org/hasPlace> \"2\" . ?d3 <http://example.org/hasActualDigit> ?ad3 . ?d3 <http://example.org/hasOverflow> ?o ."));
		kb10.register(reactKI, new MyReactHandler(2));

		kb100 = new MockedKnowledgeBase("kb100");
		kb100.setReasonerEnabled(true);
		kn.addKB(kb100);
		reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), new GraphPattern(
				"?e <http://example.org/type> <http://example.org/Expression> . ?e <http://example.org/hasFirstNr> ?n1 . ?e <http://example.org/hasSecondNr> ?n2 . ?n1 <http://example.org/type> <http://example.org/Number> . ?n1 <http://example.org/hasDigit> ?d1 . ?d1 <http://example.org/hasPlace> \"3\" . ?d1 <http://example.org/hasActualDigit> ?ad1 . ?n2 <http://example.org/type> <http://example.org/Number> . ?n2 <http://example.org/hasDigit> ?d2 . ?d2 <http://example.org/hasPlace> \"3\" . ?d2 <http://example.org/hasActualDigit> ?ad2 ."),
				new GraphPattern(
						"?e <http://example.org/hasOutcome> ?n3 . ?n3 <http://example.org/type> <http://example.org/Number> . ?n3 <http://example.org/hasDigit> ?d3 . ?d3 <http://example.org/hasPlace> \"3\" . ?d3 <http://example.org/hasActualDigit> ?ad3 . ?d3 <http://example.org/hasOverflow> ?o ."));
		kb100.register(reactKI, new MyReactHandler(3));

		kbSum = new MockedKnowledgeBase("kbSum");
		kbSum.setReasonerEnabled(true);
		kn.addKB(kbSum);

		var answerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), new GraphPattern(
				"?e <http://example.org/type> <http://example.org/Expression> . ?e ?hasFirstOrSecondNumber ?n1 . ?n1 <http://example.org/type> <http://example.org/Number> . ?n1 <http://example.org/hasDigit> ?d1 . ?d1 <http://example.org/hasPlace> ?p . ?d1 <http://example.org/hasActualDigit> ?ad1 ."));

		kbSum.register(answerKI, (anAKI, anAnswerExchangeInfo) -> {

			BindingSet bs = new BindingSet();

			// first number
			var b1 = new Binding();
			b1.put("e", "<e1>");
			b1.put("hasFirstOrSecondNumber", "<http://example.org/hasFirstNr>");
			b1.put("n1", "<123>");
			b1.put("d1", "<123d1>");
			b1.put("p", "\"1\"");
			b1.put("ad1", "\"1\"");
			bs.add(b1);

			var b2 = new Binding();
			b2.put("e", "<e1>");
			b2.put("hasFirstOrSecondNumber", "<http://example.org/hasFirstNr>");
			b2.put("n1", "<123>");
			b2.put("d1", "<123d2>");
			b2.put("p", "\"2\"");
			b2.put("ad1", "\"2\"");
			bs.add(b2);

			var b3 = new Binding();
			b3.put("e", "<e1>");
			b3.put("hasFirstOrSecondNumber", "<http://example.org/hasFirstNr>");
			b3.put("n1", "<123>");
			b3.put("d1", "<123d3>");
			b3.put("p", "\"3\"");
			b3.put("ad1", "\"3\"");
			bs.add(b3);

			// second number
			var b4 = new Binding();
			b4.put("e", "<e1>");
			b4.put("hasFirstOrSecondNumber", "<http://example.org/hasSecondNr>");
			b4.put("n1", "<456>");
			b4.put("d1", "<456d1>");
			b4.put("p", "\"1\"");
			b4.put("ad1", "\"4\"");
			bs.add(b4);

			var b5 = new Binding();
			b5.put("e", "<e1>");
			b5.put("hasFirstOrSecondNumber", "<http://example.org/hasSecondNr>");
			b5.put("n1", "<456>");
			b5.put("d1", "<456d2>");
			b5.put("p", "\"2\"");
			b5.put("ad1", "\"5\"");
			bs.add(b5);

			var b6 = new Binding();
			b6.put("e", "<e1>");
			b6.put("hasFirstOrSecondNumber", "<http://example.org/hasSecondNr>");
			b6.put("n1", "<456>");
			b6.put("d1", "<456d3>");
			b6.put("p", "\"3\"");
			b6.put("ad1", "\"6\"");
			bs.add(b6);

			System.out.println(bs);

			return bs;
		});

		kbNum = new MockedKnowledgeBase("kbNum");
		kbNum.setReasonerEnabled(true);
		kn.addKB(kbNum);

		this.askKI = new AskKnowledgeInteraction(new CommunicativeAct(), new GraphPattern(
				"?e <http://example.org/type> <http://example.org/Expression> . ?e <http://example.org/hasOutcome> ?n1 . ?n1 <http://example.org/type> <http://example.org/Number> . ?n1 <http://example.org/hasDigit> ?d1 . ?d1 <http://example.org/hasPlace> ?p . ?d1 <http://example.org/hasActualDigit> ?ad1 ."));

		kbNum.register(askKI);

		kn.sync();

	}

	public String removeFirstLastChar(String withQuotes) {
		return withQuotes.substring(1, withQuotes.length() - 1);
	}

	@Test
	public void test() throws InterruptedException, ExecutionException {

		beforeAll();

		AskPlan ap = kbNum.planAsk(this.askKI, new RecipientSelector());

		var rp = ap.getReasonerPlan();

		rp.getStore().printGraphVizCode(rp);

		AskResult ar = ap.execute(new BindingSet()).get();

		System.out.println("Result: " + ar.getBindings());

	}

	@AfterAll
	public static void afterAll() throws InterruptedException, ExecutionException, TimeoutException {
		kn.stop().get(10, TimeUnit.SECONDS);
	}

}
