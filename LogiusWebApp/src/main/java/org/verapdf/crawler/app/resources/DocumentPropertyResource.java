package org.verapdf.crawler.app.resources;

import org.verapdf.crawler.repository.document.ReportDocumentDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.List;

@Path("/document-properties")
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
