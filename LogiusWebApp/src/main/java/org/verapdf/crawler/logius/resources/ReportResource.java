package org.verapdf.crawler.logius.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.verapdf.crawler.logius.core.reports.ReportsGenerator;
import org.verapdf.crawler.logius.db.DocumentDAO;
import org.verapdf.crawler.logius.document.DomainDocument;
import org.verapdf.crawler.logius.report.CrawlJobSummary;
import org.verapdf.crawler.logius.report.ErrorStatistics;
import org.verapdf.crawler.logius.report.PDFWamErrorStatistics;
import org.verapdf.crawler.logius.report.PdfPropertyStatistics;
import org.verapdf.crawler.logius.tools.DomainUtils;

import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("api/report")
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
                                      @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date documentsSince) {
        // PDF
        Long pdfCount = documentDAO.count(domain, DomainDocument.DocumentTypeGroup.PDF.getTypes(), null, documentsSince);
        // Office Open XML
        Long microsoftOfficeCount = documentDAO.count(domain, DomainDocument.DocumentTypeGroup.MS_OFFICE.getTypes(),null, documentsSince);
        // OO_XML_OFFICE
        Long officeOpenXmlCount = documentDAO.count(domain, DomainDocument.DocumentTypeGroup.OO_XML_OFFICE.getTypes(), null, documentsSince);
        // Open Document format (ODF)
        Long odfCount = documentDAO.count(domain, DomainDocument.DocumentTypeGroup.OPEN_OFFICE.getTypes(), null, documentsSince);

        CrawlJobSummary summary = new CrawlJobSummary();
        summary.getTypeOfDocuments().put(DomainDocument.DocumentTypeGroup.PDF, pdfCount);
        summary.getTypeOfDocuments().put(DomainDocument.DocumentTypeGroup.OO_XML_OFFICE, officeOpenXmlCount);
        summary.getTypeOfDocuments().put(DomainDocument.DocumentTypeGroup.OPEN_OFFICE, odfCount);
        summary.getTypeOfDocuments().put(DomainDocument.DocumentTypeGroup.MS_OFFICE, microsoftOfficeCount);
        return summary;
    }

    @GetMapping(value = "/document-statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public PdfPropertyStatistics getDocumentStatistics(@RequestParam("domain") String domain,
                                                       @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date documentsSince) {

        Long openPdf = documentDAO.count(domain, DomainDocument.DocumentTypeGroup.PDF.getTypes(), DomainDocument.BaseTestResult.OPEN, documentsSince);
        Long notOpenPdf = documentDAO.count(domain, DomainDocument.DocumentTypeGroup.PDF.getTypes(), DomainDocument.BaseTestResult.NOT_OPEN, documentsSince);
        Long total = openPdf + notOpenPdf;

        List<PdfPropertyStatistics.ValueCount> flavourStatistics = documentDAO.getPropertyStatistics(
                domain, PdfPropertyStatistics.TYPE_PROPERTY_NAME, documentsSince);
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
                                              @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date documentsSince,
                                              @RequestParam(value = "flavour", required = false) String flavour,
                                              @RequestParam(value = "version", required = false) String version,
                                              @RequestParam(value = "producer", required = false) String producer) {

        List<ErrorStatistics.ErrorCount> errorCounts = documentDAO.getErrorsStatistics(
                domain, documentsSince, flavour, version, producer, ErrorStatistics.TOP_ERRORS_COUNT);

        ErrorStatistics errorStatistics = new ErrorStatistics();
        errorStatistics.setTopErrorStatistics(errorCounts);
        return errorStatistics;
    }


    @GetMapping("/pdfwam-statistics")
    @Transactional
    public List<PDFWamErrorStatistics.ErrorCount> getDocumentPropertyStatistics(@RequestParam("domain") @NotNull String domain,
                                                                                @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
                                                                                @RequestParam(value = "flavour", required = false) String flavour,
                                                                                @RequestParam(value = "version", required = false) String version,
                                                                                @RequestParam(value = "producer", required = false) String producer) {
        return documentDAO.getPDFWamErrorsStatistics(domain, startDate, flavour, version, producer);
    }


    @GetMapping(value = "/full.ods")
    @Transactional
    public ResponseEntity getFullReportAsOds(@RequestParam("domain") String domain,
                                             @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate) {
        domain = DomainUtils.trimUrl(domain);
        long compliantPDFA12Count = getDocumentsCount(domain, DomainDocument.DocumentTypeGroup.PDF,
                DomainDocument.BaseTestResult.OPEN, startDate);
        long odfCount = getDocumentsCount(domain, DomainDocument.DocumentTypeGroup.OPEN_OFFICE,
                null, startDate);
        long invalidPDFA12Count = getDocumentsCount(domain, DomainDocument.DocumentTypeGroup.PDF,
                DomainDocument.BaseTestResult.NOT_OPEN, startDate);
        long msCount = getDocumentsCount(domain, DomainDocument.DocumentTypeGroup.MS_OFFICE,
                null, startDate);
        long ooXMLCount = getDocumentsCount(domain, DomainDocument.DocumentTypeGroup.OO_XML_OFFICE,
                null, startDate);
        List<DomainDocument> invalidPDFDocuments = documentDAO.getDocuments(domain,
                DomainDocument.DocumentTypeGroup.PDF.getTypes(),
                DomainDocument.BaseTestResult.NOT_OPEN, startDate, ODS_MAX_DOCUMENTS_SHOW);
        List<String> microsoftDocuments = documentDAO.getDocumentsUrls(domain,
                DomainDocument.DocumentTypeGroup.MS_OFFICE.getTypes(),
                null, startDate, ODS_MAX_DOCUMENTS_SHOW);
        List<String> openOfficeXMLDocuments = documentDAO.getDocumentsUrls(domain,
                DomainDocument.DocumentTypeGroup.OO_XML_OFFICE.getTypes(), null, startDate, ODS_MAX_DOCUMENTS_SHOW);
        try {
            File tempODS = reportsGenerator.generateODSReport(domain, startDate,
                    compliantPDFA12Count, odfCount,
                    invalidPDFA12Count, msCount,
                    ooXMLCount, invalidPDFDocuments,
                    microsoftDocuments, openOfficeXMLDocuments);
            logger.info("ODS report requested");
            Path path = Paths.get(tempODS.getAbsolutePath());
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tempODS.getName() + "\"")
                    .body(resource);

        } catch (IOException e) {
            logger.error("Exception during ods report creation: " + e.getMessage(), e);
        }
        return ResponseEntity.badRequest().build();
    }

    private long getDocumentsCount(String domain, DomainDocument.DocumentTypeGroup documentGroup,
                                   DomainDocument.BaseTestResult testResult,
                                   Date start) {
        Long count = documentDAO.count(domain, documentGroup.getTypes(),
                testResult, start);
        return count == null ? 0 : count;
    }
}
