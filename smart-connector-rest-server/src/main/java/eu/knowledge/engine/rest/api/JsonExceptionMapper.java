package eu.knowledge.engine.rest.api;

import com.fasterxml.jackson.core.JsonProcessingException;

import eu.knowledge.engine.rest.model.ResponseMessage;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Since apparently Jersey gives a status 500 response when it encounters
 * unexpected input in request bodies, this exception mapper maps those
 * exceptions to a 400, since it is a bad request.
 */
@Provider
public class JsonExceptionMapper implements ExceptionMapper<JsonProcessingException> {
	@Override
	public Response toResponse(JsonProcessingException exception) {

		var response = new ResponseMessage();
		response.setMessageType("error");
		response.setMessage(exception.getClass().getSimpleName() + ": " + exception.getOriginalMessage());

		return Response.status(Response.Status.BAD_REQUEST).entity(response).type(MediaType.APPLICATION_JSON).build();
	}
}
