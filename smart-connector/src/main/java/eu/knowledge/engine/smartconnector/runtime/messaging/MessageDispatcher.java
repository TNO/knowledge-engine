package eu.knowledge.engine.smartconnector.runtime.messaging;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.messaging.KnowledgeMessage;
import eu.knowledge.engine.smartconnector.runtime.KeRuntime;
import eu.knowledge.engine.smartconnector.runtime.KnowledgeDirectoryProxy;
import eu.knowledge.engine.smartconnector.runtime.KnowledgeDirectoryProxyListener;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.factories.MessagingApiServiceFactory;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.factories.SmartConnectorManagementApiServiceFactory;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.model.KnowledgeEngineRuntimeDetails;

/**
 * The {@link MessageDispatcher} is responsible for sending messages between
 * {@link SmartConnector}s (also know as the SPARQL+ protocol).
 *
 * Within a single JVM you can have multiple {@link SmartConnector}s. Such a JVM
 * is called a Knowledge Engine Runtime. Message exchange between those
 * {@link SmartConnector}s happens in-memory. For message exchange between
 * Knowledge Engine Runtimes, a JSON- and REST-based peer-to-peer protocol is
 * used. Knowledge Engine Runtimes communicate directly with each other, but the
 * KnowledgeDirectory is used as a bootstrapping mechanism for peers to find
 * each other.
 *
 * The {@link MessageDispatcher} is responsible for communication between
 * {@link SmartConnector} within the same Knowledge Engine Runtime, for
 * communication between {@link SmartConnector}s in different Knowledge Engine
 * Runtimes and for communication with the Knowledge Directory.
 *
 * The {@link MessageDispatcher} is configured in the {@link KeRuntime} class.
 */
public class MessageDispatcher implements KnowledgeDirectoryProxy {

	private static final Logger LOG = LoggerFactory.getLogger(MessageDispatcher.class);

	public static final String PEER_PROTOCOL = "http";

	private static enum State {
		NEW, RUNNING, STOPPED
	}

	private final int myPort;
	private final URI myExposedUrl;
	private final URI kdUrl;

	private boolean useEdc = false;
	private State state;
	private Server httpServer;

	private KnowledgeDirectoryConnection knowledgeDirectoryConnectionManager = null;
	private LocalSmartConnectorConnectionManager localSmartConnectorConnectionsManager = null;
	private RemoteKerConnectionManager remoteSmartConnectorConnectionsManager = null;

	private final List<KnowledgeDirectoryProxyListener> knowledgeDirectoryProxyListeners = new CopyOnWriteArrayList<>();

	/**
	 * Messages of which the fromKnowledgeBaseId is unknown. This message is not
	 * sent to the local Smart Connector, since that will lead to an error while
	 * replying to this message. Instead, we wait until the (remote) known Smart
	 * Connectors change, and then try again.
	 */
	private final List<KnowledgeMessage> undeliverableMail = new CopyOnWriteArrayList<>();
	private final static int MAX_ENTRIES_UNDELIVERABLE_MAIL = 5000;

	/**
	 * Construct the {@link MessageDispatcher} in a distributed mode, with an
	 * external KnowledgeDirectory.
	 *
	 * @param myPort
	 * @param myExposedUrl
	 * @param kdUrl
	 */
	public MessageDispatcher(int myPort, URI myExposedUrl, URI kdUrl, boolean useEdc) {
		this.myPort = myPort;
		this.myExposedUrl = myExposedUrl;
		this.kdUrl = kdUrl;
		this.state = State.NEW;
		this.useEdc = useEdc;
	}

	public MessageDispatcher(int myPort, URI myExposedUrl, URI kdUrl) {
		this(myPort, myExposedUrl, kdUrl, false);
	}

	/**
	 * Construct the {@link MessageDispatcher} in a JVM-only mode, without an
	 * external Knowledge Directory.
	 */
	public MessageDispatcher() {
		this(0, null, null, false);
	}

	boolean runsInDistributedMode() {
		return kdUrl != null;
	}

