package org.verapdf.crawler.logius.core.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.validation.ValidationJob;
import org.verapdf.crawler.logius.validation.VeraPDFServiceStatus;
import org.verapdf.crawler.logius.validation.VeraPDFValidationResult;
import org.verapdf.crawler.logius.validation.settings.ValidationSettings;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class VeraPDFValidator implements PDFValidator {

    private static final Logger logger = LoggerFactory.getLogger(VeraPDFValidator.class);
    private static final long GET_VALIDATION_RESULT_TIMEOUT = 5 * 60 * 1000;      // 5 min
    private static final long GET_VALIDATION_RESULT_CHECK_INTERVAL = 5 * 1000;    // 5 sec
    private static final int MAX_VALIDATION_RETRIES = 3;
    private final File veraPDFErrorLog;
    private final ValidationSettings validationSettings;
    private final ExecutorService service = Executors.newFixedThreadPool(1);
    private String veraPDFPath;
    private VeraPDFProcessor veraPDFProcessor;
    private VeraPDFValidationResult validationResult;
    private boolean isAborted = false;

    public VeraPDFValidator(@Value("${veraPDFService.verapdfPath}") String veraPDFPath,
                            @Value("${veraPDFService.verapdfErrors}") String veraPDFErrorFilePath,
                            ValidationSettings validationSettings) {
        this.veraPDFPath = veraPDFPath;
        this.validationSettings = validationSettings;
        this.veraPDFErrorLog = new File(veraPDFErrorFilePath);
    }

    @Override
    public void startValidation(ValidationJob job) throws ValidationDeadlockException {
        String localFilename = job.getFilePath();
        logger.info("Sending file " + localFilename + " to validator");
        sendValidationRequest(localFilename);
    }

    public VeraPDFValidationResult getValidationResult(ValidationJob job) throws ValidationDeadlockException, InterruptedException {
        try {
            int validationRetries = 0;
            if (job == null) {
                return new VeraPDFValidationResult("Validation can't be performed for empty validation job");
            }
            String filename = job.getFilePath();

            long endTime = System.currentTimeMillis() + GET_VALIDATION_RESULT_TIMEOUT;
            while (System.currentTimeMillis() < endTime) {
                VeraPDFServiceStatus status = getValidationStatus();

                switch (status.getProcessorStatus()) {
                    case FINISHED:
                        logger.info("Validation is finished");
                        return status.getValidationResult();

                    case ACTIVE:
                        logger.info("Validation is in progress");
                        Thread.sleep(GET_VALIDATION_RESULT_CHECK_INTERVAL);
                        break;

                    case ABORTED:
                        logger.info("Validation is aborted");
                        return new VeraPDFValidationResult("Validation was aborted");

                    default:
                        logger.info("Something went wrong and validation was not finished");
                        if (++validationRetries == MAX_VALIDATION_RETRIES) {
                            throw new ValidationDeadlockException(ValidationDeadlockException.VALIDATOR_STATE_IDLE);
                        }
                        sendValidationRequest(filename);
                        endTime = System.currentTimeMillis() + GET_VALIDATION_RESULT_TIMEOUT; // Reset timeout cycle
                }
            }
            return new VeraPDFValidationResult("Document was not validated in time (" + GET_VALIDATION_RESULT_TIMEOUT + " minutes)");
        } finally {
            // Cleanup validation service, if there is nothing to cleanup VeraPDF service will just ignore this.
            terminateValidation();
        }
    }

    void validationFinished(VeraPDFValidationResult result) {
        this.validationResult = result;
        this.veraPDFProcessor = null;
        //TODO: send message to main service
    }

    public void terminateValidation() {
        logger.info("Terminating current job");
        if (this.veraPDFProcessor != null) {
            this.veraPDFProcessor.stopProcess();
            this.veraPDFProcessor = null;
        }
        validationResult = null;
        isAborted = true;
    }

    private void sendValidationRequest(String filename) throws ValidationDeadlockException {
        logger.info("Starting processing of " + filename);
        synchronized (this) {
            if (evaluateStatus() == VeraPDFServiceStatus.ProcessorStatus.ACTIVE) {
                logger.warn("Another validation job is already in progress.");
                throw new ValidationDeadlockException(ValidationDeadlockException.VALIDATOR_STATE_LOCKED);
            }
        }
        isAborted = false;
        validate(filename);
        logger.info("Validation request have been sent");
    }

    private VeraPDFServiceStatus getValidationStatus() {
        VeraPDFServiceStatus.ProcessorStatus processorStatus = evaluateStatus();
        logger.info("Status requested, processorStatus is " + processorStatus);
        return new VeraPDFServiceStatus(processorStatus, validationResult);
    }

    private void validate(String filename) {
        this.veraPDFProcessor = new VeraPDFProcessor(veraPDFPath, veraPDFErrorLog, filename, this, validationSettings);
        service.submit(veraPDFProcessor);
    }

    private VeraPDFServiceStatus.ProcessorStatus evaluateStatus() {
        if (this.veraPDFProcessor != null) {
            return VeraPDFServiceStatus.ProcessorStatus.ACTIVE;
        } else if (validationResult != null) {
            return VeraPDFServiceStatus.ProcessorStatus.FINISHED;
        } else {
            return isAborted ? VeraPDFServiceStatus.ProcessorStatus.ABORTED : VeraPDFServiceStatus.ProcessorStatus.IDLE;
        }
    }
}