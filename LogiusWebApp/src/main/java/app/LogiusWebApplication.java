package app;

import engine.HeritrixClient;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import resources.CrawlJobResource;

public class LogiusWebApplication extends Application<Configuration> {
    public static void main(String[] args) throws Exception {
        new LogiusWebApplication().run(args);
    }

    @Override
    public String getName() {
        return "Logius";
    }

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/html", "/", "index.html", "main"));
        bootstrap.addBundle(new AssetsBundle("/html", "/jobinfo", "info.html", "status"));
    }

    @Override
    public void run(Configuration configuration,
                    Environment environment) {
        environment.jersey().setUrlPattern("/crawl-job/*");
        final CrawlJobResource resource = new CrawlJobResource(
                new HeritrixClient("https://localhost:8444", "admin", "admin")
        );
        environment.jersey().register(resource);
    }
}