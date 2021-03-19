package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorBuilder;
import eu.interconnectproject.knowledge_engine.rest.api.NotFoundException;
import eu.interconnectproject.knowledge_engine.rest.api.SmartConnectorLifeCycleApiService;
import eu.interconnectproject.knowledge_engine.rest.model.InlineObject;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class SmartConnectorLifeCycleApiServiceImpl extends SmartConnectorLifeCycleApiService {

	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	@Override
	public Response scDelete(@NotNull String knowledgeBaseId, SecurityContext securityContext)
			throws NotFoundException {
		URI kbId;
		try {
			kbId = new URI(knowledgeBaseId);
		} catch (URISyntaxException e) {
			return Response.status(400).entity("Smart Connector not found, because its ID must be a valid URI.")
					.build();
		}

		if (manager.hasKB(knowledgeBaseId)) {
			manager.deleteKB(knowledgeBaseId);
		} else {
			return Response.status(404)
					.entity("Deletion of smart connector failed, because smart connector could not be found.").build();
		}

		return Response.ok().build();
	}

	@Override
	public Response scGet(String knowledgeBaseId, SecurityContext securityContext) throws NotFoundException {
		if (knowledgeBaseId == null) {
			return Response.ok().entity(convertToModel(this.manager.getKBs())).build();
		} else {
			URI kbId;
			try {
				kbId = new URI(knowledgeBaseId);
			} catch (URISyntaxException e) {
				return Response.status(400).entity("Smart Connector not found, because its ID must be a valid URI.")
						.build();
			}
			if (this.manager.hasKB(knowledgeBaseId)) {
				return Response.ok().entity(convertToModel(this.manager.getKBs())).build();
			} else {
				return Response.status(404).entity("Knowledge base not found.").build();
			}
		}
	}

	private SmartConnector[] convertToModel(Set<RestKnowledgeBase> kBs) {

		throw new RuntimeException("Not yet implemented!");

	}

	@Override
	public Response scPost(InlineObject inlineObject, SecurityContext securityContext) throws NotFoundException {
		URI nonFinalKbId;
		try {
			nonFinalKbId = new URI(inlineObject.getKnowledgeBaseId());
		} catch (URISyntaxException e) {
			return Response.status(400).entity("Knowledge base ID must be a valid URI.").build();
		}

		// Not-so-nice hack to have it be a `final` variable and otherwise respond with
		// status 400.
		final URI kbId = nonFinalKbId;
		final String kbDescription = inlineObject.getKnowledgeBaseDescription();
		final String kbName = inlineObject.getKnowledgeBaseName();

		if (this.manager.hasKB(inlineObject.getKnowledgeBaseId())) {
			return Response.status(400).entity("That knowledge base ID is already in use.").build();
		}

		// Store it in the map.
		this.manager.createKB(new eu.interconnectproject.knowledge_engine.rest.model.SmartConnector()
				.knowledgeBaseId(kbId.toString()).knowledgeBaseName(kbName).knowledgeBaseDescription(kbDescription));

		return Response.ok().build();
	}
}
