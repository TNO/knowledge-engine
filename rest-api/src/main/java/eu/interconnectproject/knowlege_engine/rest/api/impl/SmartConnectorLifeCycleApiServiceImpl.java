package eu.interconnectproject.knowlege_engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorBuilder;
import eu.interconnectproject.knowlege_engine.rest.api.ApiResponseMessage;
import eu.interconnectproject.knowlege_engine.rest.api.NotFoundException;
import eu.interconnectproject.knowlege_engine.rest.api.SmartConnectorLifeCycleApiService;
import eu.interconnectproject.knowlege_engine.rest.model.InlineObject;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class SmartConnectorLifeCycleApiServiceImpl extends SmartConnectorLifeCycleApiService {

	@Inject
	SmartConnectorStore store;

	@Override
	public Response scDelete(@NotNull String knowledgeBaseId, SecurityContext securityContext)
			throws NotFoundException {
		// do some magic!
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "scDelete!")).build();
	}

	@Override
	public Response scGet(String knowledgeBaseId, SecurityContext securityContext) throws NotFoundException {
		if (knowledgeBaseId == null) {
			return Response.ok().entity(this.store.getKBs()).build();
		} else {
			URI kbId;
			try {
				kbId = new URI(knowledgeBaseId);
			} catch (URISyntaxException e) {
				return Response.status(404).entity("Knowledge base not found, because its ID must be a valid URI.").build();
			}
			if (this.store.hasKB(kbId)) {
				return Response.ok().entity(new eu.interconnectproject.knowlege_engine.rest.model.SmartConnector[] { this.store.getKB(kbId) }).build();
			} else {
				return Response.status(404).entity("Knowledge base not found.").build();
			}
		}
	}

	@Override
	public Response scPost(InlineObject inlineObject, SecurityContext securityContext) throws NotFoundException {
		URI nonFinalKbId;
		try {
			nonFinalKbId = new URI(inlineObject.getKnowledgeBaseId());
		} catch (URISyntaxException e) {
			return Response.status(400).entity("Knowledge base ID must be a valid URI.").build();
		}

		// Not-so-nice hack to have it be a `final` variable and otherwise respond with status 400.
		final URI kbId = nonFinalKbId;
		final String kbDescription = inlineObject.getKnowledgeBaseDescription();
		final String kbName = inlineObject.getKnowledgeBaseName();

		if (this.store.containsSC(kbId)) {
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
		this.store.putSC(kbId, sc);
		this.store.putKB(kbId, new eu.interconnectproject.knowlege_engine.rest.model.SmartConnector()
			.knowledgeBaseId(kbId.toString())
			.knowledgeBaseName(kbName)
			.knowledgeBaseDescription(kbDescription)
		);

		return Response.ok().build();
	}
}
