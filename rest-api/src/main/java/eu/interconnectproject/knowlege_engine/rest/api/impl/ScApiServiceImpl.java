package eu.interconnectproject.knowlege_engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorBuilder;
import eu.interconnectproject.knowlege_engine.rest.api.ApiResponseMessage;
import eu.interconnectproject.knowlege_engine.rest.api.NotFoundException;
import eu.interconnectproject.knowlege_engine.rest.api.ScApiService;
import eu.interconnectproject.knowlege_engine.rest.model.InlineObject;
import eu.interconnectproject.knowlege_engine.rest.model.InlineObject1;
import eu.interconnectproject.knowlege_engine.rest.model.Workaround;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-12T14:24:41.746+01:00[Europe/Berlin]")
public class ScApiServiceImpl extends ScApiService {

	private static final Logger LOG = LoggerFactory.getLogger(ScApiServiceImpl.class);

	private Map<URI, SmartConnector> connectors = new HashMap<>();

	@Override
	public Response scPost(InlineObject req, SecurityContext securityContext) throws NotFoundException {
		URI nonFinalKbId;
		try {
			nonFinalKbId = new URI(req.getKnowledgeBaseId());
		} catch (URISyntaxException e) {
			return Response.status(400).entity("Knowledge base ID must be a valid URI.").build();
		}

		// Not-so-nice hack to have it be a `final` variable and otherwise respond with status 400.
		final URI kbId = nonFinalKbId;
		final String kbDescription = req.getKnowledgeBaseDescription();
		final String kbName = req.getKnowledgeBaseName();

		if (this.connectors.containsKey(kbId)) {
			return Response.status(400).entity("That knowledge base ID is already in use.").build();
		}

		SmartConnector sc = SmartConnectorBuilder.newSmartConnector(new KnowledgeBase(){
			@Override
			public URI getKnowledgeBaseId() {
				return kbId;
			}

			@Override
			public String getKnowledgeBaseName() {
				return kbName;
			}

			@Override
			public String getKnowledgeBaseDescription() {
				return kbDescription;
			}

			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				// Do nothing. The REST API doesn't provide these signals (yet).
			}

			@Override
			public void smartConnectorConnectionLost(SmartConnector aSC) {
				// Do nothing. The REST API doesn't provide these signals (yet).
			}

			@Override
			public void smartConnectorConnectionRestored(SmartConnector aSC) {
				// Do nothing. The REST API doesn't provide these signals (yet).
			}

			@Override
			public void smartConnectorStopped(SmartConnector aSC) {
				// Do nothing. The REST API doesn't provide these signals (yet).
			}
		}).create();

		// Store it in the map.
		this.connectors.put(kbId, sc);

		return Response.ok().build();
	}

	@Override
	public Response scAskPost(@NotNull String knowledgeBaseId, @NotNull String knowledgeInteractionId,
			List<Map<String, String>> requestBody, SecurityContext securityContext) throws NotFoundException {
		
		
//		var sc = new eu.interconnectproject.knowlege_engine.rest.model.SmartConnector();
		
		
		
		LOG.info("scAskPost()");
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "scAskPost!")).build();
	}

	@Override
	public Response scDelete(@NotNull String knowledgeBaseId, SecurityContext securityContext)
			throws NotFoundException {
		// do some magic!
		LOG.info("scDelete()");
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "scDelete!")).build();
	}

	@Override
	public Response scGet(String knowledgeBaseId, SecurityContext securityContext) throws NotFoundException {
		// do some magic!
		LOG.info("scGet()");
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "scGet!")).build();
	}

	@Override
	public Response scHandleGet(@NotNull String knowledgeBaseId, SecurityContext securityContext)
			throws NotFoundException {
		// do some magic!
		LOG.info("scHandleGet()");
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "scHandleGet!")).build();
	}

	@Override
	public Response scHandlePost(@NotNull String knowledgeBaseId, @NotNull String knowledgeInteractionId,
			InlineObject1 inlineObject1, SecurityContext securityContext) throws NotFoundException {
		// do some magic!
		LOG.info("scHandlePost()");
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "scHandlePost!")).build();
	}

	@Override
	public Response scKiDelete(@NotNull String knowledgeBaseId, @NotNull String knowledgeInteractionId,
			SecurityContext securityContext) throws NotFoundException {
		// do some magic!
		LOG.info("scKiDelete()");
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "scKiDelete!")).build();
	}

	@Override
	public Response scKiGet(@NotNull String knowledgeBaseId, SecurityContext securityContext) throws NotFoundException {
		// do some magic!
		LOG.info("scKiGet()");
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "scKiGet!")).build();
	}

	@Override
	public Response scKiPost(@NotNull String knowledgeBaseId, Workaround workaround, SecurityContext securityContext)
			throws NotFoundException {
		// do some magic!
		LOG.info("scKiPost()");
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "scKiPost!")).build();
	}

	@Override
	public Response scPostPost(@NotNull String knowledgeBaseId, @NotNull String knowledgeInteractionId,
			List<Map<String, String>> requestBody, SecurityContext securityContext) throws NotFoundException {
		// do some magic!
		LOG.info("scPostPost()");
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "scPostPost!")).build();
	}
}
