package org.verapdf.crawler.app.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.List;

@Path("/document-properties")
public class DocumentPropertyResource {

    @GET
    @Path("/{propertyName}/values")
    public List<String> getDocumentPropertyValues(@PathParam("propertyName") String propertyName, @QueryParam("domain") String domain) {
        // todo: return the list of all values for property in the selected domain (used for dropdowns)
        return null;
    }
}
