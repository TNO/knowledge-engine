package eu.interconnectproject.knowlege_engine.rest.api;

import java.util.Arrays;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		Server server = new Server(8080);

		ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

		ctx.setContextPath("/");
		server.setHandler(ctx);

//		ServletHolder serHol2 = ctx.addServlet(AsyncServlet.class, "/rest/sc/handle");
//		serHol2.setInitOrder(1);

		ServletHolder serHol = ctx.addServlet(ServletContainer.class, "/rest/*");
		serHol.setInitOrder(1);
//		serHol.setInitParameter("jersey.config.server.provider.packages",
//				"eu.interconnectproject.knowlege_engine.rest");
		serHol.setInitParameter("javax.ws.rs.Application" , "eu.interconnectproject.knowlege_engine.rest.api.impl.Application");

		ServletHandler sh = serHol.getServletHandler();
		
		LOG.info("Filters: {}", Arrays.asList(sh.getFilters()));
		
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
