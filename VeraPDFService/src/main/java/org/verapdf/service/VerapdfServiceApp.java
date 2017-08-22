package org.verapdf.service;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider;

public class VerapdfServiceApp extends Application<Configuration> {

    public static void main(String[] args) throws Exception {
        VeraGreenfieldFoundryProvider.initialise();
        new VerapdfServiceApp().run(args);
    }

    @Override
    public String getName() {
        return "VeraPDF Service";
    }

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) { }

    @Override
    public void run(Configuration configuration, Environment environment) {
        environment.jersey().setUrlPattern("/*");
        environment.jersey().register(new ValidationResource());
    }
}
