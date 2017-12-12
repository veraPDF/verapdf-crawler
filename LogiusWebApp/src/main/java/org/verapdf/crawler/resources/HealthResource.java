package org.verapdf.crawler.resources;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/healthcheck")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    private final int adminPort;

    public HealthResource(int adminPort) {
        this.adminPort = adminPort;
    }

    @GET
    public String getHealthCheck() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("http://localhost:" + adminPort + "/healthcheck");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return IOUtils.toString(response.getEntity().getContent());
            }
        }
    }
}
