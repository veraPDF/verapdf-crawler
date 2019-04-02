package org.verapdf.crawler.logius.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.verapdf.crawler.logius.core.reports.ReportsGenerator;
import org.verapdf.crawler.logius.db.DocumentDAO;
import org.verapdf.crawler.logius.document.DomainDocument;
import org.verapdf.crawler.logius.dto.user.TokenUserDetails;
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
import java.util.UUID;

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
    public CrawlJobSummary getSummary(@AuthenticationPrincipal TokenUserDetails principal, @RequestParam("domain") String domain,
                                      @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date documentsSince) {
        UUID userId = principal == null ? null : principal.getUuid();
        // PDF
        Long pdfCount = documentDAO.count(domain, userId, DomainDocument.DocumentTypeGroup.PDF.getTypes(), null, documentsSince);
        // Office Open XML
        Long microsoftOfficeCount = documentDAO.count(domain, userId, DomainDocument.DocumentTypeGroup.MS_OFFICE.getTypes(), null, documentsSince);
        // OO_XML_OFFICE
        Long officeOpenXmlCount = documentDAO.count(domain, userId, DomainDocument.DocumentTypeGroup.OO_XML_OFFICE.getTypes(), null, documentsSince);
        // Open Document format (ODF)
        Long odfCount = documentDAO.count(domain, userId, DomainDocument.DocumentTypeGroup.OPEN_OFFICE.getTypes(), null, documentsSince);

        CrawlJobSummary summary = new CrawlJobSummary();
        summary.addTypeOfDocumentCount(DomainDocument.DocumentTypeGroup.PDF, pdfCount);
        summary.addTypeOfDocumentCount(DomainDocument.DocumentTypeGroup.OO_XML_OFFICE, officeOpenXmlCount);
        summary.addTypeOfDocumentCount(DomainDocument.DocumentTypeGroup.OPEN_OFFICE, odfCount);
        summary.addTypeOfDocumentCount(DomainDocument.DocumentTypeGroup.MS_OFFICE, microsoftOfficeCount);
        return summary;
    }

    @GetMapping(value = "/document-statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public PdfPropertyStatistics getDocumentStatistics(@AuthenticationPrincipal TokenUserDetails principal, @RequestParam("domain") String domain,
                                                       @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date documentsSince) {
        UUID userId = principal == null ? null : principal.getUuid();
        Long openPdf = documentDAO.count(domain, userId, DomainDocument.DocumentTypeGroup.PDF.getTypes(), DomainDocument.BaseTestResult.OPEN, documentsSince);
        Long notOpenPdf = documentDAO.count(domain, userId, DomainDocument.DocumentTypeGroup.PDF.getTypes(), DomainDocument.BaseTestResult.NOT_OPEN, documentsSince);
        Long total = openPdf + notOpenPdf;

        List<PdfPropertyStatistics.ValueCount> flavourStatistics = documentDAO.getPropertyStatistic(
                domain, userId, documentsSince);
        List<PdfPropertyStatistics.ValueCount> versionStatistics = documentDAO.getPropertyStatistics(
                domain, userId, PdfPropertyStatistics.VERSION_PROPERTY_NAME, documentsSince);
        List<PdfPropertyStatistics.ValueCount> producerStatistics = documentDAO.getPropertyStatistics(
                domain, userId, PdfPropertyStatistics.PRODUCER_PROPERTY_NAME, documentsSince, true, PdfPropertyStatistics.TOP_PRODUCERS_COUNT);

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
    public ErrorStatistics getErrorStatistics(@AuthenticationPrincipal TokenUserDetails principal,
                                              @RequestParam("domain") String domain,
                                              @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date documentsSince,
                                              @RequestParam(value = "flavour", required = false) String flavour,
                                              @RequestParam(value = "version", required = false) String version,
                                              @RequestParam(value = "producer", required = false) String producer) {

        UUID userId = principal == null ? null : principal.getUuid();

        List<ErrorStatistics.ErrorCount> errorCounts = documentDAO.getErrorsStatistics(
                domain, userId, documentsSince, flavour, version, producer, ErrorStatistics.TOP_ERRORS_COUNT);

        ErrorStatistics errorStatistics = new ErrorStatistics();
        errorStatistics.setTopErrorStatistics(errorCounts);
        return errorStatistics;
    }


    @GetMapping("/pdfwam-statistics")
    @Transactional
    public List<PDFWamErrorStatistics.ErrorCount> getDocumentPropertyStatistics(@AuthenticationPrincipal TokenUserDetails principal,
                                                                                @RequestParam("domain") @NotNull String domain,
                                                                                @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
                                                                                @RequestParam(value = "flavour", required = false) String flavour,
                                                                                @RequestParam(value = "version", required = false) String version,
                                                                                @RequestParam(value = "producer", required = false) String producer) {
        UUID userId = principal == null ? null : principal.getUuid();
        return documentDAO.getPDFWamErrorsStatistics(domain, userId, startDate, flavour, version, producer);
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
        Long count = documentDAO.count(domain, null, documentGroup.getTypes(),
                testResult, start);
        return count == null ? 0 : count;
    }
}
