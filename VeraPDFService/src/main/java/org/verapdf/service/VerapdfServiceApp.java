package org.verapdf.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.common.GracefulHttpClient;
import org.verapdf.common.RetryFailedException;
import org.verapdf.crawler.api.validation.settings.ValidationSettings;

import java.io.InputStream;

public class VerapdfServiceApp extends Application<VeraPDFServiceConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(VerapdfServiceApp.class);

    private static final int LOAD_SETTINGS_MAX_ATTEMPTS = 3;
    private static final int LOAD_SETTINGS_ATTEMPT_INTERVAL = 10 * 1000;

    public static void main(String[] args) throws Exception {
        new VerapdfServiceApp().run(args);
    }

    @Override
    public String getName() {
        return "VeraPDF Service";
    }

    @Override
    public void initialize(Bootstrap<VeraPDFServiceConfiguration> bootstrap) { }

    @Override
    public void run(VeraPDFServiceConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().setUrlPattern("/*");

        ObjectMapper mapper = environment.getObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        ValidationSettings validationSettings = loadValidationSettings(configuration.getLogiusUrl(), mapper);

        environment.jersey().register(new ValidationResource(configuration.getVerapdfPath(), validationSettings));
    }

    private ValidationSettings loadValidationSettings(String logiusUrl, ObjectMapper mapper) throws Exception {
        try (CloseableHttpClient httpClient = new GracefulHttpClient(LOAD_SETTINGS_MAX_ATTEMPTS, LOAD_SETTINGS_ATTEMPT_INTERVAL)) {
            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(logiusUrl + "/verapdf-service/settings"))) {
                InputStream responseBody = response.getEntity().getContent();
                if (response.getStatusLine().getStatusCode() == 200) {
                    return mapper.readValue(responseBody, ValidationSettings.class);
                } else {
                    logger.error("Fail to get settings from main Logius web application. Response:\n" + IOUtils.toString(responseBody));
                }
            }
        } catch (RetryFailedException e) {
            logger.error("Fail to get settings from main Logius web application in " + e.getTimeSpent() + "ms", e);
        }
        throw new Exception("Fail to get settings from main Logius web application");
    }
}
