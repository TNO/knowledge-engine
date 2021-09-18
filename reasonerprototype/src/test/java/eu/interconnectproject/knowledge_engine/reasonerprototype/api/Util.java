package eu.interconnectproject.knowledge_engine.reasonerprototype.api;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class Util {

	static String getStringFromInputStream(InputStream is) {
		String text = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining("\n"));

		return text;
	}

}
