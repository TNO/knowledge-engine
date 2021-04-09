package eu.interconnectproject.knowledge_engine.knowledgedirectory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import eu.interconnectproject.knowledge_engine.knowledgedirectory.api.NotFoundException;
import eu.interconnectproject.knowledge_engine.knowledgedirectory.api.ScrApiService;
import eu.interconnectproject.knowledge_engine.knowledgedirectory.model.SmartConnectorRuntime;

public class ScrApiImpl extends ScrApiService {

	private Map<String, SmartConnectorRuntime> scrs = new ConcurrentHashMap<>();

	private void cleanupExpired() {
		Date threshold = new Date(System.currentTimeMillis() - KnowledgeDirectory.SCR_LEASE_SECONDS * 1000);
		scrs.entrySet().removeIf(e -> e.getValue().getLastRenew().before(threshold));
	}

	@Override
	public Response scrGet(SecurityContext securityContext) throws NotFoundException {
		cleanupExpired();
		return Response.status(200).entity(scrs.values()).build();
	}

	@Override
	public Response scrPost(SmartConnectorRuntime smartConnectorRuntime, SecurityContext securityContext)
			throws NotFoundException {
		cleanupExpired();

		// Required data present?
		if (smartConnectorRuntime.getHostname() == null || smartConnectorRuntime.getPort() == null
				|| smartConnectorRuntime.getProtocolVersion() == null) {
			return Response.status(400).entity("Data was not valid").build();
		}

		// Does it already exist?
		String id = smartConnectorRuntime.getHostname() + "-" + smartConnectorRuntime.getPort();
		if (scrs.containsKey(id)) {
			return Response.status(400).entity("Smart Connector Runtime already registered").build();
		}

		// TODO probably check if it can be reached by the Knowledge Directory itself
		
		// Apparently everything was ok
		smartConnectorRuntime.setId(id);
		smartConnectorRuntime.setLastRenew(new Date());
		scrs.put(id, smartConnectorRuntime);

		return Response.ok().entity(id).build();
	}

	@Override
	public Response scrScrIdDelete(String scrId, SecurityContext securityContext) throws NotFoundException {
		cleanupExpired();
		if (scrs.remove(scrId) == null) {
			return Response.status(404).entity("Smart Connector Runtime not found").build();
		}
		return Response.ok().build();
	}

	@Override
	public Response scrScrIdGet(String scrId, SecurityContext securityContext) throws NotFoundException {
		cleanupExpired();
		SmartConnectorRuntime smartConnectorRuntime = scrs.get(scrId);
		if (smartConnectorRuntime == null) {
			return Response.status(404).entity("Smart Connector Runtime not found").build();
		}
		return Response.status(200).entity(smartConnectorRuntime).build();
	}

	@Override
	public Response scrScrIdRenewPost(String scrId, SecurityContext securityContext) throws NotFoundException {
		cleanupExpired();
		SmartConnectorRuntime smartConnectorRuntime = scrs.get(scrId);
		if (smartConnectorRuntime == null) {
			return Response.status(404).entity("Smart Connector Runtime not found").build();
		} else {
			smartConnectorRuntime.setLastRenew(new Date());
			return Response.ok().build();
		}
	}

}
