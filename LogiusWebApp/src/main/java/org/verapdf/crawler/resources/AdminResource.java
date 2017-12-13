package org.verapdf.crawler.resources;

import org.verapdf.crawler.ResourceManager;
import org.verapdf.crawler.tools.AbstractService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Maksim Bezrukov
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

	private final ResourceManager resourceManager;

	public AdminResource(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	@POST
	@Path("/service")
	public Response startService(@QueryParam("name") String name) {
		AbstractService service = resourceManager.getService(name);
		if (service == null) {
			throw new WebApplicationException("Service " + name + " not found", Response.Status.NOT_FOUND);
		}
		service.start();

		return Response.ok().build();
	}
}
