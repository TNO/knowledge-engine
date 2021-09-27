package eu.knowledge.engine.smartconnector.api;

import java.util.Iterator;
import java.util.ServiceLoader;

public class SmartConnectorSPI {

	static ServiceLoader<SmartConnectorProvider> loader = ServiceLoader.load(SmartConnectorProvider.class);

	public static Iterator<SmartConnectorProvider> providers(boolean refresh) {
		if (refresh) {
			loader.reload();
		}
		return loader.iterator();
	}

}
