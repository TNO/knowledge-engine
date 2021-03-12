package eu.interconnectproject.knowlege_engine.rest.api.impl;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowlege_engine.rest.api.ApiResponseMessage;
import eu.interconnectproject.knowlege_engine.rest.api.NotFoundException;
import eu.interconnectproject.knowlege_engine.rest.api.ScApiService;
import eu.interconnectproject.knowlege_engine.rest.model.InlineObject;
import eu.interconnectproject.knowlege_engine.rest.model.InlineObject1;
import eu.interconnectproject.knowlege_engine.rest.model.Workaround;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-12T14:24:41.746+01:00[Europe/Berlin]")
public class ScApiServiceImpl extends ScApiService {

	private static final Logger LOG = LoggerFactory.getLogger(ScApiServiceImpl.class);

	private Map<eu.interconnectproject.knowlege_engine.rest.model.SmartConnector, SmartConnector> connectors;

	@Override
	public Response scAskPost(@NotNull String knowledgeBaseId, @NotNull String knowledgeInteractionId,
			List<Object> requestBody, SecurityContext securityContext) throws NotFoundException {
		
		
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
	public Response scPost(InlineObject inlineObject, SecurityContext securityContext) throws NotFoundException {
		// do some magic!
		LOG.info("scPost()");
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "scPost!")).build();
	}

	@Override
	public Response scPostPost(@NotNull String knowledgeBaseId, @NotNull String knowledgeInteractionId,
			List<Object> requestBody, SecurityContext securityContext) throws NotFoundException {
		// do some magic!
		LOG.info("scPostPost()");
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "scPostPost!")).build();
	}
}
