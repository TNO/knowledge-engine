package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.KnowledgeMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.MessageDispatcherEndpoint;

public class DistributedMessageDispatcher implements MessageDispatcherEndpoint {

	private static final Logger LOG = LoggerFactory.getLogger(DistributedMessageDispatcher.class);

	private static enum State {
		NEW, RUNNING, STOPPED
	}

	private String myHosntame;
	private int myPort;
	private String kdHostname;
	private String kdPort;

	private State state;
	private Server server;

	private KnowledgeDirectoryConnectionManager knowledgeDirectoryConnectionManager; // TODO
	private HashMap<URI, SmartConnectorConnectionManager> smartConnectorConnectionManagers; // TODO

	public DistributedMessageDispatcher(String myHosntame, int myPort, String kdHostname, String kdPort) {
		this.myHosntame = myHosntame;
		this.myPort = myPort;
		this.kdHostname = kdHostname;
		this.kdPort = kdPort;
		this.state = State.NEW;
	}

	public void startServer() throws Exception {
		if (state != State.NEW) {
			throw new IllegalStateException("Server already started or stopped");
		}
		this.state = State.RUNNING;

		LOG.info("Starting Inter-KER REST API on port {}.", myPort);

		server = new Server(myPort);

		ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

		ctx.setContextPath("/");
		server.setHandler(ctx);

		ServletHolder serHol = ctx.addServlet(ServletContainer.class, "/*");
		serHol.setInitOrder(1);
		serHol.setInitParameter("jersey.config.server.provider.packages",
				"eu.interconnectproject.knowledge_engine.smartconnector.messaging");

		server.start();
	}

	public void joinServer() throws InterruptedException {
		if (state != State.RUNNING) {
			throw new IllegalStateException("Can only stop join when it is running");
		}
		server.join();
	}

	public void stopServer() {
		if (state != State.RUNNING) {
			throw new IllegalStateException("Can only stop server when it is running");
		}
		this.state = State.STOPPED;

		server.destroy();
	}

	@Override
	public void send(KnowledgeMessage message) throws IOException {
		// TODO Auto-generated method stub

	}
}
