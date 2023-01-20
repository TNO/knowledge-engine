package eu.knowledge.engine.test_utils;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * This class allows you to easily let JUnit catch assertion errors from other 
 * threads. For usage, see eu.knowledge.engine.rest.api.TestQuickScPosts
 */
public class AsyncTester {
	private Thread thread;
	private AssertionError exc;

	public AsyncTester(final Runnable runnable) {
		thread = new Thread(new Runnable() {
			public void run() {
				try {
					runnable.run();
				} catch (AssertionError e) {
					exc = e;
				}
			}
		});
	}

	public void start() {
		thread.start();
	}

	public void joinAndRethrow() {
		try {
			thread.join();
		} catch (InterruptedException e) {
			fail();
		}
		if (exc != null) throw exc;
	}
}
