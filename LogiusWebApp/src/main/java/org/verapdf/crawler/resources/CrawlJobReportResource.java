package org.verapdf.crawler.resources;

import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.api.report.CrawlJobSummary;
import org.verapdf.crawler.api.report.ErrorStatistics;
import org.verapdf.crawler.api.report.PdfPropertyStatistics;
import org.verapdf.crawler.core.heritrix.HeritrixReporter;
import org.verapdf.crawler.db.document.ValidatedPDFDao;
import org.verapdf.crawler.db.jobs.CrawlJobDao;
import org.verapdf.crawler.tools.DateParam;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Date;

@Path("/report")
public class CrawlJobReportResource {
    // todo: clarify if we need multi-domain statistics (even if not, we use domain as a query param rather than path param to easy migrate in the future)

    private final CrawlJobDao crawlJobDao;
    private final HeritrixReporter reporter;
    private final ValidatedPDFDao validatedPDFDao;

    public CrawlJobReportResource(CrawlJobDao crawlJobDao, HeritrixReporter reporter, ValidatedPDFDao validatedPDFDao) {
        this.crawlJobDao = crawlJobDao;
        this.reporter = reporter;
        this.validatedPDFDao = validatedPDFDao;
    }

    @GET
    @Path("/summary")
    @Produces(MediaType.APPLICATION_JSON)
    public CrawlJobSummary getSummary(@QueryParam("domain") String domain,
                                      @QueryParam("startDate") DateParam startDate) throws IOException, ParserConfigurationException, SAXException {
        CrawlJob crawlJob = crawlJobDao.getCrawlJobByCrawlUrl(domain);
        if (crawlJob == null) {
            return null;
        }
        Date parsedDate = DateParam.getDateFromParam(startDate);
        Date time = parsedDate == null ? crawlJob.getStartTime() : parsedDate;
        //TODO: use db rather than HeritrixReporter
        if(crawlJob.getJobURL() != null && !crawlJob.getJobURL().isEmpty()) {
            return reporter.getReport(crawlJob.getHeritrixJobId(), crawlJob.getJobURL(), time);
        }
        return reporter.getReport(crawlJob.getHeritrixJobId(), time);
    }

    @GET
    @Path("/document-statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public PdfPropertyStatistics getDocumentStatistics(@QueryParam("domain") String domain,
                                                       @QueryParam("startDate") DateParam startDate) {
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
    @Produces(MediaType.APPLICATION_JSON)
    public ErrorStatistics getErrorStatistics(@QueryParam("domain") String domain,
                                              @QueryParam("startDate") DateParam startDate,
                                              @QueryParam("flavor") String flavor,
                                              @QueryParam("version") String version,
                                              @QueryParam("producer") String producer) {
        return validatedPDFDao.getErrorStatistics(domain, DateParam.getDateFromParam(startDate), flavor, version, producer);
    }

    @GET
    @Path("/full.ods")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getFullReportAsOds(@QueryParam("domain") String domain,
                                       @QueryParam("startDate") DateParam startDate) {
        // todo: determine structure of ODS report
        return null;
    }
}
