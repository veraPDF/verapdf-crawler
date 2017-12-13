package org.verapdf.crawler.resources;

import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.ResourceManager;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.api.document.DomainDocument;
import org.verapdf.crawler.api.validation.ValidationJob;
import org.verapdf.crawler.db.CrawlJobDAO;
import org.verapdf.crawler.db.DocumentDAO;
import org.verapdf.crawler.db.ValidationJobDAO;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    private static final Logger logger = LoggerFactory.getLogger(DocumentResource.class);

    private ResourceManager resourceManager;

    public DocumentResource(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @POST
    @UnitOfWork
    public DomainDocument saveDocument(DomainDocument document) {
        CrawlJob job = resourceManager.getCrawlJobDAO().getByHeritrixJobId(document.getCrawlJob().getHeritrixJobId());
        if (job == null) {
            return null;
        }
        return saveDocument(document, job, resourceManager);
    }

    public static DomainDocument saveDocument(DomainDocument document, CrawlJob job, ResourceManager resourceManager) {
        document.setCrawlJob(job);

        resourceManager.getDocumentDAO().save(document);

        switch (document.getContentType()) {
            case "pdf":
                validatePdfFile(document, resourceManager.getValidationJobDAO());
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
}
