package eu.knowledge.engine.rest.api;

import eu.knowledge.engine.rest.model.ResponseMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Validation errors by default are returned as plain text and this class maps
 * those errors to our {@link ResponseMessage}.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
	@Override
	public Response toResponse(ConstraintViolationException exception) {

		var response = new ResponseMessage();
		response.setMessageType("error");
		response.setMessage(exception.getClass().getSimpleName() + ": " + prepareMessage(exception));

		return Response.status(Response.Status.BAD_REQUEST).entity(response).type(MediaType.APPLICATION_JSON).build();
	}

	private String prepareMessage(ConstraintViolationException exception) {
		StringBuilder message = new StringBuilder();
		for (ConstraintViolation<?> cv : exception.getConstraintViolations()) {
			message.append(cv.getPropertyPath() + " " + cv.getMessage() + "\n ");
		}
		return message.toString();
	}
}
