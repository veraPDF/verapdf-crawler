package org.verapdf.crawler.resources;

import io.dropwizard.setup.Environment;
import org.verapdf.crawler.app.LogiusWebApplication;
import org.verapdf.crawler.configuration.LogiusConfiguration;
import org.verapdf.crawler.engine.HeritrixClient;

import java.io.File;

public class LogiusWebApplicationStub extends LogiusWebApplication{
    public String getBaseDirectory() {
        return  new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getParent();
    }

    @Override
    public void run(LogiusConfiguration configuration,
                    Environment environment) {
        environment.jersey().setUrlPattern("/crawl-job/*");
        final CrawlJobResourceStub resource;
        try {
            HeritrixClient client = new HeritrixClient("https://localhost/", "admin", "logius");
            client.setBaseDirectory(new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getParent() + "/src/test/resources/");
            resource = new CrawlJobResourceStub(
                    client,
                    configuration.getEmailServer()
            );
            environment.jersey().register(resource);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
