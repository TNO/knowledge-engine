package eu.knowledge.engine.rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import eu.knowledge.engine.rest.api.RestServer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestScLifeCycle {
	private Thread thread;

	@BeforeAll
	public void setUpServer() throws InterruptedException {
		var r = new Runnable(){
			@Override
			public void run() {
				RestServer.main(new String[]{});
			}
		};
		this.thread = new Thread(r);
		this.thread.start();
		Thread.sleep(5000);
	}

	@Test
	public void testInvalidJson() throws IOException {
		URL url = new URL("http://localhost:8280/rest/sc");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "*/*");
		conn.setDoOutput(true);

		OutputStream outStream = conn.getOutputStream();
		OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "UTF-8");
		outStreamWriter.write("{\"bla\"{}");
		outStreamWriter.flush();
		outStreamWriter.close();
		outStream.close();

		conn.connect();

		int responseCode = conn.getResponseCode();
		assertEquals(400, responseCode);
	}

	@AfterAll
	public void cleanUp() {
		thread.interrupt();
	}
}
