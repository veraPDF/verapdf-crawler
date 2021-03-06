package org.verapdf.crawler.resources;

import io.dropwizard.hibernate.UnitOfWork;
import org.verapdf.crawler.ResourceManager;
import org.verapdf.crawler.db.DocumentDAO;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/document-properties")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentPropertyResource {

    private final ResourceManager resourceManager;

    public DocumentPropertyResource(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @GET
    @Path("/{propertyName}/values")
    @UnitOfWork
    public List<String> getDocumentPropertyValues(@PathParam("propertyName") String propertyName,
                                                  @QueryParam("domain") @NotNull String domain,
                                                  @QueryParam("propertyValueFilter") @NotNull String propertyValueFilter,
                                                  @QueryParam("limit") Integer limit) {
        return resourceManager.getDocumentDAO().getDocumentPropertyValues(propertyName, domain, propertyValueFilter, limit);
    }
}
