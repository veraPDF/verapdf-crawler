package org.verapdf.crawler.logius.resources;

import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.verapdf.crawler.logius.core.heritrix.HeritrixClient;
import org.verapdf.crawler.logius.core.tasks.AbstractTask;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.HeritrixSettings;
import org.verapdf.crawler.logius.db.ValidationJobDAO;
import org.verapdf.crawler.logius.monitoring.ValidationQueueStatus;
import org.verapdf.crawler.logius.service.CrawlJobService;
import org.verapdf.crawler.logius.service.ValidationJobService;
import org.verapdf.crawler.logius.validation.ValidationJob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
@RestControllerEndpoint(id = "info")
public class HealthCheckController {
    private final CrawlJobService crawlJobService;
    private final ValidationJobService validationJobService;
    private final HeritrixClient heritrix;

    public HealthCheckController(ValidationJobService validationJobService,
                                 CrawlJobService crawlJobService,
                                 HeritrixClient heritrix) {
        this.crawlJobService = crawlJobService;
        this.validationJobService = validationJobService;
        this.heritrix = heritrix;
    }

    @GetMapping("/active-jobs")
    public ResponseEntity getActiveJobs(@RequestParam(value = "domainFilter", required = false) String domainFilter,
                                        @RequestParam("start") int startParam,
                                        @RequestParam("limit") int limitParam) {
        Map<String, List<CrawlJob>> activeJobs = new HashMap<>();
        long count = crawlJobService.count(domainFilter, false);
        activeJobs.put("jobs", crawlJobService.findNotFinishedJobs(domainFilter, startParam, limitParam));
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(count)).body(activeJobs);
    }

    @GetMapping("/queue-status")
    public ValidationQueueStatus getQueueStatus() {
        return validationJobService.getValidationJobStatus(10);
    }

    @GetMapping("/heritrix")
    public HeritrixSettings getHeritrixSettings() {
        return new HeritrixSettings(heritrix.getEngineUrl());
    }
}

