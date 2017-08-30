package org.verapdf.crawler.app.resources;

import org.verapdf.crawler.domain.report.CrawlJobReport;
import org.verapdf.crawler.domain.report.PDFValidationStatistics;
import org.verapdf.crawler.domain.report.PdfPropertyStatistics;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/report")
public class CrawlJobReportResource {
    // todo: clarify if we need multi-domain statistics (even if not, we use domain as a query param rather than path param to easy migrate in the future)

    @GET
    @Path("/summary")
    public CrawlJobReport getSummary(@QueryParam("domain") String domain) {
        // todo: rename CrawlJobReport to CrawlJobSummary
        return null;
    }

    @GET
    @Path("/document-statistics")
    public PdfPropertyStatistics getDocumentStatistics(@QueryParam("domain") String domain,
                                                       @QueryParam("startDate") String startDate) {
        // todo: rename to DocumentStatistics
        /* todo: change to the following structure:
            {
                totalCount: 5612,
                flavorStatistics: [{
                    flavor: 'PDF/A-1a',
                    count: 123
                }, {
                    flavor: 'PDF/A-2a',
                    count: 456
                }, ...
                ],
                versionStatistics: [{
                    version: '1.0',
                    count: 456
                }, {
                    version: '1.1',
                    count: 123
                }],
                topProducerStatistics: [{
                    producer: 'Producer 9',
                    count: 456
                }, {
                    producer: 'Producer 3',
                    count: 345
                }, ...]
            }

            stats should include all flavors, all versions (order by flavor/version) and top 10 producers (order by doc count desc).
        */
        return null;
    }

    @GET
    @Path("/error-statistics")
    public PDFValidationStatistics getErrorStatistics(@QueryParam("domain") String domain,
                                                      @QueryParam("startDate") String startDate,
                                                      @QueryParam("flavor") String flavor,
                                                      @QueryParam("version") String version,
                                                      @QueryParam("producer") String producer) {
        // todo: rename to ErrorStatistics
        /* todo: change to the following structure
            {
                totalCount: 2311,
                topErrorStatistics: [{
                    description: 'Error 1 description',
                    count: 345
                }, {
                    description: 'Error 2 description',
                    count: 234
                }, ...]
            }
            same as producers, return top 10 most frequent errors, order by count desc
         */
        return null;
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/full.ods")
    public Response getFullReportAsOds(@QueryParam("domain") String domain,
                                       @QueryParam("startDate") String startDate) {
        return null;
    }
}
