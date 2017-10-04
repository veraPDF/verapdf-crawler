package org.verapdf.crawler.core.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.validation.ValidationJob;
import org.verapdf.crawler.api.validation.VeraPDFServiceStatus;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;
import org.verapdf.crawler.api.validation.error.ValidationError;
import org.verapdf.crawler.configurations.VeraPDFServiceConfiguration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.function.Function;

public class VeraPDFValidator implements PDFValidator {

    private static final Logger logger = LoggerFactory.getLogger(VeraPDFValidator.class);

    private static final int CONNECTION_INTERVAL = 60 * 1000;
    private static final int VALIDATION_TIMEOUT = 5 * 60 * 1000;      // 5 min
    private static final int VALIDATION_CHECK_INTERVAL = 5 * 1000;    // 5 sec
    private static final int MAX_VALIDATION_RETRIES = 2;
    private static final int MAX_CONNECTION_RETRIES = 5;

    private final String verapdfUrl;
    private final ObjectMapper mapper;

    public VeraPDFValidator(VeraPDFServiceConfiguration configuration) {
        this.verapdfUrl = configuration.getUrl();
        this.mapper = new ObjectMapper();
    }

    @Override
    public boolean startValidation(ValidationJob job) {
        String localFilename = job.getFilePath();
        logger.info("Sending file " + localFilename + " to validator");
        return sendValidationRequest(localFilename);
    }

    public VeraPDFValidationResult getValidationResult(ValidationJob job) throws Throwable {
        try {
            int validationRetries = 0;
            String filename = job.getFilePath();
            for (int i = 0; i < VALIDATION_TIMEOUT; ) {
                VeraPDFServiceStatus status = getValidationStatus();
                if (status == null) {
                    Thread.sleep(VALIDATION_CHECK_INTERVAL);
                    i += VALIDATION_CHECK_INTERVAL;
                }

                switch (status.getProcessorStatus()) {
                    case FINISHED:
                        logger.info("Validation is finished");
                        return status.getValidationResult();

                    case ACTIVE:
                        logger.info("Validation is in progress");
                        Thread.sleep(VALIDATION_CHECK_INTERVAL);
                        i += VALIDATION_CHECK_INTERVAL;
                        break;

                    case ABORTED:
                        throw new Exception("Document " + filename + " has been aborted");

                    default:
                        logger.info("Something went wrong and validation was not finished");
                        validationRetries++;
                        if (validationRetries == MAX_VALIDATION_RETRIES) {
                            throw new Exception("Failed to process document " + filename);
                        }
                        terminateValidation();
                        if (!sendValidationRequest(filename)) {
                            throw new Exception("Failed to process document " + filename);
                        }
                        i = 0; // Reset timeout cycle
                }
            }
            throw new Exception("Document " + filename + " was not validated in time (" + VALIDATION_TIMEOUT + " minutes)");
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

    private boolean sendValidationRequest(String filename) {
        HttpPost request = new HttpPost(verapdfUrl);
        try {
            request.setEntity(new StringEntity(filename));
        } catch (UnsupportedEncodingException e) {
            logger.error("Fail to construct send validation request body", e);
            // TODO: send email to admin
            return true;
        }

        try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_ACCEPTED:
                    logger.info("Validation request have been sent");
                    return true;
                case HttpStatus.SC_LOCKED:
                    logger.warn("Another validation job is already in progress.");
                    // TODO: send email to admin
                    return true;
                default:
                    logger.warn("Unexpected response " + response.getStatusLine().getStatusCode() + ": "
                                                       + EntityUtils.toString(response.getEntity()));
                    return false;
            }
        } catch (IOException e) {
            logger.error("Fail to post file to VeraPDFValidationService", e);
            return false;
        }
    }

    private VeraPDFServiceStatus getValidationStatus() {
        HttpGet request = new HttpGet(verapdfUrl);
        try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
            return mapper.readValue(response.getEntity().getContent(), VeraPDFServiceStatus.class);
        } catch (IOException e) {
            logger.error("Failed to get validation result", e);
            return null;
        }
    }

    public void terminateValidation() {
        HttpDelete request = new HttpDelete(verapdfUrl);
        try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            logger.error("Failed to terminate validation job", e);
        }
    }
}