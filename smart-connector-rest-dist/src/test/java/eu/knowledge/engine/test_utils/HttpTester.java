package eu.knowledge.engine.test_utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTester {
	private static final Logger LOG = LoggerFactory.getLogger(HttpTester.class);
	private URL url;
	private String method;
	private String body;
	private Map<String, String> headers;

	private boolean didRequest = false;
	private Integer gotStatus = null;
	private String gotBody = null;

	public HttpTester(URL url, String method, String body, Map<String, String> headers) {
		this.url = url;
		this.method = method;
		this.body = body;
		this.headers = headers;
	}

	public void expectStatus(int expectedStatus) {
		if (!didRequest) {
			doRequest();
		}
		if (expectedStatus != gotStatus) {
			LOG.warn("Unexpected status {}. Body:\n{}", gotStatus, gotBody);
		}
		assertEquals(expectedStatus, gotStatus);
	}

	private void doRequest() {
		HttpURLConnection conn;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(method);
			conn.setDoOutput(true);

			if (headers != null) {
				headers.forEach((k, v) -> {
					conn.setRequestProperty(k, v);
				});
			}

			if (body != null) {
				OutputStream outStream = conn.getOutputStream();
				OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "UTF-8");
				outStreamWriter.write(body);
				outStreamWriter.flush();
				outStreamWriter.close();
				outStream.close();
			}

			conn.connect();

			this.gotStatus = conn.getResponseCode();
			if (100 <= conn.getResponseCode() && conn.getResponseCode() <= 399) {
				this.gotBody = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			} else {
				this.gotBody = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
			}
			didRequest = true;
		} catch (IOException e) {
			fail();
		}
	}
}
