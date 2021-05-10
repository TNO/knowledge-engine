package eu.interconnectproject.knowledge_engine.knowledgedirectory;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import eu.interconnectproject.knowledge_engine.knowledgedirectory.api.KerApiService;
import eu.interconnectproject.knowledge_engine.knowledgedirectory.api.NotFoundException;
import eu.interconnectproject.knowledge_engine.knowledgedirectory.model.KnowledgeEngineRuntime;

public class KerApiImpl extends KerApiService {

	private final Map<String, KnowledgeEngineRuntime> kers = new ConcurrentHashMap<>();

	private void cleanupExpired() {
		OffsetDateTime threshold = OffsetDateTime.now().minusSeconds(Main.KER_LEASE_SECONDS);
		kers.entrySet().removeIf(e -> e.getValue().getLastRenew().isBefore(threshold));
	}

	@Override
	public Response kerGet(SecurityContext securityContext) throws NotFoundException {
		cleanupExpired();
		return Response.status(200).entity(kers.values()).build();
	}

	@Override
	public Response kerPost(KnowledgeEngineRuntime knowledgeEngineRuntime, SecurityContext securityContext)
			throws NotFoundException {
		cleanupExpired();

		// Required data present?
		if (knowledgeEngineRuntime.getHostname() == null || knowledgeEngineRuntime.getPort() == null
				|| knowledgeEngineRuntime.getProtocolVersion() == null) {
			return Response.status(400).entity("Data was not valid").build();
		}

		// Does it already exist?
		String id = knowledgeEngineRuntime.getHostname() + "-" + knowledgeEngineRuntime.getPort();
		if (kers.containsKey(id)) {
			return Response.status(409).entity(id).build();
		}

		// TODO probably check if it can be reached by the Knowledge Directory itself

		// Apparently everything was ok
		knowledgeEngineRuntime.setId(id);
		knowledgeEngineRuntime.setLastRenew(OffsetDateTime.now());
		kers.put(id, knowledgeEngineRuntime);

		return Response.status(201).entity(id).build();
	}

	@Override
	public Response kerKerIdGet(String kerId, SecurityContext securityContext) throws NotFoundException {
		cleanupExpired();
		KnowledgeEngineRuntime knowledgeEngineRuntime = kers.get(kerId);
		if (knowledgeEngineRuntime == null) {
			return Response.status(404).entity("Smart Connector Runtime not found").build();
		}
		return Response.status(200).entity(knowledgeEngineRuntime).build();
	}

	@Override
	public Response kerKerIdDelete(String kepId, SecurityContext securityContext) throws NotFoundException {
		cleanupExpired();
		if (kers.remove(kepId) == null) {
			return Response.status(404).entity("Smart Connector Runtime not found").build();
		}
		return Response.status(200).build();
	}

	@Override
	public Response kerKerIdRenewPost(String kerId, SecurityContext securityContext) throws NotFoundException {
		cleanupExpired();
		KnowledgeEngineRuntime knowledgeEngineRuntime = kers.get(kerId);
		if (knowledgeEngineRuntime == null) {
			return Response.status(404).entity("Smart Connector Runtime not found").build();
		} else {
			knowledgeEngineRuntime.setLastRenew(OffsetDateTime.now());
			return Response.status(204).build();
		}
	}

}
