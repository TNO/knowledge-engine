package eu.interconnectproject.knowledge_engine.reasonerprototype.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BindingTest {

	@Test
	public void testSameVar() {
		BindingSet b1 = new BindingSet(new Binding("var1", "val1"));
		BindingSet b2 = new BindingSet(new Binding("var1", "val2"));

		BindingSet merged = b1.merge(b2);

		assertEquals(new BindingSet(new Binding("var1", "val1"), new Binding("var1", "val2")), merged);
	}

	@Test
	public void testOther() {
		// TODO I'm not 100% convinced that this is the desired behavior...

		Binding bs1b1 = new Binding();
		bs1b1.put("?room", "r1");
		bs1b1.put("?sensor", "s1");
		Binding bs1b2 = new Binding();
		bs1b2.put("?room", "r2");
		bs1b2.put("?sensor", "s2");
		Binding bs2b1 = new Binding();
		BindingSet bs1 = new BindingSet(bs1b1, bs1b2);
		bs2b1.put("?room", "r1");
		bs2b1.put("?isOn", "true");
		Binding bs2b2 = new Binding();
		bs2b2.put("?room", "r2");
		bs2b2.put("?isOn", "true");
		BindingSet bs2 = new BindingSet(bs2b1, bs2b2);

		BindingSet merged = bs1.merge(bs2);

		Binding e1 = new Binding();
		e1.put("?room", "r1");
		e1.put("?sensor", "s1");
		e1.put("?isOn", "true");
		Binding e2 = new Binding();
		e2.put("?room", "r2");
		e2.put("?sensor", "s2");
		e2.put("?isOn", "true");
		BindingSet expected = new BindingSet(e1, e2);
		assertEquals(expected, merged);
	}

}
