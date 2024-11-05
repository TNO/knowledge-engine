package eu.knowledge.engine.rest.api;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import eu.knowledge.engine.test_utils.HttpTester;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

public class TestUtil {

	public static void unregisterAllKBs(String url) {

		HttpTester listKbs;
		try {
			listKbs = new HttpTester(new URL(url + "/sc"), "GET", null, null);
			listKbs.expectStatus(200);
			String body = listKbs.getBody();

			JsonReader jsonReader = Json.createReader(new StringReader(body));
			JsonArray KBs = jsonReader.readArray();

			for (JsonValue jo : KBs) {
				unregisterKb(url, jo.asJsonObject().getString("knowledgeBaseId"));
			}

		} catch (MalformedURLException e) {
			fail();
			e.printStackTrace();
		}

	}

	public static void unregisterKb(String url, String id) {

		try {
			HttpTester deleteKb = new HttpTester(new URL(url + "/sc"), "DELETE", null, Map.of("Knowledge-Base-Id", id));
			deleteKb.expectStatus(200);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail();

		}
	}

}
