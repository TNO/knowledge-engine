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

	@Test
	public void testingMergeOfBindingWithBinding() {

		//@formatter:off
		String[] tests = new String[] {
				
				"a:1,b:1", "b:1,c:1", "a:1,b:1,c:1",
				"a:1,b:1", "c:1,d:1", "a:1,b:1,c:1,d:1",
//				"a:1,b:1", "b:2,c:1", "",
//				"a:1,b:1", "a:2,b:2", "",
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

	@Test
	public void testingMergeOfBindingSetWithBindingSet() {
		//@formatter:off
	
		String[] tests = new String[] {
		
				"a:1,b:1|a:1,b:2", "a:1,c:1|a:1,c:2", "a:1,b:1,c:1|a:1,b:1,c:2|a:1,b:2,c:1|a:1,b:2,c:2",
				"?room:r1,?sensor:s1|?room:r1,?sensor:s2", "?room:r1,?isOn:true", "?room:r1,?sensor:s1,?isOn:true|?room:r1,?sensor:s2,?isOn:true",
							
		};
		//@formatter:on

		int first = 0, second = 1, expected = 2;

		Map<Integer, Compare<BindingSet>> failed = new HashMap<>();

		for (int i = 0; i < tests.length; i = i + 3) {
			BindingSet merged = toBindingSet(tests[i + first]).altMerge(toBindingSet(tests[i + second]));
			if (!toBindingSet(tests[i + expected]).equals(merged)) {

				failed.put(i / 3, new Compare<BindingSet>(toBindingSet(tests[i + expected]), merged));
			}
		}

		// nothing should fail.
		assertEquals(new HashMap<Integer, Compare<BindingSet>>(), failed);

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

	private BindingSet toBindingSet(String encodedBindingSet) {

		BindingSet bs = new BindingSet();
		String[] entries = encodedBindingSet.split("\\|");

		for (String entry : entries) {
			if (!entry.isEmpty()) {
				Binding b = toBinding(entry);
				bs.add(b);
			}
		}
		return bs;
	}

}
