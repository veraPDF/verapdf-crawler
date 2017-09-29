package org.verapdf.crawler.resources;

import io.dropwizard.hibernate.UnitOfWork;
import org.verapdf.crawler.api.document.DomainDocument;
import org.verapdf.crawler.api.report.CrawlJobSummary;
import org.verapdf.crawler.api.report.ErrorStatistics;
import org.verapdf.crawler.api.report.PdfPropertyStatistics;
import org.verapdf.crawler.db.DocumentDAO;
import org.verapdf.crawler.tools.DateParam;
import org.xml.sax.SAXException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Path("/report")
public class ReportResource {
    // todo: clarify if we need multi-domain statistics (even if not, we use domain as a query param rather than path param to easy migrate in the future)

    private final DocumentDAO documentDAO;

    public ReportResource(DocumentDAO documentDAO) {
        this.documentDAO = documentDAO;
    }

    @GET
    @Path("/summary")
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork
    public CrawlJobSummary getSummary(@QueryParam("domain") String domain,
                                      @QueryParam("startDate") DateParam startDate) throws IOException, ParserConfigurationException, SAXException {
        Date documentsSince = DateParam.getDateFromParam(startDate);

        Long openPdf = documentDAO.count(domain, DomainDocument.DocumentTypeGroup.PDF.getTypes(), DomainDocument.BaseTestResult.OPEN, documentsSince);
        Long notOpenPdf = documentDAO.count(domain, DomainDocument.DocumentTypeGroup.PDF.getTypes(), DomainDocument.BaseTestResult.NOT_OPEN, documentsSince);
        Long openOffice = documentDAO.count(domain, DomainDocument.DocumentTypeGroup.OFFICE.getTypes(), DomainDocument.BaseTestResult.OPEN, documentsSince);
        Long notOpenOffice = documentDAO.count(domain, DomainDocument.DocumentTypeGroup.OFFICE.getTypes(), DomainDocument.BaseTestResult.NOT_OPEN, documentsSince);

        CrawlJobSummary summary = new CrawlJobSummary();
        summary.getOpenDocuments().put(DomainDocument.DocumentTypeGroup.PDF, openPdf);
        summary.getOpenDocuments().put(DomainDocument.DocumentTypeGroup.OFFICE, openOffice);
        summary.getNotOpenDocuments().put(DomainDocument.DocumentTypeGroup.PDF, notOpenPdf);
        summary.getNotOpenDocuments().put(DomainDocument.DocumentTypeGroup.OFFICE, notOpenOffice);
        return summary;
    }

    @GET
    @Path("/document-statistics")
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork
    public PdfPropertyStatistics getDocumentStatistics(@QueryParam("domain") String domain,
                                                       @QueryParam("startDate") DateParam startDate) {
        Date documentsSince = DateParam.getDateFromParam(startDate);
        /* todo: change to the following structure:
            {
                totalCount: 5612,
                flavourStatistics: [{
                    flavour: 'PDF/A-1a',
                    count: 123
                }, {
                    flavour: 'PDF/A-2a',
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

            stats should include all flavours, all versions (order by flavour/version) and top 10 producers (order by doc count desc).
        */
        List<PdfPropertyStatistics.ValueCount> flavourStatistics = documentDAO.getPropertyStatistics(
                domain, PdfPropertyStatistics.FLAVOUR_PROPERTY_NAME, documentsSince, false, null);
        List<PdfPropertyStatistics.ValueCount> versionStatistics = documentDAO.getPropertyStatistics(
                domain, PdfPropertyStatistics.VERSION_PROPERTY_NAME, documentsSince, false, null);
        List<PdfPropertyStatistics.ValueCount> producerStatistics = documentDAO.getPropertyStatistics(
                domain, PdfPropertyStatistics.PRODUCER_PROPERTY_NAME, documentsSince, true, PdfPropertyStatistics.TOP_PRODUCERS_COUNT);

        PdfPropertyStatistics statistics = new PdfPropertyStatistics();
        statistics.setFlavourStatistics(flavourStatistics);
        statistics.setVersionStatistics(versionStatistics);
        statistics.setTopProducerStatistics(producerStatistics);

        return statistics;
    }

    @GET
    @Path("/error-statistics")
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork
    public ErrorStatistics getErrorStatistics(@QueryParam("domain") String domain,
                                              @QueryParam("startDate") DateParam startDate,
                                              @QueryParam("flavour") String flavour,
                                              @QueryParam("version") String version,
                                              @QueryParam("producer") String producer) {
        return null;
    }

    @GET
    @Path("/full.ods")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @UnitOfWork
    public Response getFullReportAsOds(@QueryParam("domain") String domain,
                                       @QueryParam("startDate") DateParam startDate) {
        // todo: determine structure of ODS report
        return null;
    }
}
