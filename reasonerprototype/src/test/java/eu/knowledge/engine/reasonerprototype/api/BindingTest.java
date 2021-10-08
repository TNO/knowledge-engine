package eu.knowledge.engine.reasonerprototype.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import eu.knowledge.engine.reasonerprototype.api.Binding;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TripleVarBindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;
import eu.knowledge.engine.reasonerprototype.api.TripleVar;
import eu.knowledge.engine.reasonerprototype.api.TripleVarBinding;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Literal;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Variable;

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
			Binding merged = Util.toBinding(tests[i + first]).merge(Util.toBinding(tests[i + second]));
			if (!Util.toBinding(tests[i + expected]).equals(merged)) {

				failed.put(i / 3, new Compare<Binding>(Util.toBinding(tests[i + expected]), merged));
			}
		}

		// nothing should fail.
		assertEquals(new HashMap<Integer, Compare<Binding>>(), failed);
	}

	@Test
	public void testingMergeOfBindingSetWithBindingSet() {
		//@formatter:off
	
		String[] tests = new String[] {
		
				"a:1,b:1|a:1,b:2", "a:1,c:1|a:1,c:2", "a:1,b:1|a:1,b:2|a:1,c:1|a:1,c:2|a:1,b:1,c:1|a:1,b:1,c:2|a:1,b:2,c:1|a:1,b:2,c:2",
				"?room:r1,?sensor:s1|?room:r1,?sensor:s2", "?room:r1,?isOn:true", "?room:r1,?sensor:s1|?room:r1,?sensor:s2|?room:r1,?isOn:true|?room:r1,?sensor:s1,?isOn:true|?room:r1,?sensor:s2,?isOn:true",
				"a:1,b:1|a:1,b:2|a:2,b:3", "b:2,c:1|b:3,c:1|b:3,c:2", "a:1,b:2,c:1|a:2,b:3,c:1|a:2,b:3,c:2|a:1,b:1|a:1,b:2|a:2,b:3|b:2,c:1|b:3,c:1|b:3,c:2",
							
		};
		//@formatter:on

		int first = 0, second = 1, expected = 2;

		Map<Integer, Compare<BindingSet>> failed = new HashMap<>();

		for (int i = 0; i < tests.length; i = i + 3) {
			BindingSet merged = Util.toBindingSet(tests[i + first]).altMerge(Util.toBindingSet(tests[i + second]), true,
					true);
			if (!Util.toBindingSet(tests[i + expected]).equals(merged)) {

				failed.put(i / 3, new Compare<BindingSet>(Util.toBindingSet(tests[i + expected]), merged));
			}
		}

		// nothing should fail.
		assertEquals(new HashMap<Integer, Compare<BindingSet>>(), failed);

	}

	@Test
	public void testConvert() {

		TriplePattern triple1 = new TriplePattern("?s type Sensor");
		TriplePattern triple2 = new TriplePattern("?t type MultiSensor");
		Set<TriplePattern> goal = new HashSet<>(Arrays.asList(triple1, triple2));

		TriplePattern triple3 = new TriplePattern("?a type ?b");
		Set<TriplePattern> consequence = new HashSet<>(Arrays.asList(triple3));

		Set<Map<TriplePattern, TriplePattern>> mapping = new HashSet<>();

		Map<TriplePattern, TriplePattern> map = new HashMap<TriplePattern, TriplePattern>();
		map.put(triple3, triple1);

		mapping.add(map);
		Map<TriplePattern, TriplePattern> map2 = new HashMap<TriplePattern, TriplePattern>();
		map2.put(triple3, triple2);
		mapping.add(map2);

		BindingSet b = Util.toBindingSet("?s:<sensor>|?t:<sensor>");

		BindingSet converted = b.translate(mapping);

		System.out.println(converted);

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

	@Test
	public void testGraphPatternBindingSets() {
		TriplePattern t1 = new TriplePattern("?a type Sensor");
		TriplePattern t2 = new TriplePattern("?a hasVal ?b");
		TripleVarBinding tb1 = new TripleVarBinding();
		tb1.put(new TripleVar(t1, "?a"), new Literal("<sensor1>"));

		TripleVarBinding tb2 = new TripleVarBinding();
		tb2.put(new TripleVar(t2, "?b"), "22");
		tb2.put(new TripleVar(t2, "?a"), "<sensor1>");
		tb2.put(new TripleVar(t1, "?a"), "<sensor1>");

		Set<TriplePattern> aGraphPattern = new HashSet<>(Arrays.asList(t1, t2));
		TripleVarBindingSet gbs = new TripleVarBindingSet(aGraphPattern);
		gbs.add(tb1);
		gbs.add(tb2);

		System.out.println(gbs);

		BindingSet bs = gbs.toBindingSet();
		System.out.println(bs);

		TripleVarBindingSet gbsReturned = bs.toGraphBindingSet(aGraphPattern);

		System.out.println(gbsReturned);
	}

	@Test
	public void testTripleVarBinding() {
		TriplePattern tp1 = new TriplePattern("?s type Sensor");
		TriplePattern tp2 = new TriplePattern("?s hasVal ?v");

		TripleVarBinding tvb1 = new TripleVarBinding();
		tvb1.put(new TripleVar(tp1, "?s"), "<sensor1>");

		TripleVarBinding tvb2 = new TripleVarBinding();
		tvb2.put(new TripleVar(tp2, "?s"), "<sensor1>");
		tvb2.put(new TripleVar(tp2, "?v"), "22");

		System.out.println(tvb1.merge(tvb2));
	}

	@Test
	public void testTripleVarBindingComplication() {
		TriplePattern tp1 = new TriplePattern("?s type ?t");
		TriplePattern tp2 = new TriplePattern("?s hasVal ?v");

		TripleVarBindingSet gbs1 = new TripleVarBindingSet(new HashSet<>(Arrays.asList(tp1, tp2)));
		TripleVarBinding tvb1 = new TripleVarBinding();
		tvb1.put(new TripleVar(tp1, "?s"), "<sensor1>");
		tvb1.put(new TripleVar(tp1, "?t"), "Sensor");
		tvb1.put(new TripleVar(tp2, "?s"), "<sensor1>");
		tvb1.put(new TripleVar(tp2, "?v"), "22");
		gbs1.add(tvb1);

		TripleVarBindingSet gbs2 = new TripleVarBindingSet(new HashSet<>(Arrays.asList(tp1, tp2)));
		TripleVarBinding tvb2 = new TripleVarBinding();
		tvb2.put(new TripleVar(tp1, "?s"), "<sensor1>");
		tvb2.put(new TripleVar(tp1, "?t"), "Device");
		gbs2.add(tvb2);

		TripleVarBindingSet merge = gbs1.merge(gbs2);
		System.out.println(merge);

		assertTrue(merge.isEmpty());

		// we want two full TripleVarBindings but one with Device and the other with
		// Sensor. The question is how to achieve this when there is only a single
		// TripleVar,value in common. And do we want the same behaviour if the common
		// TripleVar,value is the 22?
	}

	@Test
	public void testTripleVarBindingComplication2() {
		TriplePattern tp1 = new TriplePattern("?s type ?t");
		TriplePattern tp2 = new TriplePattern("?s hasVal ?v");

		TripleVarBindingSet gbs1 = new TripleVarBindingSet(new HashSet<>(Arrays.asList(tp1, tp2)));
		TripleVarBinding tvb1 = new TripleVarBinding();
		tvb1.put(new TripleVar(tp1, "?s"), "<sensor1>");
		tvb1.put(new TripleVar(tp1, "?t"), "Sensor");
		tvb1.put(new TripleVar(tp2, "?s"), "<sensor1>");
		tvb1.put(new TripleVar(tp2, "?v"), "22");
		gbs1.add(tvb1);

		TripleVarBindingSet gbs2 = new TripleVarBindingSet(new HashSet<>(Arrays.asList(tp1, tp2)));
		TripleVarBinding tvb2 = new TripleVarBinding();
		tvb2.put(new TripleVar(tp2, "?s"), "<sensor2>");
		tvb2.put(new TripleVar(tp2, "?v"), "22");
		gbs2.add(tvb2);

		TripleVarBindingSet merge = gbs1.merge(gbs2);
		System.out.println(merge);

		assertTrue(merge.isEmpty());

		// Do we want the same behavior (as complication1) if the common
		// TripleVar,value is the 22? Probably not, so, apparently, we cannot just
		// blindly look at the bindingsets and ignore the triples when merging.
		// Is it caused by the type of the 'thing in common'? Do we need to distinguish
		// between IRIs and Literals?
	}

	@Test
	public void testTripleVarBindingComplication3() {
		TriplePattern tp1 = new TriplePattern("?s type ?t");
		TriplePattern tp2 = new TriplePattern("?s hasVal ?v");

		TripleVarBindingSet gbs1 = new TripleVarBindingSet(new HashSet<>(Arrays.asList(tp1, tp2)));
		TripleVarBinding tvb1 = new TripleVarBinding();
		tvb1.put(new TripleVar(tp1, "?s"), "<sensor1>");
		tvb1.put(new TripleVar(tp1, "?t"), "Sensor");
		tvb1.put(new TripleVar(tp2, "?s"), "<sensor1>");
		tvb1.put(new TripleVar(tp2, "?v"), "<22>");
		gbs1.add(tvb1);

		TripleVarBindingSet gbs2 = new TripleVarBindingSet(new HashSet<>(Arrays.asList(tp1, tp2)));
		TripleVarBinding tvb2 = new TripleVarBinding();
		tvb2.put(new TripleVar(tp2, "?s"), "<sensor2>");
		tvb2.put(new TripleVar(tp2, "?v"), "<22>");
		gbs2.add(tvb2);

		TripleVarBindingSet merge = gbs1.merge(gbs2);
		System.out.println(merge);

		assertTrue(merge.isEmpty());

		// Do we want the same behavior (as complication1) if the common
		// TripleVar,value is the 22? Probably not, so, apparently, we cannot just
		// blindly look at the bindingsets and ignore the triples when merging.
		// Is it caused by the type of the 'thing in common'? Do we need to distinguish
		// between IRIs and Literals?
	}
	
}
