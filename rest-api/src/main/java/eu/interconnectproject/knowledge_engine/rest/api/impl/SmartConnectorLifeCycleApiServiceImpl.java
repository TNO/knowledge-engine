package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.rest.api.NotFoundException;
import eu.interconnectproject.knowledge_engine.rest.api.SmartConnectorLifeCycleApiService;
import eu.interconnectproject.knowledge_engine.rest.model.SmartConnector;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class SmartConnectorLifeCycleApiServiceImpl extends SmartConnectorLifeCycleApiService {

	private static final Logger LOG = LoggerFactory.getLogger(SmartConnectorLifeCycleApiServiceImpl.class);

	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	@Override
	public Response scDelete(String knowledgeBaseId, SecurityContext securityContext)
			throws NotFoundException {

		ResponseBuilder rb;

		if (knowledgeBaseId != null) {

			try {
				new URI(knowledgeBaseId);
			} catch (URISyntaxException e) {
				rb = Response.status(400).entity("Knowledge base not found, because its ID must be a valid URI.");
			}

			if (manager.hasKB(knowledgeBaseId)) {
				manager.deleteKB(knowledgeBaseId);
			} else {
				rb = Response.status(404).entity("Deletion of knowledge base failed, because it could not be found.");
			}
			rb = Response.ok();
		} else {
			rb = Response.status(Status.BAD_REQUEST).entity("Knowledge-Base-Id header should not be null.");
		}

		return rb.build();
	}

	@Override
	public Response scGet(String knowledgeBaseId, SecurityContext securityContext) throws NotFoundException {

		LOG.info("scGet called: {}", knowledgeBaseId);

		if (knowledgeBaseId == null) {
			return Response.ok().entity(convertToModel(this.manager.getKBs())).build();
		} else {
			try {
				new URI(knowledgeBaseId);
			} catch (URISyntaxException e) {
				return Response.status(400).entity("Knowledge base not found, because its ID must be a valid URI.")
						.build();
			}
			if (this.manager.hasKB(knowledgeBaseId)) {
				Set<RestKnowledgeBase> connectors = new HashSet<>();
				connectors.add(this.manager.getKB(knowledgeBaseId));
				return Response.ok().entity(convertToModel(connectors)).build();
			} else {
				return Response.status(404).entity("Knowledge base not found.").build();
			}
		}
	}

	private eu.interconnectproject.knowledge_engine.rest.model.SmartConnector[] convertToModel(
			Set<RestKnowledgeBase> kbs) {
		return kbs.stream().map((restKb) -> {
			return new eu.interconnectproject.knowledge_engine.rest.model.SmartConnector()
					.knowledgeBaseId(restKb.getKnowledgeBaseId().toString())
					.knowledgeBaseName(restKb.getKnowledgeBaseName())
					.knowledgeBaseDescription(restKb.getKnowledgeBaseDescription());
		}).toArray(eu.interconnectproject.knowledge_engine.rest.model.SmartConnector[]::new);
	}

	@Override
	public Response scPost(SmartConnector inlineObject, SecurityContext securityContext) throws NotFoundException {
		URI kbId;
		try {
			kbId = new URI(inlineObject.getKnowledgeBaseId());
		} catch (URISyntaxException e) {
			return Response.status(400).entity("Knowledge base ID must be a valid URI.").build();
		}

		final String kbDescription = inlineObject.getKnowledgeBaseDescription();
		final String kbName = inlineObject.getKnowledgeBaseName();

		if (this.manager.hasKB(inlineObject.getKnowledgeBaseId())) {
			return Response.status(400).entity("That knowledge base ID is already in use.").build();
		}

		// Tell the manager to create a KB, store it, and have it set up a SC etc.
		this.manager.createKB(new eu.interconnectproject.knowledge_engine.rest.model.SmartConnector()
				.knowledgeBaseId(kbId.toString()).knowledgeBaseName(kbName).knowledgeBaseDescription(kbDescription));

		return Response.ok().build();
	}
}
