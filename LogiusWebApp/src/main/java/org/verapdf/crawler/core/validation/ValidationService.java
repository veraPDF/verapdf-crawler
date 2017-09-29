package org.verapdf.crawler.core.validation;

import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.document.DomainDocument;
import org.verapdf.crawler.api.validation.ValidationJob;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;
import org.verapdf.crawler.api.validation.error.ValidationError;
import org.verapdf.crawler.db.DocumentDAO;
import org.verapdf.crawler.db.ValidationErrorDAO;
import org.verapdf.crawler.db.ValidationJobDAO;

import java.io.*;
import java.util.List;

public class ValidationService implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    private final ValidationJobDAO validationJobDAO;
    private final ValidationErrorDAO validationErrorDAO;
    private final DocumentDAO documentDAO;
    private final PDFValidator validator;
    private boolean running;

    public ValidationService(ValidationJobDAO validationJobDAO, ValidationErrorDAO validationErrorDAO, DocumentDAO documentDAO, PDFValidator validator) {
        running = false;
        this.validationJobDAO = validationJobDAO;
        this.validationErrorDAO = validationErrorDAO;
        this.documentDAO = documentDAO;
        this.validator = validator;
    }

    public void start() {
        running = true;
        new Thread(this, "Thread-ValidationService").start();
    }

    @Override
    public void run() {
        logger.info("Validation service started");
        while (running) {
            ValidationJob job = null;
            try {
                job = nextJob();
                if(job != null) {
                    logger.info("Validating " + job.getDocument().getUrl());
                    VeraPDFValidationResult result = validator.validate(job);
                    saveResult(job, result);
                    continue;
                }
            } catch (Exception e) {
                logger.error("Error in validation runner", e);
            } finally {
                cleanJob(job);
            }

            try {
                Thread.sleep(60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @UnitOfWork
    public ValidationJob nextJob() {
        ValidationJob job = validationJobDAO.next();
        if (job != null) {
            job.setStatus(ValidationJob.Status.IN_PROGRESS);
        }
        return job;
    }

    @UnitOfWork
    public void saveResult(ValidationJob job, VeraPDFValidationResult result) {
        DomainDocument document = job.getDocument();
        document.setBaseTestResult(result.getBaseTestResult());

        // Save errors where needed
        List<ValidationError> validationErrors = result.getValidationErrors();
        for (int index = 0; index < validationErrors.size(); index++) {
            validationErrors.set(index, validationErrorDAO.save(validationErrors.get(index)));
        }
        document.setValidationErrors(validationErrors);

        // Link properties
        document.setProperties(result.getProperties());

        // And update document (note that document was detached from hibernate context, thus we need to save explicitly)
        documentDAO.save(document);
    }

    @UnitOfWork
    public void cleanJob(ValidationJob job) {
        if (job == null) {
            return;
        }
        if (job.getFilePath() != null) {
            if (!new File(job.getFilePath()).delete()) {
                logger.warn("Failed to clean validation job file " + job.getFilePath());
            }
        }
        validationJobDAO.remove(job);
    }
}