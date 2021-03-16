package eu.interconnectproject.knowlege_engine.rest.api.impl;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application extends ResourceConfig {

	private static final Logger LOG = LoggerFactory.getLogger(Application.class);

	public Application() {
		LOG.info("application");
		register(new SmartConnectorStoreBinder());
	}
}
