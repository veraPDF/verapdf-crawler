package com.verapdf.crawler.logius.app.resources;

import com.verapdf.crawler.logius.app.core.reports.ReportsGenerator;
import com.verapdf.crawler.logius.app.tools.DateParam;
import com.verapdf.crawler.logius.app.tools.DomainUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.verapdf.crawler.logius.app.document.DomainDocument;
import com.verapdf.crawler.logius.app.report.CrawlJobSummary;
import com.verapdf.crawler.logius.app.report.ErrorStatistics;
import com.verapdf.crawler.logius.app.report.PDFWamErrorStatistics;
import com.verapdf.crawler.logius.app.report.PdfPropertyStatistics;
import com.verapdf.crawler.logius.app.db.DocumentDAO;
import org.xml.sax.SAXException;

import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/report")
public class ReportResource {
    // todo: clarify if we need multi-domain statistics (even if not, we use domain as a query param rather than path param to easy migrate in the future)

    private static final Logger logger = LoggerFactory.getLogger(ReportResource.class);
    private static final int ODS_MAX_DOCUMENTS_SHOW = 100;
    private final DocumentDAO documentDAO;
    private final ReportsGenerator reportsGenerator;

    @Autowired
    public ReportResource(DocumentDAO documentDAO, ReportsGenerator reportsGenerator) {
        this.documentDAO = documentDAO;
        this.reportsGenerator = reportsGenerator;
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public CrawlJobSummary getSummary(@RequestParam("domain") String domain,
                                      @RequestParam("startDate") DateParam startDate){
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

    @GetMapping(value = "/document-statistics", produces = MediaType.APPLICATION_JSON_VALUE)
//    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public PdfPropertyStatistics getDocumentStatistics(@RequestParam("domain") String domain,
                                                       @RequestParam("startDate") DateParam startDate) {
        Date documentsSince = DateParam.getDateFromParam(startDate);


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

    @GetMapping(value = "/error-statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ErrorStatistics getErrorStatistics(@RequestParam("domain") String domain,
                                              @RequestParam("startDate") DateParam startDate,
                                              @RequestParam("flavour") String flavour,
                                              @RequestParam("version") String version,
                                              @RequestParam("producer") String producer) {
        Date documentsSince = DateParam.getDateFromParam(startDate);

        List<ErrorStatistics.ErrorCount> errorCounts = documentDAO.getErrorsStatistics(
                domain, documentsSince, flavour, version, producer, ErrorStatistics.TOP_ERRORS_COUNT);

        ErrorStatistics errorStatistics = new ErrorStatistics();
        errorStatistics.setTopErrorStatistics(errorCounts);
        return errorStatistics;
    }


    @GetMapping("/pdfwam-statistics")
    public List<PDFWamErrorStatistics.ErrorCount> getDocumentPropertyStatistics(@RequestParam("domain") @NotNull String domain,
                                                                                @RequestParam("startDate") DateParam startDate,
                                                                                @RequestParam("flavour") String flavour,
                                                                                @RequestParam("version") String version,
                                                                                @RequestParam("producer") String producer) {
        Date documentsSince = DateParam.getDateFromParam(startDate);
        return documentDAO.getPDFWamErrorsStatistics(domain, documentsSince, flavour, version, producer);
    }


    @GetMapping(value = "/full.ods", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Transactional
    public ResponseEntity getFullReportAsOds(@RequestParam("domain") String domain,
                                             @RequestParam("startDate") DateParam startDate) {
        if (domain != null) {
            domain = DomainUtils.trimUrl(domain);
        }
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
            File tempODS = reportsGenerator.generateODSReport(domain, start,
                    compliantPDFA12Count, odfCount,
                    invalidPDFA12Count, msCount,
                    ooXMLCount, invalidPDFDocuments,
                    microsoftDocuments, openOfficeXMLDocuments);
            logger.info("ODS report requested");

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"" + tempODS.getName() + "\"");

            return new ResponseEntity<>(tempODS, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Exception during ods report creation: " + e.getMessage(), e);
        }
        return null;
    }

    private long getDocumentsCount(String domain, DomainDocument.DocumentTypeGroup documentGroup,
                                   DomainDocument.BaseTestResult testResult,
                                   Date start) {
        Long count = documentDAO.count(domain, documentGroup.getTypes(),
                testResult, start);
        return count == null ? 0 : count;
    }
}
