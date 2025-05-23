package eu.knowledge.engine.admin.api;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.admin.AdminUI;
import eu.knowledge.engine.rest.api.CORSFilter;

public class RestServer {

	private static final Logger LOG = LoggerFactory.getLogger(RestServer.class);

	private static final int DEFAULT_PORT = 8283;

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

		LOG.info("Starting admin UI RESTfull API on port {}.", port);
		Server server = new Server(port);
		System.setProperty("org.jboss.logging.provider", "slf4j");
		ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		ctx.setContextPath("/");
		server.setHandler(ctx);

		ResourceConfig rcRuntime = new ResourceConfig();
		rcRuntime.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
		rcRuntime.property(ServerProperties.WADL_FEATURE_DISABLE, true);
		rcRuntime.register(new CORSFilter());
		rcRuntime.packages("eu.knowledge.engine.rest");
		ServletContainer scRuntime = new ServletContainer(rcRuntime);
		ServletHolder jerseyRuntimeServlet = new ServletHolder(scRuntime);
		ctx.addServlet(jerseyRuntimeServlet, "/runtime/*");

		ResourceConfig rcAdmin = new ResourceConfig();
		rcAdmin.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
		rcAdmin.property(ServerProperties.WADL_FEATURE_DISABLE, true);
		rcAdmin.register(new CORSFilter());
		rcAdmin.packages("eu.knowledge.engine.admin.api");
		ServletContainer scAdmin = new ServletContainer(rcAdmin);
		ServletHolder jerseyAdminServlet = new ServletHolder(scAdmin);
		ctx.addServlet(jerseyAdminServlet, "/admin/*");

		try {
			server.start();
			AdminUI.newInstance(false);
			server.join();
		} catch (Exception ex) {
			LOG.error("{}", ex);
		} finally {
			try {
				server.stop();
			} catch (Exception e) {
				LOG.warn("Exception while stopping server. Will destroy server nonetheless.", e);
			}
			server.destroy();
		}
	}

}
