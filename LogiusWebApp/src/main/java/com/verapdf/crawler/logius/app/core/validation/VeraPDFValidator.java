package com.verapdf.crawler.logius.app.core.validation;

import com.verapdf.crawler.logius.app.validation.ValidationJob;
import com.verapdf.crawler.logius.app.validation.VeraPDFServiceStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.verapdf.crawler.logius.app.validation.VeraPDFValidationResult;

import java.io.IOException;


@Service
public class VeraPDFValidator implements PDFValidator {

    private static final Logger logger = LoggerFactory.getLogger(VeraPDFValidator.class);

    private static final long CONNECTION_INTERVAL = 60 * 1000;
    private static final int MAX_CONNECTION_RETRIES = 5;
    private static final long GET_VALIDATION_RESULT_TIMEOUT = 5 * 60 * 1000;      // 5 min
    private static final long GET_VALIDATION_RESULT_CHECK_INTERVAL = 5 * 1000;    // 5 sec
    private static final int MAX_VALIDATION_RETRIES = 3;

    private final ValidationResource validationResource;

    public VeraPDFValidator(ValidationResource validationResource) {
        this.validationResource = validationResource;
    }

    @Override
    public void startValidation(ValidationJob job) throws IOException, ValidationDeadlockException {
        String localFilename = job.getFilePath();
        logger.info("Sending file " + localFilename + " to validator");
        sendValidationRequest(localFilename);
    }

    public VeraPDFValidationResult getValidationResult(ValidationJob job) throws IOException, ValidationDeadlockException, InterruptedException {
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

    // Temporary removed
    /*private void sendValidationSettings(ValidatedPDFDao validatedPDFDao) throws IOException {
        HttpPost propertiesPost = new HttpPost(verapdfUrl + "/settings");
        propertiesPost.setHeader("Content-Type", "application/json");
        ObjectMapper mapper = new ObjectMapper();
        propertiesPost.setEntity(new StringEntity(mapper.writeValueAsString(getValidationSettings())));
        httpClient.execute(propertiesPost);
        propertiesPost.releaseConnection();
        logger.info("Validation settings have been sent");
    }*/

    private void sendValidationRequest(String filename) throws ValidationDeadlockException {

        if (validationResource.processValidateRequest(filename)) {
            logger.info("Validation request have been sent");
            return;
        }
        logger.warn("Another validation job is already in progress.");
        throw new ValidationDeadlockException(ValidationDeadlockException.VALIDATOR_STATE_LOCKED);
    }

    private VeraPDFServiceStatus getValidationStatus() {
        return validationResource.getStatus();
    }

    public void terminateValidation() {
        validationResource.discardCurrentJob();

    }
}