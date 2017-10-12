package org.verapdf.crawler.resources;

import org.verapdf.crawler.api.crawling.HeritrixSettings;
import org.verapdf.crawler.core.heritrix.HeritrixClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/heritrix")
public class HeritrixResource {

    private final HeritrixClient heritrix;

    public HeritrixResource(HeritrixClient heritrix) {
        this.heritrix = heritrix;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public HeritrixSettings getHeritrixSettings() {
        return new HeritrixSettings(heritrix.getEngineUrl());
    }
}
