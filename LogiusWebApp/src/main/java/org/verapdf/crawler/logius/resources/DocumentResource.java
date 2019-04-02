package org.verapdf.crawler.logius.resources;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.db.DocumentDAO;
import org.verapdf.crawler.logius.db.ValidationJobDAO;
import org.verapdf.crawler.logius.document.DomainDocument;
import org.verapdf.crawler.logius.validation.ValidationJob;

import java.time.LocalDate;
import java.time.LocalDateTime;


@RestController
@RequestMapping(value = "api/documents", produces = MediaType.APPLICATION_JSON_VALUE)
public class DocumentResource {

    private static final Logger logger = LoggerFactory.getLogger(DocumentResource.class);
    private final CrawlJobDAO crawlJobDAO;
    private final DocumentDAO documentDAO;
    private final ValidationJobDAO validationJobDAO;

    public DocumentResource(CrawlJobDAO crawlJobDAO, DocumentDAO documentDAO, ValidationJobDAO validationJobDAO) {
        this.crawlJobDAO = crawlJobDAO;
        this.documentDAO = documentDAO;
        this.validationJobDAO = validationJobDAO;
    }

    private static void validatePdfFile(DomainDocument document, ValidationJobDAO validationJobDAO) {
        ValidationJob validationJob = new ValidationJob(document);
        validationJobDAO.save(validationJob);
    }

    private static void validateOpenOfficeFile(DomainDocument document) {
        document.setBaseTestResult(DomainDocument.BaseTestResult.OPEN);
    }

    private static void validateMSOfficeFile(DomainDocument document) {
        document.setBaseTestResult(DomainDocument.BaseTestResult.NOT_OPEN);
    }

    @PostMapping
    @Transactional
    public DomainDocument saveDocument(@RequestBody DomainDocument document) {
        CrawlJob job = crawlJobDAO.getByHeritrixJobId(document.getDocumentId().getCrawlJob().getHeritrixJobId());
        if (job == null) {
            return null;
        }
        return saveDocument(document, job);
    }

    @Transactional
    public DomainDocument saveDocument(DomainDocument document, CrawlJob job) {
        document.getDocumentId().setCrawlJob(job);
        documentDAO.save(document);

        switch (document.getContentType().toLowerCase()) {
            case "pdf":
                validatePdfFile(document, validationJobDAO);
                break;
            case "odt":
            case "ods":
            case "odp":
                validateOpenOfficeFile(document);
                break;
            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
                validateMSOfficeFile(document);
                break;
            default:
                logger.warn("Unknown document type " + document.getContentType() + ". Document " + document.getDocumentId().getDocumentUrl() + " won't be tested.");
        }

        return document;
    }
}
