package org.verapdf.crawler.resources;

import org.verapdf.crawler.db.document.ReportDocumentDao;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/document-properties")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentPropertyResource {

    private final ReportDocumentDao reportDocumentDao;

    public DocumentPropertyResource(ReportDocumentDao reportDocumentDao) {
        this.reportDocumentDao = reportDocumentDao;
    }

    @GET
    @Path("/{propertyName}/values")
    public List<String> getDocumentPropertyValues(@PathParam("propertyName") String propertyName,
                                                  @QueryParam("domain") String domain,
                                                  @QueryParam("propertyValueFilter") String propertyValueFilter) {
        return reportDocumentDao.getMatchingPropertyValues(domain, propertyName, propertyValueFilter);
    }
}
