package org.verapdf.crawler.resources;

import io.dropwizard.setup.Environment;
import org.verapdf.crawler.app.LogiusWebApplication;
import org.verapdf.crawler.configuration.LogiusConfiguration;
import org.verapdf.crawler.engine.HeritrixClient;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class LogiusWebApplicationStub extends LogiusWebApplication{
    @Override
    public void run(LogiusConfiguration configuration,
                    Environment environment) {
        environment.jersey().setUrlPattern("/crawl-job/*");
        final CrawlJobResourceStub resource;
        try {
            resource = new CrawlJobResourceStub(
                    new HeritrixClient("https://localhost/", 8443, "admin", "admin"),
                    configuration.getEmailServer()
            );
            environment.jersey().register(resource);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
