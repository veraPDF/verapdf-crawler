package org.verapdf.crawler.core.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.validation.ValidationError;
import org.verapdf.crawler.api.validation.ValidationJobData;
import org.verapdf.crawler.api.validation.ValidationSettings;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;
import org.verapdf.crawler.configurations.VeraPDFServiceConfiguration;
import org.verapdf.crawler.db.document.InsertDocumentDao;
import org.verapdf.crawler.db.document.ValidatedPDFDao;
import org.verapdf.crawler.db.jobs.CrawlJobDao;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class VeraPDFValidator implements PDFValidator {

    private static final int MAX_VALIDATION_TIMEOUT_IN_MINUTES = 5;
    private static final int MAX_VALIDATION_RETRIES = 2;

    private final String verapdfUrl;
    private final HttpClient httpClient;
    private final InsertDocumentDao insertDocumentDao;
    private final CrawlJobDao crawlJobDao;
    private final ValidatedPDFDao validatedPDFDao;
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    public VeraPDFValidator(VeraPDFServiceConfiguration configuration, InsertDocumentDao insertDocumentDao, ValidatedPDFDao validatedPDFDao, CrawlJobDao crawlJobDao) {
        this.verapdfUrl = configuration.getUrl();
        this.httpClient = HttpClientBuilder.create().build();
        this.insertDocumentDao = insertDocumentDao;
        this.validatedPDFDao = validatedPDFDao;
        this.crawlJobDao = crawlJobDao;
    }

    @Override
    public void validateAndWriteResult(ValidationJobData data) throws Exception {
        VeraPDFValidationResult result;
        String fileUrl = data.getUri();
        try {
            String localFilename = data.getFilepath();
            result = validate(localFilename, validatedPDFDao);
            while (result == null) {
                logger.info("Could not reach validation service, retry in one minute");
                Thread.sleep(60 * 1000);
                result = validate(localFilename, validatedPDFDao);
            }
        }
        catch (Throwable e) {
            logger.error("Error in validation service",e);
            result = new VeraPDFValidationResult();
            result.addValidationError(new ValidationError(e.getMessage()));
        }
        String domain = crawlJobDao.getCrawlUrl(data.getJobID());
        insertDocumentDao.addPdfFile(data, domain, getStatus(result));
        for(ValidationError error: result.getValidationErrors()) {
            validatedPDFDao.addErrorToDocument(error, fileUrl);
        }
        for(Map.Entry<String, String> property: result.getProperties().entrySet()) {
            validatedPDFDao.insertPropertyForDocument(property.getKey(), property.getValue(), fileUrl);
        }
    }

    private InsertDocumentDao.Status getStatus(VeraPDFValidationResult result) {
        return result.isValid() ? InsertDocumentDao.Status.OPEN : InsertDocumentDao.Status.NOT_OPEN;
    }

    private VeraPDFValidationResult validate(String filename, ValidatedPDFDao validatedPDFDao) throws Exception {
        logger.info("Sending file " + filename + " to validator");
        try {
            sendValidationRequest(filename);

            int validationRetries = 0;
            for (int i = 0; i < MAX_VALIDATION_TIMEOUT_IN_MINUTES * 12; i++) {
                int responseCode = getValidationStatus();
                if (responseCode == HttpStatus.SC_OK) { // Vaidation is finished
                    logger.info("Validation is finished");
                    return getValidationResult();
                }
                // Validation is in process
                if (responseCode == HttpStatus.SC_PROCESSING) {
                    logger.info("Validation is in progress");
                    Thread.sleep(5 * 1000);
                    continue;
                }
                // Something went wrong and validation was not finished
                if (responseCode == HttpStatus.SC_CONTINUE) {
                    logger.info("Something went wrong and validation was not finished");
                    validationRetries++;
                    if (validationRetries == MAX_VALIDATION_RETRIES) {
                        throw new Exception("Failed to process document " + filename);
                    }
                    sendValidationRequest(filename);
                    // Reset timeout cycle
                    i = 0;
                } else { // Got unexpected response code
                    throw new Exception("Invalid response code from validation service, code was" + responseCode);
                }
            }
            throw new Exception("Document " + filename + " was not validated in time (" + MAX_VALIDATION_TIMEOUT_IN_MINUTES + " minutes)");
        }
        catch (IOException e) {
            logger.error("Error in validation service", e);
            return null;
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
        HttpPost post = new HttpPost(verapdfUrl);
        post.setEntity(new StringEntity(filename));
        httpClient.execute(post);
        post.releaseConnection();
        logger.info("Validation request have been sent");
    }

    private int getValidationStatus() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(verapdfUrl).openConnection();
        connection.setRequestMethod("GET");

        int result = connection.getResponseCode();
        logger.info("Response code is " + result);
        return result;
    }

    private VeraPDFValidationResult getValidationResult() throws IOException {
        HttpGet get = new HttpGet(verapdfUrl);
        VeraPDFValidationResult result = new ObjectMapper().readValue(httpClient.execute(new HttpGet(verapdfUrl)).getEntity().getContent(), VeraPDFValidationResult.class);
        get.releaseConnection();
        return result;
    }
}