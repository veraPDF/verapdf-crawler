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
    private boolean running = false;
    private ValidationJob currentJob;
    private String stopReason;

    public ValidationService(ValidationJobDAO validationJobDAO, ValidationErrorDAO validationErrorDAO, DocumentDAO documentDAO, PDFValidator validator) {
        this.validationJobDAO = validationJobDAO;
        this.validationErrorDAO = validationErrorDAO;
        this.documentDAO = documentDAO;
        this.validator = validator;
    }

    public boolean isRunning() {
        return running;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void start() {
        running = true;
        stopReason = null;
        new Thread(this, "Thread-ValidationService").start();
    }

    // this method should be called in one synchronized(this object) block
    // with methods that remove crawl jobs
    public void cleanUnlinkedDocuments() {
        if (this.currentJob != null) {
            ValidationJob currentJobFromDB = validationJobDAO.current();

            if (!this.currentJob.getId().equals(currentJobFromDB.getId())) {
                throw new IllegalStateException("Validation service current job is not equal to DB current job");
            }
            if (currentJobFromDB.getDocument() == null) {
                this.currentJob = null;
                try {
                    validator.terminateValidation();
                } catch (IOException e) {
                    logger.error("Can't terminate current job", e);
                }
            }
        }
        validationJobDAO.bulkRemoveUnlinked();
    }

    @Override
    public void run() {
        logger.info("Validation service started");
        try {
            synchronized (this) {
                currentJob = currentJob();
            }
            if (currentJob != null) {
                try {
                    processStartedJob();
                } catch (IOException e) {
                    saveErrorResult(e);
                }
                synchronized (this) {
                    this.currentJob = null;
                }
            }

            while (running) {
                synchronized (this) {
                    currentJob = nextJob();
                }
                if (currentJob != null) {
                    logger.info("Validating " + currentJob.getId());
                    try {
                        validator.startValidation(currentJob);
                        processStartedJob();
                    } catch (IOException e) {
                        saveErrorResult(e);
                    }
                    synchronized (this) {
                        this.currentJob = null;
                    }
                    continue;
                }
                Thread.sleep(60 * 1000);
            }
        } catch (Throwable e) {
            logger.error("Fatal error in validator, stopping validation service.", e);
            this.stopReason = e.getMessage();
        } finally {
            running = false;
        }
    }

    private void processStartedJob() throws IOException, ValidationDeadlockException, InterruptedException {
        VeraPDFValidationResult result = validator.getValidationResult(currentJob);
        saveResult(result);
    }

    private void saveErrorResult(Throwable e) {
        VeraPDFValidationResult result = new VeraPDFValidationResult(e.getMessage());
        saveResult(result);
    }

    @SuppressWarnings("WeakerAccess") // @UnitOfWork works only with public methods
    @UnitOfWork
    public ValidationJob nextJob() {
        ValidationJob job = validationJobDAO.next();
        if (job != null) {
            job.setStatus(ValidationJob.Status.IN_PROGRESS);
        }
        return job;
    }

    @SuppressWarnings("WeakerAccess")
    @UnitOfWork
    public ValidationJob currentJob() {
        return validationJobDAO.current();
    }

    @SuppressWarnings("WeakerAccess")
    @UnitOfWork
    public void saveResult(VeraPDFValidationResult result) {
        synchronized (this) {
            if (currentJob != null) {
                try {
                    DomainDocument document = currentJob.getDocument();
                    document.setBaseTestResult(result.getTestResult());

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
                } finally {
                    cleanJob(currentJob);
                }
            }
        }
    }

    private void cleanJob(ValidationJob job) {
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