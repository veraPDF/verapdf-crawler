package com.verapdf.crawler.logius.app.resources;

import com.verapdf.crawler.logius.app.core.heritrix.HeritrixClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.verapdf.crawler.logius.app.crawling.HeritrixSettings;


@RestController
@RequestMapping("/heritrix")
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
