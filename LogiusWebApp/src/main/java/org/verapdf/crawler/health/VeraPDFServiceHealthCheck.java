package org.verapdf.crawler.health;

import com.codahale.metrics.health.HealthCheck;
import org.verapdf.crawler.configurations.VeraPDFServiceConfiguration;

import java.net.HttpURLConnection;
import java.net.URL;

public class VeraPDFServiceHealthCheck extends HealthCheck {
    private final String verapdfServiceUrl;

    public VeraPDFServiceHealthCheck(VeraPDFServiceConfiguration veraPDFServiceConfiguration) {
        this.verapdfServiceUrl = veraPDFServiceConfiguration.getUrl();
    }

    @Override
    protected Result check() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(verapdfServiceUrl).openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        if(responseCode == 200 || responseCode == 100 || responseCode == 102) {
            return Result.healthy();
        }
        return Result.unhealthy("VeraPDF service is not available");
    }
}
