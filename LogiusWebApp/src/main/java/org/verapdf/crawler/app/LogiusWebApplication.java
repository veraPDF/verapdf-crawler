package org.verapdf.crawler.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.app.configuration.LogiusConfiguration;
import org.verapdf.crawler.app.engine.HeritrixClient;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.verapdf.crawler.app.resources.ResourceManager;

public class LogiusWebApplication extends Application<LogiusConfiguration> {
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
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
        environment.jersey().setUrlPattern("/api/*");
        final ResourceManager resourceManager;
        try {
            HeritrixClient client = new HeritrixClient("https://localhost:8443/",
                    configuration.getHeritrixLogin(),
                    configuration.getHeritrixPassword());
            client.setBaseDirectory(configuration.getResourcePath());
            System.out.println("Heritrix client created");
            resourceManager = new ResourceManager( client,
                    configuration.getEmailServer(),
                    configuration.getVerapdfPath(),
                    configuration.getCredentials());
            System.out.println("Resource manager client created");
            environment.jersey().register(resourceManager.getInfoResourse());
            System.out.println("Info resource registered");
            environment.jersey().register(resourceManager.getReportResource());
            System.out.println("Report resource registered");
            environment.jersey().register(resourceManager.getControlResource());
            System.out.println("Control resource registered");
        } catch (Exception e) {
            logger.error("Error on logius web application startup", e);
            e.printStackTrace();
        }
    }
}