	public void start() throws Exception {
		// Check and update state
		if (state != State.NEW) {
			throw new IllegalStateException("DistributedMesasgeDispatcher already started or stopped");
		}
		this.state = State.RUNNING;

		// Start the LocalSmartConnnectorConnectionsManager
		localSmartConnectorConnectionsManager = new LocalSmartConnectorConnectionManager(this);
		localSmartConnectorConnectionsManager.start();

		if (runsInDistributedMode()) {
			// Start the RemoteSmartConnnectorConnectionsManager
			remoteSmartConnectorConnectionsManager = new RemoteKerConnectionManager(this, this.myExposedUrl, this.useEdc);
			getRemoteSmartConnectorConnectionsManager().start();

			URI myParticipantId = remoteSmartConnectorConnectionsManager.getEdcParticipantId();
			URI myEdcConnectorUrl = remoteSmartConnectorConnectionsManager.getEdcConnectorUrl();
			URI myEdcDataPlaneUrl = remoteSmartConnectorConnectionsManager.getEdcDataPlaneUrl();
			// Start Knowledge Directory Connection Manager
			this.knowledgeDirectoryConnectionManager = new KnowledgeDirectoryConnection(kdUrl, myExposedUrl, myParticipantId, myEdcConnectorUrl, myEdcDataPlaneUrl);

			this.getKnowledgeDirectoryConnectionManager().start();

			// Start HTTP Server
			this.startHttpServer();
		}
	}

	public void stop() throws Exception {
		if (state != State.RUNNING) {
			throw new IllegalStateException("Can only stop server when it is running");
		}
		this.state = State.STOPPED;

		// Stop the LocalSmartConnnectorConnectionsManager
		localSmartConnectorConnectionsManager.stop();

		if (runsInDistributedMode()) {
			// Stop the RemoteSmartConnnectorConnectionsManager
			getRemoteSmartConnectorConnectionsManager().stop();

			// Stop the connection with the Knowledge Directory
			knowledgeDirectoryConnectionManager.stop();

			// Stop HTTP server
			this.stopHttpServer();
		}
	}

