package eu.interconnectproject.knowledge_engine.rest.api.impl;

import eu.interconnectproject.knowledge_engine.rest.api.*;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import eu.interconnectproject.knowledge_engine.rest.api.NotFoundException;
import eu.interconnectproject.knowledge_engine.rest.model.AskResult;
import eu.interconnectproject.knowledge_engine.rest.model.PostResult;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class ProactiveApiServiceImpl extends ProactiveApiService {

	private RestKnowledgeBaseManager store = RestKnowledgeBaseManager.newInstance();

	@Override
	public Response scAskPost(@NotNull String knowledgeBaseId, @NotNull String knowledgeInteractionId,
			List<Map<String, String>> bindings, SecurityContext securityContext) throws NotFoundException {
		var kb = this.store.getKB(knowledgeBaseId);
		if (kb == null) {
			return Response.status(404).entity("Smart connector not found, because its ID is unknown.").build();
		}

		if (!kb.hasKnowledgeInteraction(knowledgeInteractionId)) {
			return Response.status(404).entity("Knowledge Interaction not found, because its ID is unknown.").build();
		}

		AskResult ar;
		try {
			ar = kb.ask(knowledgeInteractionId, bindings);
		} catch (URISyntaxException | InterruptedException | ExecutionException e) {
			return Response.status(500)
					.entity("Something went wrong while sending a POST or while waiting on the REACT.").build();
		}
		return Response.ok().entity(ar).build();
	}

	@Override
	public Response scPostPost(@NotNull String knowledgeBaseId, @NotNull String knowledgeInteractionId,
			List<Map<String, String>> bindings, SecurityContext securityContext) throws NotFoundException {
		var kb = this.store.getKB(knowledgeBaseId);
		if (kb == null) {
			return Response.status(404).entity("Smart connector not found, because its ID is unknown.").build();
		}

		if (!kb.hasKnowledgeInteraction(knowledgeInteractionId)) {
			return Response.status(404).entity("Knowledge Interaction not found, because its ID is unknown.").build();
		}

		PostResult pr;
		try {
			pr = kb.post(knowledgeInteractionId, bindings);
		} catch (URISyntaxException | InterruptedException | ExecutionException e) {
			return Response.status(500)
					.entity("Something went wrong while sending a POST or while waiting on the REACT.").build();
		}
		return Response.ok().entity(pr).build();
	}
}
