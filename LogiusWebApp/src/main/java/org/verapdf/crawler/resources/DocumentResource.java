package org.verapdf.crawler.resources;

import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private CrawlJobDAO crawlJobDAO;
    private DocumentDAO documentDAO;
    private ValidationJobDAO validationJobDAO;

    public DocumentResource(CrawlJobDAO crawlJobDAO, DocumentDAO documentDAO, ValidationJobDAO validationJobDAO) {
        this.crawlJobDAO = crawlJobDAO;
        this.documentDAO = documentDAO;
        this.validationJobDAO = validationJobDAO;
    }

    @POST
    @UnitOfWork
    public DomainDocument saveDocument(DomainDocument document) {
        CrawlJob job = crawlJobDAO.getByHeritrixJobId(document.getCrawlJob().getHeritrixJobId());
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

    private void validatePdfFile(DomainDocument document) {
        ValidationJob validationJob = new ValidationJob(document);
        validationJobDAO.save(validationJob);
    }

    private void validateOpenOfficeFile(DomainDocument document) {
        document.setBaseTestResult(DomainDocument.BaseTestResult.OPEN);
    }

    private void validateMSOfficeFile(DomainDocument document) {
        document.setBaseTestResult(DomainDocument.BaseTestResult.NOT_OPEN);
    }
}
