package org.verapdf.crawler.app;

import org.verapdf.crawler.configuration.LogiusConfiguration;
import org.verapdf.crawler.engine.HeritrixClient;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.verapdf.crawler.resources.CrawlJobResource;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class LogiusWebApplication extends Application<LogiusConfiguration> {
    public static void main(String[] args) throws Exception {
        new LogiusWebApplication().run(args);
    }

    @Override
    public String getName() {
        return "Logius";
    }

    @Override
    public void initialize(Bootstrap<LogiusConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/html", "/", "index.html", "main"));
        bootstrap.addBundle(new AssetsBundle("/html", "/jobinfo", "info.html", "status"));
        bootstrap.addBundle(new AssetsBundle("/html", "/email", "email.html", "email"));
    }

    @Override
    public void run(LogiusConfiguration configuration,
                    Environment environment) {
        environment.jersey().setUrlPattern("/crawl-job/*");
        final CrawlJobResource resource;
        try {
            resource = new CrawlJobResource(
                    new HeritrixClient("https://localhost:8443/", 8443, "admin", "admin"),
                    configuration.getEmailServer()
            );
            environment.jersey().register(resource);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}