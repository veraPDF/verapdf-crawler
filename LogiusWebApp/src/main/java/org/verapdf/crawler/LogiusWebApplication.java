package org.verapdf.crawler;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.ScanningHibernateBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.core.heritrix.HeritrixClient;
import org.verapdf.crawler.health.HeritrixHealthCheck;
import org.verapdf.crawler.health.VeraPDFServiceHealthCheck;

public class LogiusWebApplication extends Application<LogiusConfiguration> {
    private static final Logger logger = LoggerFactory.getLogger(LogiusWebApplication.class);

    private final HibernateBundle<LogiusConfiguration> hibernate = new ScanningHibernateBundle<LogiusConfiguration>(
            "org.verapdf.crawler.api"
    ) {
        @Override
        public DataSourceFactory getDataSourceFactory(LogiusConfiguration configuration) {
            return configuration.getDataSourceFactory();
        }
    };

    public static void main(String[] args) throws Exception {
        new LogiusWebApplication().run(args);
    }

    @Override
    public String getName() {
        return "Logius";
    }

    @Override
    public void initialize(Bootstrap<LogiusConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/html", "/", "index.html"));
        bootstrap.addBundle(hibernate);
    }

    @Override
    public void run(LogiusConfiguration configuration, Environment environment) {
        environment.jersey().setUrlPattern("/api/*");
        environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            HeritrixClient client = new HeritrixClient(configuration.getHeritrixConfiguration());
            ResourceManager resourceManager = new ResourceManager(configuration, client, hibernate);
            for (Object resource : resourceManager.getResources()) {
                environment.jersey().register(resource);
            }
            environment.healthChecks().register("heritrix", new HeritrixHealthCheck(client));
            environment.healthChecks().register("verapdf",
                    new VeraPDFServiceHealthCheck(configuration.getVeraPDFServiceConfiguration()));
            logger.info("Logius web application started");
        } catch (Exception e) {
            logger.error("Error on logius web application startup", e);
            e.printStackTrace();
        }
    }
}