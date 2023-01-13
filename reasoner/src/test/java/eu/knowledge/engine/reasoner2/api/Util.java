package eu.knowledge.engine.reasoner2.api;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;

public class Util {

	static String getStringFromInputStream(InputStream is) {
		String text = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining("\n"));

		return text;
	}

	public static Binding toBinding(String encodedBinding) {

		Binding b = new Binding();
		String[] entries = encodedBinding.split(",");

		int varIdx = 0, valIdx = 1;

		for (String entry : entries) {

			if (!entry.isEmpty()) {
				String[] keyVal = entry.split("=");
				b.put(keyVal[varIdx], keyVal[valIdx]);
			}
		}
		return b;
	}

	public static BindingSet toBindingSet(String encodedBindingSet) {

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
