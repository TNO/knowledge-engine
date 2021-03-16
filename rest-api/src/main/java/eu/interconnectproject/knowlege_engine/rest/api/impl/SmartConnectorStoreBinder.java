package eu.interconnectproject.knowlege_engine.rest.api.impl;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class SmartConnectorStoreBinder extends AbstractBinder {
	@Override
	protected void configure() {
		bind(SmartConnectorStore.class).to(SmartConnectorStore.class);
	}
}