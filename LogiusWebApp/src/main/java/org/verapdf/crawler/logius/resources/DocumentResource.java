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
import org.verapdf.crawler.logius.service.ValidationJobService;
import org.verapdf.crawler.logius.service.ValidationManager;
import org.verapdf.crawler.logius.validation.ValidationJob;



@RestController
@RequestMapping(value = "api/documents", produces = MediaType.APPLICATION_JSON_VALUE)
public class DocumentResource {
    private static final Logger logger = LoggerFactory.getLogger(DocumentResource.class);
    private final CrawlJobDAO crawlJobDAO;
    private final DocumentDAO documentDAO;
    private final ValidationJobDAO validationJobDAO;
    private final ValidationManager validationManager;
    private final ValidationJobService validationJobService;

    public DocumentResource(CrawlJobDAO crawlJobDAO, DocumentDAO documentDAO, ValidationJobDAO validationJobDAO,
                            ValidationManager validationManager, ValidationJobService validationJobService) {
        this.crawlJobDAO = crawlJobDAO;
        this.documentDAO = documentDAO;
        this.validationJobDAO = validationJobDAO;
        this.validationManager = validationManager;
        this.validationJobService = validationJobService;
    }

    private void validatePdfFile(DomainDocument document) {
        ValidationJob validationJob = new ValidationJob(document);
        if (document.getCrawlJob().getStatus() == CrawlJob.Status.PAUSED){
            validationJob.setStatus(ValidationJob.Status.PAUSED);
        } else {
            validationJob.setStatus(ValidationJob.Status.NOT_STARTED);
        }
        validationJobService.save(validationJob);
        validationManager.updateState();
    }

    private static void validateOpenOfficeFile(DomainDocument document) {
        document.setBaseTestResult(DomainDocument.BaseTestResult.OPEN);
    }

    private static void validateMSOfficeFile(DomainDocument document) {
        document.setBaseTestResult(DomainDocument.BaseTestResult.NOT_OPEN);
    }

    @PostMapping
    @Transactional
    public DomainDocument saveDoc(@RequestBody DomainDocument document) {
        return saveDocument(document);
    }

    @Transactional
    public DomainDocument saveDocument(DomainDocument document) {
        CrawlJob job = crawlJobDAO.getByHeritrixJobId(document.getCrawlJob().getHeritrixJobId());
        if (job == null) {
            return null;
        }
        document.setCrawlJob(job);
        documentDAO.save(document);

        switch (document.getContentType()) {
            case "pdf":
                validatePdfFile(document);
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
                logger.warn("Unknown document type " + document.getContentType() + ". Document " + document.getUrl() + " won't be tested.");
        }

        return document;
    }
}
