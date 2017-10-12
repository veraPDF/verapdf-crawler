package org.verapdf.crawler.health;

import com.codahale.metrics.health.HealthCheck;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.verapdf.crawler.configurations.VeraPDFServiceConfiguration;

public class VeraPDFServiceHealthCheck extends HealthCheck {
    private final String verapdfServiceUrl;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    public VeraPDFServiceHealthCheck(VeraPDFServiceConfiguration veraPDFServiceConfiguration) {
        this.verapdfServiceUrl = veraPDFServiceConfiguration.getUrl();
    }

    @Override
    protected Result check() throws Exception {
        try {
            HttpGet request = new HttpGet(verapdfServiceUrl);
            try(CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    return Result.healthy();
                } else {
                    return Result.unhealthy("VeraPDF service is not available. Response "
                            + response.getStatusLine().getStatusCode() + ": "
                            + EntityUtils.toString(response.getEntity()));
                }
            }
        } catch (Exception e) {
            return Result.unhealthy("VeraPDF service is not available. Details: " + e.getMessage());
        }
    }
}