	private void startHttpServer() throws Exception {
		LOG.info("Starting Inter-KER REST API on port {}.", myPort);

		httpServer = new Server(myPort);

		ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

		ctx.setContextPath("/");
		httpServer.setHandler(ctx);

		ServletHolder serHol = new ServletHolder(ServletContainer.class);
		ctx.addServlet(serHol, "/*");
		serHol.setInitOrder(1);
		serHol.setInitParameter("jersey.config.server.provider.packages",
				"eu.knowledge.engine.smartconnector.runtime.messaging");
		serHol.setInitParameter("port", String.valueOf(myPort));

		SmartConnectorManagementApiServiceFactory.registerSmartConnectorManagementApiService(myPort,
				remoteSmartConnectorConnectionsManager);
		MessagingApiServiceFactory.registerMessagingApiService(myPort,
				remoteSmartConnectorConnectionsManager.getMessageReceiver());

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
		SmartConnectorManagementApiServiceFactory.unregisterSmartConnectorManagementApiService(myPort);
		MessagingApiServiceFactory.unregisterMessagingApiService(myPort);
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
		boolean success = false;
		LocalSmartConnectorConnection localSender = localSmartConnectorConnectionsManager
				.getLocalSmartConnectorConnection(message.getToKnowledgeBase());
		if (localSender != null) {
			localSender.deliverToLocalSmartConnector(message);
			success = true;
		} else {
			if (runsInDistributedMode()) {
				// must be a remote smart connector then
				RemoteKerConnection remoteSender = getRemoteSmartConnectorConnectionsManager()
						.getRemoteKerConnection(message.getToKnowledgeBase());
				if (remoteSender != null) {
					remoteSender.sendToRemoteSmartConnector(message);
					success = true;
				}
			}
		}
		if (!success) {
			// Cannot find a remote or a local sender
			throw new IOException("Could not send message " + message.getMessageId() + ", the Knowledge Base "
					+ message.getToKnowledgeBase() + " is not known");
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
		Set<URI> knowledgeBaseIds = this.getKnowledgeBaseIds();
		if (!knowledgeBaseIds.contains(message.getFromKnowledgeBase())) {
			// TODO this is not the prettiest solution, it is a potential memory leak and
			// might cause other Smart Connectors to wait for responses indefinately
			LOG.warn("Received message from unknown Knowledge Base: " + message.getFromKnowledgeBase()
					+ ", I only know " + knowledgeBaseIds);

			if (this.undeliverableMail.size() >= MAX_ENTRIES_UNDELIVERABLE_MAIL) {
				int numMessagesToRemove = this.undeliverableMail.size() - MAX_ENTRIES_UNDELIVERABLE_MAIL;
				for (int i = 0; i <= numMessagesToRemove; i++) {
					KnowledgeMessage removedMessage = this.undeliverableMail.remove(0);
					LOG.warn("Too many undelivered messages. Removing the oldest message with ID "
							+ removedMessage.getMessageId() + " to make space for a new message.");
				}
			}
			this.undeliverableMail.add(message);

			// Force a refresh of the KERs from the Knowledge Directory
			this.remoteSmartConnectorConnectionsManager.scheduleQueryKnowledgeDirectory();
		} else {
			LocalSmartConnectorConnection cm = localSmartConnectorConnectionsManager
					.getLocalSmartConnectorConnection(message.getToKnowledgeBase());
			if (cm != null) {
				cm.deliverToLocalSmartConnector(message);
			} else {
				throw new IOException("Could not deliver message " + message.getMessageId() + ", the Knowledge Base "
						+ message.getToKnowledgeBase() + " is not known locally");
			}
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

	LocalSmartConnectorConnectionManager getLocalSmartConnectorConnectionManager() {
		return localSmartConnectorConnectionsManager;
	}

	KnowledgeDirectoryConnection getKnowledgeDirectoryConnectionManager() {
		return knowledgeDirectoryConnectionManager;
	}

	/**
	 * Implementation of the {@link KnowledgeDirectoryProxy}
	 */
	@Override
	public Set<URI> getKnowledgeBaseIds() {
		HashSet<URI> set = new HashSet<>();
		set.addAll(this.localSmartConnectorConnectionsManager.getLocalSmartConnectorIds());
		if (runsInDistributedMode()) {
			set.addAll(this.getRemoteSmartConnectorConnectionsManager().getRemoteSmartConnectorIds());
		}
		return set;
	}

	@Override
	public void addListener(KnowledgeDirectoryProxyListener listener) {
		knowledgeDirectoryProxyListeners.add(listener);
	}

	@Override
	public void removeListener(KnowledgeDirectoryProxyListener listener) {
		knowledgeDirectoryProxyListeners.remove(listener);
	}

	/**
	 * Try to deliver messages of which the fromKnowledgeBaseId is unknown. New
	 * remote Smart Connectors start sending messages right away, often before the
	 * MessageDispatcher even knows of its existence.
	 *
	 * TODO This solves the problem for now, but it would be better if the other
	 * side retries to send the message instead. This is a potential memory leak and
	 * might cause other Smart Connectors to keep waiting for a response that never
	 * comes.
	 */
	private void tryDeliverUndeliveredMail() throws IOException {
		Iterator<KnowledgeMessage> it = undeliverableMail.iterator();
		Set<KnowledgeMessage> toBeRemoved = new HashSet<>();
		while (it.hasNext()) {
			KnowledgeMessage message = it.next();
			Set<URI> knowledgeBaseIds = this.getKnowledgeBaseIds();
			if (knowledgeBaseIds.contains(message.getFromKnowledgeBase())) {
				LOG.info("I can now deliver the message I received from " + message.getFromKnowledgeBase());
				deliverToLocalSmartConnector(message);
				toBeRemoved.add(message);
			}
		}
		undeliverableMail.removeAll(toBeRemoved);
	}

	void notifySmartConnectorsChanged() {
		LOG.info("Notifying " + knowledgeDirectoryProxyListeners.size() + " listeners about the "
				+ getKnowledgeBaseIds().size() + " knowledge bases in the KE");
		for (KnowledgeDirectoryProxyListener knowledgeDirectoryProxyListener : knowledgeDirectoryProxyListeners) {
			knowledgeDirectoryProxyListener.knowledgeBaseIdSetChanged();
		}
		try {
			tryDeliverUndeliveredMail();
		} catch (IOException e) {
			LOG.error("Could not deliver message", e);
		}
	}

	protected boolean usesEdc() {
		return this.useEdc;
	}

}
