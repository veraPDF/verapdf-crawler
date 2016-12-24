package org.verapdf.crawler.app;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class Sample extends Application<Configuration> {
    public static void main(String[] args) throws Exception {
        new Sample().run(args);
    }

    @Override
    public String getName() {
        return "sample";
    }

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/html", "/", "sample.html"));
    }

    @Override
    public void run(Configuration configuration,
                    Environment environment) {
        environment.jersey().setUrlPattern("/crawl/*");
    }
}