package org.verapdf.crawler.logius.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.verapdf.crawler.logius.core.heritrix.HeritrixClient;
import org.verapdf.crawler.logius.crawling.HeritrixSettings;


@RestController
@RequestMapping("api/heritrix")
public class HeritrixResource {

    private final HeritrixClient heritrixClient;

    @Autowired
    public HeritrixResource(HeritrixClient heritrixClient) {
        this.heritrixClient = heritrixClient;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public HeritrixSettings getHeritrixSettings() {
        return new HeritrixSettings(heritrixClient.getEngineUrl());
    }
}
