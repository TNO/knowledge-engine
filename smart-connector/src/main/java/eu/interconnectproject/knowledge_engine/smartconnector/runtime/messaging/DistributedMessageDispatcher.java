package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import java.io.IOException;
import java.net.URI;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.KnowledgeMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.KnowledgeEngineRuntimeDetails;

public class DistributedMessageDispatcher {

	private static final Logger LOG = LoggerFactory.getLogger(DistributedMessageDispatcher.class);

	public static final String PEER_PROTOCOL = "http";

	private static enum State {
		NEW, RUNNING, STOPPED
	}

	private final String myHostname;
	private final int myPort;
	private final String kdHostname;
	private final int kdPort;

	private State state;
	private Server httpServer;

	private KnowledgeDirectoryConnection knowledgeDirectoryConnectionManager = null;
	private LocalSmartConnectorConnectionManager localSmartConnectorConnectionsManager = null;
	private RemoteKerConnectionManager remoteSmartConnectorConnectionsManager = null;

	public DistributedMessageDispatcher(String myHostname, int myPort, String kdHostname, int kdPort) {
		this.myHostname = myHostname;
		this.myPort = myPort;
		this.kdHostname = kdHostname;
		this.kdPort = kdPort;
		this.state = State.NEW;
	}

	public void start() throws Exception {
		// Check and update state
		if (state != State.NEW) {
			throw new IllegalStateException("DistributedMesasgeDispatcher already started or stopped");
		}
		this.state = State.RUNNING;

		// Start Knowledge Directory Connection Manager
		this.knowledgeDirectoryConnectionManager = new KnowledgeDirectoryConnection(myHostname, myPort, kdHostname,
				kdPort);
		this.getKnowledgeDirectoryConnectionManager().start();

		// Start the LocalSmartConnnectorConnectionsManager
		localSmartConnectorConnectionsManager = new LocalSmartConnectorConnectionManager(this);
		localSmartConnectorConnectionsManager.start();

		// Start the RemoteSmartConnnectorConnectionsManager
		remoteSmartConnectorConnectionsManager = new RemoteKerConnectionManager(this);
		getRemoteSmartConnectorConnectionsManager().start();

		// Start HTTP Server
		this.startHttpServer();
	}

	public void stop() throws Exception {
		if (state != State.RUNNING) {
			throw new IllegalStateException("Can only stop server when it is running");
		}
		this.state = State.STOPPED;

		// Stop the LocalSmartConnnectorConnectionsManager
		localSmartConnectorConnectionsManager.stop();

		// Stop the RemoteSmartConnnectorConnectionsManager
		getRemoteSmartConnectorConnectionsManager().stop();

		// Stop HTTP server
		this.stopHttpServer();
	}

	private void startHttpServer() throws Exception {
		LOG.info("Starting Inter-KER REST API on port {}.", myPort);

		httpServer = new Server(myPort);

		ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

		ctx.setContextPath("/");
		httpServer.setHandler(ctx);

		ServletHolder serHol = ctx.addServlet(ServletContainer.class, "/*");
		serHol.setInitOrder(1);
		serHol.setInitParameter("jersey.config.server.provider.packages",
				"eu.interconnectproject.knowledge_engine.smartconnector.messaging");

		httpServer.start();
	}

	public void joinHttpServer() throws InterruptedException {
		if (state != State.RUNNING) {
			throw new IllegalStateException("Can only join server when it is running");
		}
		httpServer.join();
	}

	private void stopHttpServer() throws Exception {
		httpServer.stop();
		httpServer.destroy();
	}

	/**
	 * This is an internal method called by the
	 * {@link LocalSmartConnectorConnection}, which sends the message to the right
	 * (local or remote) sender
	 *
	 * @param message
	 * @throws IOException
	 */
	void sendToLocalOrRemoteSmartConnector(KnowledgeMessage message) throws IOException {
		LocalSmartConnectorConnection localSender = localSmartConnectorConnectionsManager
				.getLocalSmartConnectorConnection(message.getToKnowledgeBase());
		if (localSender != null) {
			localSender.deliverToLocalSmartConnector(message);
		} else {
			// must be a remote smart connector then
			RemoteKerConnection remoteSender = getRemoteSmartConnectorConnectionsManager()
					.getRemoteKerConnection(message.getToKnowledgeBase());
			if (remoteSender != null) {
				remoteSender.sendToRemoteSmartConnector(message);
			} else {
				// Cannot find a remote or a local sender
				throw new IOException("Could not send message " + message.getMessageId() + ", the Knowledge Base "
						+ message.getToKnowledgeBase() + " is not known");
			}
		}
	}

	/**
	 * This is an internal method called by the REMOTE receiver, which sends the
	 * message to the right local SmartConnector
	 *
	 * @param message
	 * @throws IOException
	 */
	void deliverToLocalSmartConnector(KnowledgeMessage message) throws IOException {
		LocalSmartConnectorConnection cm = localSmartConnectorConnectionsManager
				.getLocalSmartConnectorConnection(message.getToKnowledgeBase());
		if (cm != null) {
			cm.deliverToLocalSmartConnector(message);
		} else {
			throw new IOException("Could not deliver message " + message.getMessageId() + ", the Knowledge Base "
					+ message.getToKnowledgeBase() + " is not known locally");
		}
	}

	KnowledgeEngineRuntimeDetails getMyKnowledgeEngineRuntimeDetails() {
		KnowledgeEngineRuntimeDetails kers = new KnowledgeEngineRuntimeDetails();
		// TODO check state of the knowledgeDirectoryConnectionManager
		kers.setRuntimeId(getKnowledgeDirectoryConnectionManager().getMyKnowledgeDirectoryId());
		kers.setSmartConnectorIds(localSmartConnectorConnectionsManager.getLocalSmartConnectorIds().stream()
				.map(URI::toString).collect(Collectors.toList()));
		return kers;
	}

	public RemoteKerConnectionManager getRemoteSmartConnectorConnectionsManager() {
		return remoteSmartConnectorConnectionsManager;
	}

	KnowledgeDirectoryConnection getKnowledgeDirectoryConnectionManager() {
		return knowledgeDirectoryConnectionManager;
	}

}
