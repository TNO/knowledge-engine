package eu.interconnectproject.knowledge_engine.knowledgedirectory;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnowledgeDirectory {

	public static final Logger LOG = LoggerFactory.getLogger(KnowledgeDirectory.class);

	private static enum State {
		NEW, RUNNING, STOPPED
	}

	private final int port;
	private State state;
	private Server server;

	public KnowledgeDirectory(int port) {
		this.port = port;
		this.state = State.NEW;
	}

	public void start() throws Exception {
		if (state != State.NEW) {
			throw new IllegalStateException("Server already started or stopped");
		}
		this.state = State.RUNNING;

		LOG.info("Starting Knowledge Directory REST API on port {}.", port);

		server = new Server(port);

		ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

		ctx.setContextPath("/");
		server.setHandler(ctx);

		ServletHolder serHol = ctx.addServlet(ServletContainer.class, "/*");
		serHol.setInitOrder(1);
		serHol.setInitParameter("jersey.config.server.provider.packages",
				"eu.interconnectproject.knowledge_engine.knowledgedirectory");

		server.start();
	}

	public void join() throws InterruptedException {
		if (state != State.RUNNING) {
			throw new IllegalStateException("Can only stop join when it is running");
		}
		server.join();
	}

	public void stop() throws Exception {
		if (state != State.RUNNING) {
			throw new IllegalStateException("Can only stop server when it is running");
		}
		this.state = State.STOPPED;

		try {
			server.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		server.stop();

		server.destroy();
	}

}
