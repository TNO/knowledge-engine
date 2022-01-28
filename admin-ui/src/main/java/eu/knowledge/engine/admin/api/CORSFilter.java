package eu.knowledge.engine.admin.api;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;


public class CORSFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request,
            ContainerResponseContext response) throws IOException {
    	response.getHeaders().add("Access-Control-Allow-Origin", "*");
    	response.getHeaders().add("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, HEAD");
    	response.getHeaders().add("Access-Control-Allow-Headers","Accept, Content-Type, Knowledge-Base-Id, Knowledge-Interaction-Id");
    }
}
