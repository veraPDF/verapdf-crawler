package org.verapdf.crawler.logius.core.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.service.ValidationSettingsService;
import org.verapdf.crawler.logius.validation.VeraPDFServiceStatus;
import org.verapdf.crawler.logius.validation.VeraPDFValidationResult;

import javax.annotation.PostConstruct;
import java.io.File;


@Service
public class VeraPDFValidator implements PDFValidator {

    private static final Logger logger = LoggerFactory.getLogger(VeraPDFValidator.class);
    private static final long GET_VALIDATION_RESULT_TIMEOUT = 5 * 60 * 1000;      // 5 min
    private static final long GET_VALIDATION_RESULT_CHECK_INTERVAL = 5 * 1000;    // 5 sec
    private static final int MAX_VALIDATION_RETRIES = 3;
    private ThreadPoolTaskExecutor service;
    private VeraPDFProcessor veraPDFProcessor;
    private VeraPDFValidationResult validationResult;
    private ObjectFactory<VeraPDFProcessor> veraPDFProcessorObjectFactory;
    private boolean isAborted = false;
    private final ValidationSettingsService validationSettingsService;

    public VeraPDFValidator(ObjectFactory<VeraPDFProcessor> veraPDFProcessorObjectFactory,
                            ValidationSettingsService validationSettingsService) {
        this.veraPDFProcessorObjectFactory = veraPDFProcessorObjectFactory;
        this.validationSettingsService = validationSettingsService;
    }

    @PostConstruct
    public void init(){
        service = new ThreadPoolTaskExecutor();
        service.setCorePoolSize(1);
        service.initialize();
    }

    @Override
    public void startValidation(File job, boolean isValidationDisabled) throws ValidationDeadlockException {
        logger.info("Sending file " + job.getAbsolutePath() + " to validator");
        sendValidationRequest(job, isValidationDisabled);
    }

    public VeraPDFValidationResult getValidationResult(File job, boolean isValidationDisabled) throws ValidationDeadlockException, InterruptedException {
        try {
            int validationRetries = 0;
            if (job == null) {
                return new VeraPDFValidationResult("Validation can't be performed for empty validation job");
            }

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
                        sendValidationRequest(job, isValidationDisabled);
                        endTime = System.currentTimeMillis() + GET_VALIDATION_RESULT_TIMEOUT; // Reset timeout cycle
                }
            }
            return new VeraPDFValidationResult("Document was not validated in time (" + GET_VALIDATION_RESULT_TIMEOUT + " minutes)");
        } finally {
            // Cleanup validation service, if there is nothing to cleanup VeraPDF service will just ignore this.
            terminateValidation();
        }
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

    private void sendValidationRequest(File file, boolean isValidationDisabled) throws ValidationDeadlockException {
        logger.info("Starting processing of " + file.getAbsolutePath());
        synchronized (this) {
            if (evaluateStatus() == VeraPDFServiceStatus.ProcessorStatus.ACTIVE) {
                logger.warn("Another validation job is already in progress.");
                throw new ValidationDeadlockException(ValidationDeadlockException.VALIDATOR_STATE_LOCKED);
            }
        }
        isAborted = false;
        validate(file, isValidationDisabled);
        logger.info("Validation request have been sent");
    }

    private VeraPDFServiceStatus getValidationStatus() {
        VeraPDFServiceStatus.ProcessorStatus processorStatus = evaluateStatus();
        logger.info("Status requested, processorStatus is " + processorStatus);
        return new VeraPDFServiceStatus(processorStatus, validationResult);
    }

    private void validate(File job, boolean isValidationDisabled) {
        this.veraPDFProcessor = veraPDFProcessorObjectFactory.getObject();
        veraPDFProcessor.setFilePath(job);
        veraPDFProcessor.setSettings(validationSettingsService.getValidationSettings());
        veraPDFProcessor.setValidationDisabled(isValidationDisabled);
        service.submitListenable(veraPDFProcessor).completable().thenAccept(result ->{
            this.validationResult = result;
            this.veraPDFProcessor = null;
        });
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