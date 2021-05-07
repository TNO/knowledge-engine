package eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.impl;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.*;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.*;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.AnswerMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.AskMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.ErrorMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.PostMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.ReactMessage;

import java.util.List;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.NotFoundException;

import java.io.InputStream;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-04-30T11:30:55.605099+02:00[Europe/Amsterdam]")
public class MessagingApiServiceImpl extends MessagingApiService {
    @Override
    public Response messagingAnswermessagePost(AnswerMessage answerMessage, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response messagingAskmessagePost(AskMessage askMessage, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response messagingErrormessagePost(ErrorMessage errorMessage, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response messagingPostmessagePost(PostMessage postMessage, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response messagingReactmessagePost(ReactMessage reactMessage, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
}
