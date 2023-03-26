package eu.knowledge.engine.smartconnector.api;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AdditionForAudienceTest {

	MockedKnowledgeBase kb1;
	MockedKnowledgeBase kbSum;
	MockedKnowledgeBase kbNum;
	MockedKnowledgeBase kb10;
	MockedKnowledgeBase kb100;
	private AskKnowledgeInteraction askKI;

	public void beforeAll() {

		KnowledgeNetwork kn = new KnowledgeNetwork();

		kb1 = new MockedKnowledgeBase("kb1");
		kb1.setReasonerEnabled(true);
		kn.addKB(kb1);

		var reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), new GraphPattern(
				"?e <type> <Expression> . ?e <hasNr> ?n1 . ?e <hasNr> ?n2 . ?n1 <type> <Number> . ?n1 <hasDigit> ?d1 . ?d1 <hasPlace> ?p . ?d1 <hasActualDigit> ?ad1 . ?n2 <type> <Number> . ?n2 <hasDigit> ?d2 . ?d2 <hasPlace> ?p . ?d2 <hasActualDigit> ?ad2 ."),
				new GraphPattern(
						"?e <hasOutcome> ?n3 . ?n3 <type> <Number> . ?n3 <hasDigit> ?d3 . ?d3 <hasPlace> ?p . ?d3 <hasActualDigit> ?ad3 . ?d3 <hasOverflow> ?o ."));

		kb1.register(reactKI, (anRKI, aReactExchangeInfo) -> {
			BindingSet bs = aReactExchangeInfo.getArgumentBindings();

			System.out.println("In: " + bs);

			int i = 0;

			BindingSet bs2 = new BindingSet();
			for (Binding b : bs) {
				var expr = b.get("e");
				var n1 = b.get("n1");
				var d1 = b.get("d1");
				var p = b.get("p");
				var ad1 = b.get("ad1");

				var n2 = b.get("n2");
				var d2 = b.get("d2");
				var ad2 = b.get("ad2");

//				System.out.println(n1);
//				System.out.println(d1);
//				System.out.println(p);
//				System.out.println(ad1);
//				System.out.println(n2);
//				System.out.println(d2);
//				System.out.println(ad2);

				if (!n1.equals(n2)) {

					int actualD1 = Integer.parseInt(ad1.substring(1, 2));
					int actualD2 = Integer.parseInt(ad2.substring(1, 2));

					int actualD3 = actualD1 + actualD2;

					String n3 = "<" + i + ">";
					String d3 = "<" + i + "d" + p.substring(1, 2) + ">";
					String ad3 = "\"" + ((actualD3 > 9) ? ("" + actualD3).substring(1) : actualD3) + "\"";
					String o = "\"" + (actualD3 > 9) + "\"";

					Binding e = new Binding();
					e.put("e", expr);
					e.put("n3", n3);
					e.put("d3", d3);
					e.put("ad3", ad3);
					e.put("p", p);
					e.put("o", o);
					bs2.add(e);
					i++;
				}
			}

			System.out.println("Out: " + bs2);
			return bs2;
		});

		kbSum = new MockedKnowledgeBase("kbSum");
		kbSum.setReasonerEnabled(true);
		kn.addKB(kbSum);

		var answerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), new GraphPattern(
				"?e <type> <Expression> . ?e <hasNr> ?n1 . ?n1 <type> <Number> . ?n1 <hasDigit> ?d1 . ?d1 <hasPlace> ?p . ?d1 <hasActualDigit> ?ad1 .")); //

		kbSum.register(answerKI, (anAKI, anAnswerExchangeInfo) -> {

			BindingSet bs = new BindingSet();

			/**
			 * <123> <type> <Number> . <123> <hasDigit> <123d1> . <123d1> <type> <Digit> .
			 * <123d1> <hasPlace> "1" . <123d1> <hasActualDigit> "1" . <123> <hasDigit>
			 * <123d2> . <123d2> <type> <Digit> . <123d2> <hasPlace> "2" . <123d2>
			 * <hasActualDigit> "2" . <123> <hasDigit> <123d3> . <123d3> <type> <Digit> .
			 * <123d3> <hasPlace> "3" . <123d3> <hasActualDigit> "3" .
			 */

			// first number
			var b1 = new Binding();
			b1.put("e", "<e1>");
			b1.put("n1", "<123>");
			b1.put("d1", "<123d1>");
			b1.put("p", "\"1\"");
			b1.put("ad1", "\"1\"");
			bs.add(b1);

			var b2 = new Binding();
			b2.put("e", "<e1>");
			b2.put("n1", "<123>");
			b2.put("d1", "<123d2>");
			b2.put("p", "\"2\"");
			b2.put("ad1", "\"2\"");
			bs.add(b2);

			var b3 = new Binding();
			b3.put("e", "<e1>");
			b3.put("n1", "<123>");
			b3.put("d1", "<123d3>");
			b3.put("p", "\"3\"");
			b3.put("ad1", "\"3\"");
			bs.add(b3);

			// second number
			var b4 = new Binding();
			b4.put("e", "<e1>");
			b4.put("n1", "<456>");
			b4.put("d1", "<456d1>");
			b4.put("p", "\"1\"");
			b4.put("ad1", "\"4\"");
			bs.add(b4);

			var b5 = new Binding();
			b5.put("e", "<e1>");
			b5.put("n1", "<456>");
			b5.put("d1", "<456d2>");
			b5.put("p", "\"2\"");
			b5.put("ad1", "\"5\"");
			bs.add(b5);

			var b6 = new Binding();
			b6.put("e", "<e1>");
			b6.put("n1", "<456>");
			b6.put("d1", "<456d3>");
			b6.put("p", "\"3\"");
			b6.put("ad1", "\"6\"");
			bs.add(b6);

			return bs;
		});

		kbNum = new MockedKnowledgeBase("kbNum");
		kbNum.setReasonerEnabled(true);
		kn.addKB(kbNum);

		this.askKI = new AskKnowledgeInteraction(new CommunicativeAct(), new GraphPattern(
				"?e <type> <Expression> . ?e <hasOutcome> ?n1 . ?n1 <type> <Number> . ?n1 <hasDigit> ?d1 . ?d1 <hasPlace> ?p . ?d1 <hasActualDigit> ?ad1 ."));

		kbNum.register(askKI);

//		kb10 = new MockedKnowledgeBase("kb10");
//		kb100 = new MockedKnowledgeBase("kb100");

		kn.sync();

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

}
