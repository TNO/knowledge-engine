package eu.interconnectproject.knowledge_engine.knowledgedirectory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static final int DEFAULT_PORT = 8282;
	public static final int KER_LEASE_SECONDS = 60;

	public static void main(String[] args) throws Exception {
		int port = DEFAULT_PORT;

		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				LOG.error("{} is not a valid port number.", args[0]);
				System.exit(1);
			}
		}

		KnowledgeDirectory knowledgeDirectory = new KnowledgeDirectory(port);

		try {
			knowledgeDirectory.start();
			knowledgeDirectory.join();
		} catch (Exception e) {
			LOG.error("Error starting server");
		} finally {
			knowledgeDirectory.stop();
		}

	}

}
