package org.verapdf.crawler.core.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.validation.*;
import org.verapdf.crawler.api.validation.error.ValidationError;
import org.verapdf.crawler.configurations.VeraPDFServiceConfiguration;
import org.verapdf.crawler.db.CrawlJobDAO;
import org.verapdf.crawler.db.DocumentDAO;
import org.verapdf.crawler.db.ValidationErrorDAO;

import java.io.IOException;

public class VeraPDFValidator implements PDFValidator {

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    private static final int VALIDATION_TIMEOUT = 5 * 60 * 1000;      // 5 min
    private static final int VALIDATION_CHECK_INTERVAL = 5 * 1000;    // 5 sec
    private static final int MAX_VALIDATION_RETRIES = 2;

    private final String verapdfUrl;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper mapper;

    public VeraPDFValidator(VeraPDFServiceConfiguration configuration) {
        this.verapdfUrl = configuration.getUrl();
        this.httpClient = HttpClients.createDefault();
        this.mapper = new ObjectMapper();
    }

    @Override
    public VeraPDFValidationResult validate(ValidationJob job) throws Exception {
        VeraPDFValidationResult result;
        try {
            String localFilename = job.getFilePath();
            result = validate(localFilename);
            while (result == null) {
                logger.info("Could not reach validation service, retry in one minute");
                Thread.sleep(60 * 1000);
                result = validate(localFilename);
            }
        }
        catch (Throwable e) {
            logger.error("Error in validation service",e);
            result = new VeraPDFValidationResult();
            result.addValidationError(new ValidationError(e.getMessage()));
        }
        return result;
    }

    private VeraPDFValidationResult validate(String filename) throws Exception {
        logger.info("Sending file " + filename + " to validator");
        try {
            sendValidationRequest(filename);

            int validationRetries = 0;
            for (int i = 0; i < VALIDATION_TIMEOUT; ) {
                VeraPDFServiceStatus status = getValidationStatus();

                switch (status.getProcessorStatus()) {
                    case FINISHED:
                        logger.info("Validation is finished");
                        return status.getValidationResult();

                    case ACTIVE:
                        logger.info("Validation is in progress");
                        Thread.sleep(VALIDATION_CHECK_INTERVAL);
                        i += VALIDATION_CHECK_INTERVAL;
                        break;

                    default:
                        logger.info("Something went wrong and validation was not finished");
                        validationRetries++;
                        if (validationRetries == MAX_VALIDATION_RETRIES) {
                            throw new Exception("Failed to process document " + filename);
                        }
                        terminateValidation();
                        sendValidationRequest(filename);
                        i = 0; // Reset timeout cycle
                }
            }

            throw new Exception("Document " + filename + " was not validated in time (" + VALIDATION_TIMEOUT + " minutes)");

        } catch (IOException e) {
            logger.error("Error in validation service", e);
            return null;
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

    private void sendValidationRequest(String filename) throws IOException {
        HttpPost request = new HttpPost(verapdfUrl);
        request.setEntity(new StringEntity(filename));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_ACCEPTED:
                    logger.info("Validation request have been sent");
                    break;
                case HttpStatus.SC_LOCKED:
                    logger.warn("Another validation job is already in progress.");
                    break;
                default:
                    logger.warn("Unexpected response " + response.getStatusLine().getStatusCode() + ": "
                                                       + EntityUtils.toString(response.getEntity()));
            }
        }
    }

    private VeraPDFServiceStatus getValidationStatus() throws IOException {
        HttpGet request = new HttpGet(verapdfUrl);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return mapper.readValue(response.getEntity().getContent(), VeraPDFServiceStatus.class);
        }
    }

    private void terminateValidation() {
        HttpDelete request = new HttpDelete(verapdfUrl);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            logger.error("Failed to terminate validation job", e);
        }
    }
}