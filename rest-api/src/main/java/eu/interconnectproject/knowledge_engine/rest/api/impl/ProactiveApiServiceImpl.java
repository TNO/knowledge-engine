package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import eu.interconnectproject.knowledge_engine.rest.api.NotFoundException;
import eu.interconnectproject.knowledge_engine.rest.api.ProactiveApiService;
import eu.interconnectproject.knowledge_engine.rest.model.AskResult;
import eu.interconnectproject.knowledge_engine.rest.model.KnowledgeInteractionWithId;
import eu.interconnectproject.knowledge_engine.rest.model.PostResult;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class ProactiveApiServiceImpl extends ProactiveApiService {

	private RestKnowledgeBaseManager store = RestKnowledgeBaseManager.newInstance();

	@Override
	public Response scAskPost(String knowledgeBaseId, String knowledgeInteractionId, List<Map<String, String>> bindings,
			SecurityContext securityContext) throws NotFoundException {

		ResponseBuilder rb;

		if (knowledgeBaseId != null && knowledgeInteractionId != null) {

			var kb = this.store.getKB(knowledgeBaseId);
			if (kb == null) {
				rb = Response.status(404).entity("Smart connector not found, because its ID is unknown.");
			} else {

				try {
					new URI(knowledgeInteractionId);
				} catch (URISyntaxException e) {
					rb = Response.status(400)
							.entity("Knowledge interaction not found, because its ID must be a valid URI.");
				}

				if (!kb.hasKnowledgeInteraction(knowledgeInteractionId)) {
					rb = Response.status(404).entity("Knowledge Interaction not found, because its ID is unknown.");
				} else {

					KnowledgeInteractionWithId ki = kb.getKnowledgeInteraction(knowledgeInteractionId);
					if (!ki.getKnowledgeInteractionType().equals("AskKnowledgeInteraction")) {
						rb = Response.status(400).entity(
								"Given Knowledge Interaction ID should have type AskKnowledgeInteraction and not "
										+ ki.getKnowledgeInteractionType() + ".");
					} else {

						AskResult ar;
						try {
							ar = kb.ask(knowledgeInteractionId, bindings);
							rb = Response.ok().entity(ar);
						} catch (URISyntaxException | InterruptedException | ExecutionException e) {
							rb = Response.status(500)
									.entity("Something went wrong while sending a POST or while waiting on the REACT.");
						} catch (IllegalArgumentException e) {
							rb = Response.status(400).entity(e.getMessage());
						}
					}
				}
			}
		} else {
			rb = Response.status(Status.BAD_REQUEST)
					.entity("Both Knowledge-Base-Id and Knowledge-Interaction-Id headers should be non-null.");
		}

		return rb.build();
	}

	@Override
	public Response scPostPost(String knowledgeBaseId, String knowledgeInteractionId,
			List<Map<String, String>> bindings, SecurityContext securityContext) throws NotFoundException {

		ResponseBuilder rb;

		if (knowledgeBaseId != null && knowledgeInteractionId != null) {

			var kb = this.store.getKB(knowledgeBaseId);
			if (kb == null) {
				rb = Response.status(404).entity("Smart connector not found, because its ID is unknown.");
			} else {

				try {
					new URI(knowledgeInteractionId);
				} catch (URISyntaxException e) {
					rb = Response.status(400)
							.entity("Knowledge interaction not found, because its ID must be a valid URI.");
				}

				if (!kb.hasKnowledgeInteraction(knowledgeInteractionId)) {
					rb = Response.status(404).entity("Knowledge Interaction not found, because its ID is unknown.");
				} else {
					KnowledgeInteractionWithId ki = kb.getKnowledgeInteraction(knowledgeInteractionId);
					if (!ki.getKnowledgeInteractionType().equals("PostKnowledgeInteraction")) {
						rb = Response.status(400).entity(
								"Given Knowledge Interaction ID should have type PostKnowledgeInteraction and not "
										+ ki.getKnowledgeInteractionType() + ".");
					} else {

						PostResult pr;
						try {
							pr = kb.post(knowledgeInteractionId, bindings);
							rb = Response.ok().entity(pr);
						} catch (URISyntaxException | InterruptedException | ExecutionException e) {
							rb = Response.status(500)
									.entity("Something went wrong while sending a POST or while waiting on the REACT.");
						} catch (IllegalArgumentException e) {
							rb = Response.status(400).entity(e.getMessage());
						}
					}
				}
			}
		} else {
			rb = Response.status(Status.BAD_REQUEST)
					.entity("Both Knowledge-Base-Id and Knowledge-Interaction-Id headers should be non-null.");
		}
		return rb.build();
	}
}
