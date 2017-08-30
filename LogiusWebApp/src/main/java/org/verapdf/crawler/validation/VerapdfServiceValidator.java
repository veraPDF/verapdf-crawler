package org.verapdf.crawler.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.validation.ValidationError;
import org.verapdf.crawler.domain.validation.ValidationSettings;
import org.verapdf.crawler.domain.validation.VeraPDFValidationResult;
import org.verapdf.crawler.repository.document.ValidatedPDFDao;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Path("/verapdf-service")
public class VerapdfServiceValidator implements PDFValidator {

    private final static int MAX_VALIDATION_TIMEOUT_IN_MINUTES = 5;
    private final static int MAX_VALIDATION_RETRIES = 2;

    private final String verapdfUrl;
    private final HttpClient httpClient;
    private final ValidatedPDFDao validatedPDFDao;
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    public VerapdfServiceValidator(String verapdfUrl, ValidatedPDFDao validatedPDFDao) {
        this.verapdfUrl = verapdfUrl;
        httpClient = HttpClientBuilder.create().build();
        this.validatedPDFDao = validatedPDFDao;
    }

    @GET
    @Path("/settings")
    public ValidationSettings getValidationSettings() {
        return new ValidationSettings(validatedPDFDao.getPdfPropertiesWithXpath(), new HashMap<>());
    }

    @POST
    @Path("/result")
    public void setValidationResult(VeraPDFValidationResult result) {
        
    }

    @Override
    public boolean validateAndWirteResult(String localFilename, String fileUrl) throws Exception {
        VeraPDFValidationResult result;
        try {
            result = validate(localFilename, validatedPDFDao);
            while (result == null) {
                logger.info("Could not reach validation service, retry in one minute");
                Thread.sleep(60 * 1000);
                result = validate(localFilename, validatedPDFDao);
            }
        }
        catch (Exception e) {
            logger.error("Error in validation service",e);
            validatedPDFDao.addProcessingError(e.getMessage(), fileUrl);
            return false;
        }
        for(ValidationError error: result.getValidationErrors()) {
            validatedPDFDao.addErrorToDocument(error, fileUrl);
        }
        for(Map.Entry<String, String> property: result.getProperties().entrySet()) {
            validatedPDFDao.insertPropertyForDocument(property.getKey(), property.getValue(), fileUrl);
        }
        validatedPDFDao.addProcessingError(result.getProcessingError(), fileUrl);
        return result.isValid();
    }

    private VeraPDFValidationResult validate(String filename, ValidatedPDFDao validatedPDFDao) throws Exception {
        logger.info("Sending file " + filename + " to validator");
        try {
            sendValidationSettings(validatedPDFDao);
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
                    sendValidationSettings(validatedPDFDao);
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

    private void sendValidationSettings(ValidatedPDFDao validatedPDFDao) throws IOException {
        HttpPost propertiesPost = new HttpPost(verapdfUrl + "/properties");
        propertiesPost.setHeader("Content-Type", "application/json");
        ObjectMapper mapper = new ObjectMapper();
        propertiesPost.setEntity(new StringEntity(mapper.writeValueAsString(getValidationSettings())));
        httpClient.execute(propertiesPost);
        propertiesPost.releaseConnection();
        logger.info("Validation settings have been sent");
    }

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