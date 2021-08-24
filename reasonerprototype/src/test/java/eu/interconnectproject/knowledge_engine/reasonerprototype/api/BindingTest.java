package eu.interconnectproject.knowledge_engine.reasonerprototype.api;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
		BindingSet bs1 = new BindingSet(bs1b1, bs1b2);
		Binding bs2b1 = new Binding();
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

	/**
	 * Another interesting test that currently fails, but should (I think) succeed.
	 */
	@Test
	public void test1() {

		// we get the following info from one KB
		Binding bs1b1 = new Binding();
		bs1b1.put("?room", "r1");
		bs1b1.put("?sensor", "s1");
		Binding bs1b2 = new Binding();
		bs1b2.put("?room", "r1");
		bs1b2.put("?sensor", "s2");
		BindingSet bs1 = new BindingSet(bs1b1, bs1b2);
		// we get the following info from another KB
		Binding bs2b1 = new Binding();
		bs2b1.put("?room", "r1");
		bs2b1.put("?isOn", "true");
		BindingSet bs2 = new BindingSet(bs2b1);

		// when merged the isOn value should be distributed over the two bindings of bs1
		BindingSet merged = bs1.merge(bs2);

		Binding e1 = new Binding();
		e1.put("?room", "r1");
		e1.put("?sensor", "s1");
		e1.put("?isOn", "true");
		Binding e2 = new Binding();
		e2.put("?room", "r1");
		e2.put("?sensor", "s2");
		e2.put("?isOn", "true");
		BindingSet expected = new BindingSet(e1, e2);
		assertEquals(expected, merged);
	}

	@Test
	public void test2() {

		// we get the following info from one KB
		Binding bs1b1 = new Binding();
		bs1b1.put("?room", "r1");
		bs1b1.put("?sensor", "s1");
		Binding bs1b2 = new Binding();
		bs1b2.put("?room", "r1");
		bs1b2.put("?sensor", "s2");
		BindingSet bs1 = new BindingSet(bs1b1, bs1b2);
		// we get the following info from another KB
		Binding bs2b1 = new Binding();
		bs2b1.put("?isOn", "true");
		Binding bs2b2 = new Binding();
		bs2b1.put("?isOn", "false");
		BindingSet bs2 = new BindingSet(bs2b1, bs2b2);

		// when merged the isOn value should be distributed over the two bindings of bs1
		BindingSet merged = bs1.merge(bs2);

		Binding e1 = new Binding();
		e1.put("?room", "r1");
		e1.put("?sensor", "s1");
		e1.put("?isOn", "true");
		Binding e2 = new Binding();
		e2.put("?room", "r1");
		e2.put("?sensor", "s1");
		e2.put("?isOn", "false");
		Binding e3 = new Binding();
		e3.put("?room", "r1");
		e3.put("?sensor", "s2");
		e3.put("?isOn", "true");
		Binding e4 = new Binding();
		e4.put("?room", "r1");
		e4.put("?sensor", "s2");
		e4.put("?isOn", "false");
		BindingSet expected = new BindingSet(e1, e2, e3, e4);
		assertEquals(expected, merged);
	}

	@Test
	public void test3() {
		Binding bs1b1 = new Binding();
		bs1b1.put("?eek", "a");
		bs1b1.put("?bla", "b");
		Binding bs1b2 = new Binding();
		bs1b2.put("?eek", "a");
		bs1b2.put("?bla", "c");
		BindingSet bs1 = new BindingSet(bs1b1, bs1b2);

		Binding bs2b1 = new Binding();
		bs1b1.put("?eek", "b");
		BindingSet bs2 = new BindingSet(bs2b1);

		// cannot be merged, so empty bindingset? TODO not sure about emptiness (or
		// return bs1?)
		BindingSet merged = bs1.merge(bs2);

		BindingSet expected = new BindingSet();
		assertEquals(expected, merged);

	}

	@Test
	public void testingMergeOfBindingWithBinding() {

		//@formatter:off
		String[] tests = new String[] {
				
				"a:1,b:1", "b:1,c:1", "a:1,b:1,c:1",
				"a:1,b:1", "c:1,d:1", "a:1,b:1,c:1,d:1",
				"a:1,b:1", "b:2,c:1", "",
				"a:1,b:1", "a:2,b:2", "",
				"a:1,b:1,c:1", "b:1,c:1,d:1", "a:1,b:1,c:1,d:1",
		};
		//@formatter:on

		int first = 0, second = 1, expected = 2;

		Map<Integer, Compare<Binding>> failed = new HashMap<>();

		for (int i = 0; i < tests.length; i = i + 3) {
			Binding merged = toBinding(tests[i + first]).merge(toBinding(tests[i + second]));
			if (!toBinding(tests[i + expected]).equals(merged)) {

				failed.put(i / 3, new Compare<Binding>(toBinding(tests[i + expected]), merged));
			}
		}

		// nothing should fail.
		assertEquals(new HashMap<Integer, Compare<Binding>>(), failed);
	}

	private static class Compare<T> {
		public T expected;
		public T actual;

		public Compare(T anExpected, T anActual) {
			if (anExpected == null || anActual == null) {
				throw new IllegalArgumentException("Both sets should be non-null.");
			}

			expected = anExpected;
			actual = anActual;
		}

		@Override
		public String toString() {
			return "Compare [expected=" + expected + ", actual=" + actual + "]";
		}
	}

	private Binding toBinding(String encodedBinding) {

		Binding b = new Binding();
		String[] entries = encodedBinding.split(",");

		int varIdx = 0, valIdx = 1;

		for (String entry : entries) {

			if (!entry.isEmpty()) {
				String[] keyVal = entry.split(":");
				b.put(keyVal[varIdx], keyVal[valIdx]);
			}
		}
		return b;
	}

}
