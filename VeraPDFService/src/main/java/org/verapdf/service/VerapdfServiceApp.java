package org.verapdf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.verapdf.crawler.domain.validation.ValidationSettings;

import java.io.IOException;

public class VerapdfServiceApp extends Application<VeraPDFServiceConfiguration> {

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
    public void run(VeraPDFServiceConfiguration configuration, Environment environment) throws IOException {
        environment.jersey().setUrlPattern("/*");
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpGet(configuration.getLogiusUrl() + "/verapdf-service/settings"));
        environment.jersey().register(new ValidationResource(configuration.getVerapdfPath(), new ObjectMapper().readValue(response.getEntity().getContent(), ValidationSettings.class)));
    }
}
