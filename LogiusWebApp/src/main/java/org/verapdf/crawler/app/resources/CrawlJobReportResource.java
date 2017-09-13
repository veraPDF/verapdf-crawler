package org.verapdf.crawler.app.resources;

import org.verapdf.crawler.domain.crawling.CrawlJob;
import org.verapdf.crawler.domain.report.CrawlJobSummary;
import org.verapdf.crawler.domain.report.ErrorStatistics;
import org.verapdf.crawler.domain.report.PDFValidationStatistics;
import org.verapdf.crawler.domain.report.PdfPropertyStatistics;
import org.verapdf.crawler.report.HeritrixReporter;
import org.verapdf.crawler.repository.document.ValidatedPDFDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Path("/report")
public class CrawlJobReportResource {
    // todo: clarify if we need multi-domain statistics (even if not, we use domain as a query param rather than path param to easy migrate in the future)

    private final CrawlJobDao crawlJobDao;
    private final HeritrixReporter reporter;
    private final ValidatedPDFDao validatedPDFDao;

    CrawlJobReportResource(CrawlJobDao crawlJobDao, HeritrixReporter reporter, ValidatedPDFDao validatedPDFDao) {
        this.crawlJobDao = crawlJobDao;
        this.reporter = reporter;
        this.validatedPDFDao = validatedPDFDao;
    }

    @GET
    @Path("/summary")
    public CrawlJobSummary getSummary(@QueryParam("domain") String domain,
                                      @QueryParam("startDate") Date startDate) throws IOException, ParserConfigurationException, SAXException {
        CrawlJob crawlJob = crawlJobDao.getCrawlJobByCrawlUrl(domain);
        Date time;
        if(startDate == null) {
            time = crawlJob.getStartTime();
        }
        else {
            time = startDate;
        }
        if(crawlJob.getJobURL() != null && !crawlJob.getJobURL().isEmpty()) {
            return reporter.getReport(crawlJob.getId(), crawlJob.getJobURL(), time);
        }
        return reporter.getReport(crawlJob.getId(), time);
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
    public ErrorStatistics getErrorStatistics(@QueryParam("domain") String domain,
                                              @QueryParam("startDate") String startDate,
                                              @QueryParam("flavor") String flavor,
                                              @QueryParam("version") String version,
                                              @QueryParam("producer") String producer) {
        return validatedPDFDao.getErrorStatistics(domain, startDate, flavor, version, producer);
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/full.ods")
    public Response getFullReportAsOds(@QueryParam("domain") String domain,
                                       @QueryParam("startDate") String startDate) {
        // todo: determine structure of ODS report
        return null;
    }
}
