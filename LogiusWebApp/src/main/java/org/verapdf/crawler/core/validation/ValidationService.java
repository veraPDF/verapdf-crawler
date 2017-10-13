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

	public ValidationJob getCurrentJob() {
		return currentJob;
	}

	private void setCurrentJob(ValidationJob currentJob) {
    	synchronized (ValidationService.class) {
			this.currentJob = currentJob;
		}
	}

	public void start() {
        running = true;
        stopReason = null;
        new Thread(this, "Thread-ValidationService").start();
    }

    public void abortCurrentJob() {
		try {
			logger.info("Aborting current job");
			currentJob.setStatus(ValidationJob.Status.ABORTED);
			validator.terminateValidation();
		} catch (IOException e) {
			logger.error("Can't terminate current job", e);
		}
	}

    @Override
    public void run() {
        logger.info("Validation service started");
        try {
			setCurrentJob(retrieveCurrentJob());
            if (currentJob != null) {
                try {
                    processStartedJob();
                } catch (IOException e) {
                    saveErrorResult(e);
                }
            }

            while (running) {
                setCurrentJob(retrieveNextJob());
                if (currentJob != null) {
                    logger.info("Validating " + currentJob.getId());
                    try {
                        validator.startValidation(currentJob);
                        processStartedJob();
                    } catch (IOException e) {
                        saveErrorResult(e);
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
    public ValidationJob retrieveNextJob() {
		logger.debug("Getting next job");
        ValidationJob job = validationJobDAO.next();
        if (job != null) {
            job.setStatus(ValidationJob.Status.IN_PROGRESS);
        }
        return job;
    }

    @SuppressWarnings("WeakerAccess")
    @UnitOfWork
    public ValidationJob retrieveCurrentJob() {
		logger.debug("Getting current job");
        return validationJobDAO.current();
    }

    @SuppressWarnings("WeakerAccess")
    @UnitOfWork
    public void saveResult(VeraPDFValidationResult result) {
        try {
            if (!currentJob.getStatus().equals(ValidationJob.Status.ABORTED)) {
				logger.debug("Saving validation job results");
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
            } else {
            	logger.debug("Validation job was aborted, don't save any results");
			}
        } finally {
            cleanJob(currentJob);
        }
    }

    private void cleanJob(ValidationJob job) {
    	logger.debug("Cleanup validation job");
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