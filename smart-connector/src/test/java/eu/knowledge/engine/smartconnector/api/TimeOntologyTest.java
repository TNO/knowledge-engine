package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import jakarta.xml.bind.DatatypeConverter;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.EasyKnowledgeBase;

public class TimeOntologyTest {

	private static final Logger LOG = LoggerFactory.getLogger(TimeOntologyTest.class);

	private PrefixMapping prefixes = new PrefixMappingMem().setNsPrefix("ex", "https://www.tno.nl/example/")
			.setNsPrefix("time", "https://www.w3.org/TR/owl-time/")
			.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

	private KnowledgeNetwork kn = new KnowledgeNetwork();

	private EasyKnowledgeBase calendar = new EasyKnowledgeBase("Calendar");
	private EasyKnowledgeBase app = new EasyKnowledgeBase("App");
	private EasyKnowledgeBase time = new EasyKnowledgeBase("Time");

	@Test
	public void test() throws InterruptedException, ExecutionException {

		// add KBs

		this.calendar.setReasonerEnabled(true);
		this.app.setReasonerEnabled(true);
		this.time.setReasonerEnabled(true);

		kn.addKB(this.calendar);
		kn.addKB(this.app);
		kn.addKB(this.time);

		// add KIs
		GraphPattern cGp1 = new GraphPattern(this.prefixes, "?meeting rdf:type ex:Meeting .\n"
				+ "?meeting ex:hasTopic ?subject .\n" + "?meeting ex:startAt ?start .\n" + "?meeting ex:endAT ?end .\n"
				+ "?start rdf:type time:Instant .\n" + "?end rdf:type time:Instant .\n"
				+ "?start time:inXSDDateTimeStamp ?timeStart .\n" + "?end time:inXSDDateTimeStamp ?timeEnd .\n");
		AnswerKnowledgeInteraction cAnswerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), cGp1);
		this.calendar.register(cAnswerKI, (AnswerHandler) (aAnswerKI, aExchangeInfo) -> {

			BindingSet bs = new BindingSet();
			Binding b1 = new Binding();
			bs.add(b1);
			b1.put("meeting", "<meeting1>");
			b1.put("subject", "\"Meeting 1\"");
			b1.put("start", "<meeting1start>");
			b1.put("end", "<meeting1end>");
			b1.put("timeStart", "\"2017-04-12T10:30:00+10:00\"");
			b1.put("timeEnd", "\"2017-04-12T11:30:00+10:00\"");

			Binding b2 = new Binding();
			bs.add(b2);
			b2.put("meeting", "<meeting2>");
			b2.put("subject", "\"Meeting 2\"");
			b2.put("start", "<meeting2start>");
			b2.put("end", "<meeting2end>");
			b2.put("timeStart", "\"2017-04-12T12:30:00+10:00\"");
			b2.put("timeEnd", "\"2017-04-12T13:30:00+10:00\"");

			LOG.info("Calendar: all items returned.");

			return bs;
		});

		GraphPattern aGp1 = new GraphPattern(this.prefixes,
				"?instant rdf:type time:Instant .\n"
						+ "?instant time:inXSDDateTimeStamp \"2017-04-12T12:00:00+10:00\" .\n"
						+ "?instant time:before ?meetingStart .\n" + "?meeting ex:startAt ?meetingStart .\n"
						+ "?meeting rdf:type ex:Meeting .\n" + "?meeting ex:hasTopic ?topic .");
		AskKnowledgeInteraction aAskKI = new AskKnowledgeInteraction(new CommunicativeAct(), aGp1);
		this.app.register(aAskKI);

		GraphPattern tGp1 = new GraphPattern(this.prefixes,
				"?i rdf:type time:Instant .\n" + "?i time:inXSDDateTimeStamp ?t .");
		AnswerKnowledgeInteraction tAnswerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), tGp1);
		this.time.register(tAnswerKI, (AnswerHandler) (aAnswerKI, aExchangeInfo) -> {

			String newIVal = null, newTVal = null;

			BindingSet inBS = aExchangeInfo.getIncomingBindings();

			if (inBS.iterator().hasNext()) {

				Binding b = inBS.iterator().next();

				String iVal = b.get("i");
				String tVal = b.get("t");

				if (tVal != null) {

					try {
						DatatypeConverter.parseDateTime(tVal.substring(1, tVal.length() - 1));
						newTVal = tVal;
					} catch (IllegalArgumentException iae) {
						// not valid date, newTVal remains null
					}
					if (iVal == null) {
						newIVal = "<" + UUID.randomUUID().toString() + ">";
					} else {
						newIVal = iVal;
					}
				}
			}

			BindingSet bs = new BindingSet();

			if (inBS.iterator().hasNext()) {
				Binding newB1 = new Binding();
				Binding newB2 = new Binding();
				Binding newB3 = new Binding();
				Binding newB4 = new Binding();
				Binding newB5 = new Binding();

				newB1.put("i", "<meeting1start>");
				newB1.put("t", "\"2017-04-12T10:30:00+10:00\"");

				newB2.put("i", "<meeting2start>");
				newB2.put("t", "\"2017-04-12T12:30:00+10:00\"");

				newB3.put("i", "<meeting1end>");
				newB3.put("t", "\"2017-04-12T11:30:00+10:00\"");

				newB4.put("i", "<meeting2end>");
				newB4.put("t", "\"2017-04-12T13:30:00+10:00\"");

				newB5.put("i", "<testing>");
				newB5.put("t", "\"2017-04-12T12:00:00+10:00\"");

				bs.add(newB1);
				bs.add(newB2);
				bs.add(newB3);
				bs.add(newB4);
				bs.add(newB5);

			} else if (newIVal != null && newTVal != null) {
				Binding newB = new Binding();
				bs.add(newB);
				newB.put("i", newIVal);
				newB.put("t", newTVal);
			} else {
				Binding newB = new Binding();
				bs.add(newB);
			}

			LOG.info("Time: {} - {}", inBS, bs);

			return bs;
		});

		GraphPattern tGp2 = new GraphPattern(this.prefixes,
				"?i1 rdf:type time:Instant .\n" + "?i1 time:inXSDDateTimeStamp ?t1 .\n"
						+ "?i2 rdf:type time:Instant .\n" + "?i2 time:inXSDDateTimeStamp ?t2 .");
		GraphPattern tGp3 = new GraphPattern(this.prefixes, "?i1 time:before ?i2 .");
		ReactKnowledgeInteraction tReactKI1 = new ReactKnowledgeInteraction(new CommunicativeAct(), tGp2, tGp3);
		this.time.register(tReactKI1, (ReactHandler) (aReactKI, anExchangeInfo) -> {

			BindingSet iBS = anExchangeInfo.getArgumentBindings();
			BindingSet bs = new BindingSet();

			Iterator<Binding> iter = iBS.iterator();

			while (iter.hasNext()) {
				String i1 = null, i2 = null;
				boolean isValid = false, isBefore = false;

				Binding iB = iter.next();

				LOG.info("Time1: {}", iB);

				i1 = iB.get("i1");
				i2 = iB.get("i2");
				String t1 = iB.get("t1");
				String t2 = iB.get("t2");
				Calendar c1, c2;
				try {
					c1 = DatatypeConverter.parseDateTime(t1.substring(1, t1.length() - 1));
					c2 = DatatypeConverter.parseDateTime(t2.substring(1, t1.length() - 1));
					isValid = true;
					isBefore = c1.before(c2);
				} catch (IllegalArgumentException iae) {
					// t1 or t2 invalid date
				}

				Binding b = new Binding();
				if (isValid && isBefore) {
					b.put("i1", i1);
					b.put("i2", i2);
					bs.add(b);
				}

			}

			LOG.info("Time2: {}", bs);

			return bs;
		});

		GraphPattern tGp4 = new GraphPattern(this.prefixes,
				"?i1 rdf:type time:Instant .\n" + "?i1 time:inXSDDateTimeStamp ?t1 .\n"
						+ "?i2 rdf:type time:Instant .\n" + "?i2 time:inXSDDateTimeStamp ?t2 .");
		GraphPattern tGp5 = new GraphPattern(this.prefixes, "?i1 time:after ?i2 .");
		ReactKnowledgeInteraction tReactKI2 = new ReactKnowledgeInteraction(new CommunicativeAct(), tGp4, tGp5);
		this.time.register(tReactKI2, (ReactHandler) (aReactKI, anExchangeInfo) -> {
			boolean isValid = false, isBefore = false;
			String i1 = null, i2 = null;
			BindingSet iBS = anExchangeInfo.getArgumentBindings();

			if (iBS.iterator().hasNext()) {
				Binding iB = iBS.iterator().next();

				i1 = iB.get("i1");
				i2 = iB.get("i2");
				String t1 = iB.get("t1");
				String t2 = iB.get("t2");
				Calendar c1, c2;
				try {
					c1 = DatatypeConverter.parseDateTime(t1.substring(1, t1.length() - 1));
					c2 = DatatypeConverter.parseDateTime(t2.substring(1, t2.length() - 1));
					isValid = true;
					isBefore = c1.after(c2);
				} catch (IllegalArgumentException iae) {
					// t1 or t2 invalid date
				}
			}
			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			bs.add(b);

			if (isValid && isBefore) {
				b.put("i1", i1);
				b.put("i2", i2);
			}
			LOG.info("Time: {} after {}", i1, i2);

			return bs;
		});

		kn.sync();

		AskResult ar = app.ask(aAskKI, new BindingSet()).get();

		BindingSet bindings = ar.getBindings();
		LOG.info("Bindings: {}", bindings);

		assertTrue(bindings.size() > 0);
		assertEquals("\"Meeting 2\"", bindings.iterator().next().get("topic"));

		kn.stop().get();

	}

}
