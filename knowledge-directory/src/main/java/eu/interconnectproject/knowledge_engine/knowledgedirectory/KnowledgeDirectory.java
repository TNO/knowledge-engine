package eu.interconnectproject.knowledge_engine.knowledgedirectory;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnowledgeDirectory {

	private static final Logger LOG = LoggerFactory.getLogger(KnowledgeDirectory.class);

	public static final int DEFAULT_PORT = 8282; // TODO make configurable
	public static final int SCR_LEASE_SECONDS = 60;

	public static void main(String[] args) {
		int port = DEFAULT_PORT;

		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				LOG.error("{} is not a valid port number.", args[0]);
				System.exit(1);
			}
		}

		LOG.info("Starting Knowledge Director REST API on port {}.", port);
		Server server = new Server(port);

		ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

		ctx.setContextPath("/");
		server.setHandler(ctx);

		ServletHolder serHol = ctx.addServlet(ServletContainer.class, "/*");
		serHol.setInitOrder(1);
		serHol.setInitParameter("jersey.config.server.provider.packages",
				"eu.interconnectproject.knowledge_engine.knowledgedirectory");

		try {
			server.start();
			server.join();
		} catch (Exception ex) {
			LOG.error("{}", ex);
		} finally {
			server.destroy();
		}
	}

}
