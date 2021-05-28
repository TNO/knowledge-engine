package eu.interconnectproject.knowledge_engine.rest.api;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestServer {

	private static final Logger LOG = LoggerFactory.getLogger(RestServer.class);

	private static final int DEFAULT_PORT = 8280;

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

		LOG.info("Starting Knowledge Engine REST API on port {}.", port);
		Server server = new Server(port);
		System.setProperty("org.jboss.logging.provider", "slf4j");
		ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		ctx.setContextPath("/");
		server.setHandler(ctx);

		ResourceConfig rc = new ResourceConfig();
		rc.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
		rc.property(ServerProperties.WADL_FEATURE_DISABLE, true);

		rc.packages("eu.interconnectproject.knowledge_engine.rest");
		ServletContainer sc = new ServletContainer(rc);
		ServletHolder jerseyServlet = new ServletHolder(sc);

		ctx.addServlet(jerseyServlet, "/rest/*");

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
