package org.verapdf.crawler.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/document-properties")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentPropertyResource {

    public DocumentPropertyResource() {

    }

    @GET
    @Path("/{propertyName}/values")
    public List<String> getDocumentPropertyValues(@PathParam("propertyName") String propertyName,
                                                  @QueryParam("domain") String domain,
                                                  @QueryParam("propertyValueFilter") String propertyValueFilter) {
        return null;
    }
}
