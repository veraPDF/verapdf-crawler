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
    private final CrawlJobDao crawlJobDao;

    public DocumentPropertyResource(ReportDocumentDao reportDocumentDao, CrawlJobDao crawlJobDao) {
        this.reportDocumentDao = reportDocumentDao;
        this.crawlJobDao = crawlJobDao;
    }

    @GET
    @Path("/{propertyName}/values")
    public List<String> getDocumentPropertyValues(@PathParam("propertyName") String propertyName,
                                                  @QueryParam("domain") String domain,
                                                  @QueryParam("propertyValueFilter") String propertyValueFilter) {
        // todo: return the list of all values for property in the selected domain (used for dropdowns)
        return reportDocumentDao.getMatchingPropertyValues(crawlJobDao.getIdByUrl(domain), propertyName, propertyValueFilter);
    }
}
