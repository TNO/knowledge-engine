package eu.knowledge.engine.rest;

import java.io.IOException;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.rest.api.RestServer;

public class RestServerHelper {
	private static final Logger LOG = LoggerFactory.getLogger(RestServerHelper.class);
	private static int WAIT_BEFORE_NEXT_POLL = 300;
	
	private Thread thread;

	public void start(int port) {
		var r = new Runnable() {
			@Override
			public void run() {
				RestServer.main(new String[] {String.format("%d", port)});
			}
		};
		this.thread = new Thread(r);
		this.thread.start();

		while (portAvailable(port)) {
			try {
				Thread.sleep(WAIT_BEFORE_NEXT_POLL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			LOG.info("Reattempting to check if server is up...");
		}
	}

	public void cleanUp() {
		thread.interrupt();
	}

	private static boolean portAvailable(int port) {
    try (Socket ignored = new Socket("localhost", port)) {
        return false;
    } catch (IOException ignored) {
        return true;
    }
	}
}
