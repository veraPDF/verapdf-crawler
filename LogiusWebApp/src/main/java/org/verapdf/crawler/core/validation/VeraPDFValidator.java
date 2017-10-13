package org.verapdf.crawler.core.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.common.GracefulHttpClient;
import org.verapdf.crawler.api.validation.ValidationJob;
import org.verapdf.crawler.api.validation.VeraPDFServiceStatus;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;
import org.verapdf.crawler.configurations.VeraPDFServiceConfiguration;

import java.io.IOException;

public class VeraPDFValidator implements PDFValidator {

    private static final Logger logger = LoggerFactory.getLogger(VeraPDFValidator.class);

    private static final long CONNECTION_INTERVAL = 60 * 1000;
    private static final int MAX_CONNECTION_RETRIES = 5;
    private static final long GET_VALIDATION_RESULT_TIMEOUT = 5 * 60 * 1000;      // 5 min
    private static final long GET_VALIDATION_RESULT_CHECK_INTERVAL = 5 * 1000;    // 5 sec
    private static final int MAX_VALIDATION_RETRIES = 3;

    private final String verapdfUrl;
    private final ObjectMapper mapper;

    public VeraPDFValidator(VeraPDFServiceConfiguration configuration) {
        this.verapdfUrl = configuration.getUrl();
        this.mapper = new ObjectMapper();
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

    private void sendValidationRequest(String filename) throws IOException, ValidationDeadlockException {
        HttpPost request = new HttpPost(verapdfUrl);
        request.setEntity(new StringEntity(filename));

        try (CloseableHttpClient httpClient = new GracefulHttpClient(MAX_CONNECTION_RETRIES, CONNECTION_INTERVAL)) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                switch (response.getStatusLine().getStatusCode()) {
                    case HttpStatus.SC_ACCEPTED:
                        logger.info("Validation request have been sent");
                        return;
                    case HttpStatus.SC_LOCKED:
                        logger.warn("Another validation job is already in progress.");
                        throw new ValidationDeadlockException(ValidationDeadlockException.VALIDATOR_STATE_LOCKED);

                    default:
                        logger.error("Unexpected response " + response.getStatusLine().getStatusCode() + ": "
                                + EntityUtils.toString(response.getEntity()));
                        throw new ValidationDeadlockException(ValidationDeadlockException.VALIDATOR_STATE_UNKNOWN);
                }
            }
        } catch (IOException e) {
            logger.error("Fail to post file to VeraPDFValidationService", e);
            throw e;
        }
    }

    private VeraPDFServiceStatus getValidationStatus() throws IOException {
        HttpGet request = new HttpGet(verapdfUrl);
        try (CloseableHttpClient httpClient = new GracefulHttpClient(MAX_CONNECTION_RETRIES, CONNECTION_INTERVAL)) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return mapper.readValue(response.getEntity().getContent(), VeraPDFServiceStatus.class);
            }
        } catch (IOException e) {
            logger.error("Failed to get validation result", e);
            throw e;
        }
    }

    public void terminateValidation() throws IOException {
        HttpDelete request = new HttpDelete(verapdfUrl);
        try (CloseableHttpClient httpClient = new GracefulHttpClient(MAX_CONNECTION_RETRIES, CONNECTION_INTERVAL)) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                EntityUtils.consume(response.getEntity());
            }
        } catch (IOException e) {
            logger.error("Failed to terminate validation job", e);
            throw e;
        }
    }
}