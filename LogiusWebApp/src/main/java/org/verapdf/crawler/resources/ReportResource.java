package org.verapdf.crawler.resources;

import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.ResourceManager;
import org.verapdf.crawler.api.document.DomainDocument;
import org.verapdf.crawler.api.report.CrawlJobSummary;
import org.verapdf.crawler.api.report.ErrorStatistics;
import org.verapdf.crawler.api.report.PdfPropertyStatistics;
import org.verapdf.crawler.core.reports.ReportsGenerator;
import org.verapdf.crawler.db.DocumentDAO;
import org.verapdf.crawler.tools.DateParam;
import org.verapdf.crawler.tools.DomainUtils;
import org.xml.sax.SAXException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Path("/report")
public class ReportResource {
    // todo: clarify if we need multi-domain statistics (even if not, we use domain as a query param rather than path param to easy migrate in the future)

    private static final Logger logger = LoggerFactory.getLogger(ReportResource.class);

    private static final int ODS_MAX_DOCUMENTS_SHOW = 100;

    private final ResourceManager resourceManager;

    public ReportResource(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @GET
    @Path("/summary")
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork
    public CrawlJobSummary getSummary(@QueryParam("domain") String domain,
                                      @QueryParam("startDate") DateParam startDate) throws IOException, ParserConfigurationException, SAXException {
        Date documentsSince = DateParam.getDateFromParam(startDate);

        DocumentDAO documentDAO = resourceManager.getDocumentDAO();
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

        DocumentDAO documentDAO = resourceManager.getDocumentDAO();
        Long openPdf = documentDAO.count(domain, DomainDocument.DocumentTypeGroup.PDF.getTypes(), DomainDocument.BaseTestResult.OPEN, documentsSince);
        Long notOpenPdf = documentDAO.count(domain, DomainDocument.DocumentTypeGroup.PDF.getTypes(), DomainDocument.BaseTestResult.NOT_OPEN, documentsSince);
        Long total = openPdf + notOpenPdf;

        List<PdfPropertyStatistics.ValueCount> flavourStatistics = documentDAO.getPropertyStatistics(
                domain, PdfPropertyStatistics.FLAVOUR_PROPERTY_NAME, documentsSince);
        List<PdfPropertyStatistics.ValueCount> versionStatistics = documentDAO.getPropertyStatistics(
                domain, PdfPropertyStatistics.VERSION_PROPERTY_NAME, documentsSince);
        List<PdfPropertyStatistics.ValueCount> producerStatistics = documentDAO.getPropertyStatistics(
                domain, PdfPropertyStatistics.PRODUCER_PROPERTY_NAME, documentsSince, true, PdfPropertyStatistics.TOP_PRODUCERS_COUNT);

        PdfPropertyStatistics statistics = new PdfPropertyStatistics();
        statistics.setOpenPdfDocumentsCount(openPdf);
        statistics.setNotOpenPdfDocumentsCount(notOpenPdf);
        statistics.setTotalPdfDocumentsCount(total);
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
        Date documentsSince = DateParam.getDateFromParam(startDate);

        List<ErrorStatistics.ErrorCount> errorCounts = resourceManager.getDocumentDAO().getErrorsStatistics(
                domain, documentsSince, flavour, version, producer, ErrorStatistics.TOP_ERRORS_COUNT);

        ErrorStatistics errorStatistics = new ErrorStatistics();
        errorStatistics.setTopErrorStatistics(errorCounts);
        return errorStatistics;
    }

    @GET
    @Path("/full.ods")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @UnitOfWork
    public Response getFullReportAsOds(@QueryParam("domain") String domain,
                                       @QueryParam("startDate") DateParam startDate) {
        if (domain != null) {
            domain = DomainUtils.trimUrl(domain);
        }
        DocumentDAO documentDAO = resourceManager.getDocumentDAO();
        Date start = DateParam.getDateFromParam(startDate);
        long compliantPDFA12Count = getDocumentsCount(domain, DomainDocument.DocumentTypeGroup.PDF,
                DomainDocument.BaseTestResult.OPEN, start);
        long odfCount = getDocumentsCount(domain, DomainDocument.DocumentTypeGroup.OPEN_OFFICE,
                null, start);
        long invalidPDFA12Count = getDocumentsCount(domain, DomainDocument.DocumentTypeGroup.PDF,
                DomainDocument.BaseTestResult.NOT_OPEN, start);
        long msCount = getDocumentsCount(domain, DomainDocument.DocumentTypeGroup.MS_OFFICE,
                null, start);
        long ooXMLCount = getDocumentsCount(domain, DomainDocument.DocumentTypeGroup.OO_XML_OFFICE,
                null, start);
        List<DomainDocument> invalidPDFDocuments = documentDAO.getDocuments(domain,
                DomainDocument.DocumentTypeGroup.PDF.getTypes(),
                DomainDocument.BaseTestResult.NOT_OPEN, start, ODS_MAX_DOCUMENTS_SHOW);
        List<String> microsoftDocuments = documentDAO.getDocumentsUrls(domain,
                DomainDocument.DocumentTypeGroup.MS_OFFICE.getTypes(),
                null, start, ODS_MAX_DOCUMENTS_SHOW);
        List<String> openOfficeXMLDocuments = documentDAO.getDocumentsUrls(domain,
                DomainDocument.DocumentTypeGroup.OO_XML_OFFICE.getTypes(), null, start, ODS_MAX_DOCUMENTS_SHOW);
        try {
            File tempODS = ReportsGenerator.generateODSReport(domain, start,
                    compliantPDFA12Count, odfCount,
                    invalidPDFA12Count, msCount,
                    ooXMLCount, invalidPDFDocuments,
                    microsoftDocuments, openOfficeXMLDocuments);
            logger.info("ODS report requested");
            return Response.ok(tempODS, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + tempODS.getName() + "\"")
                    .build();
        } catch (IOException e) {
            logger.error("Exception during ods report creation: " + e.getMessage(), e);
        }
        return null;
    }

    private long getDocumentsCount(String domain, DomainDocument.DocumentTypeGroup documentGroup,
                                   DomainDocument.BaseTestResult testResult,
                                   Date start) {
        Long count = resourceManager.getDocumentDAO().count(domain, documentGroup.getTypes(),
                testResult, start);
        return count == null ? 0 : count;
    }
}
