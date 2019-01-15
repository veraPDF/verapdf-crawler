package com.verapdf.crawler.logius.app.configurations;

import com.verapdf.crawler.logius.app.core.services.BingService;
import com.verapdf.crawler.logius.app.core.services.HeritrixCleanerService;
import com.verapdf.crawler.logius.app.core.services.MonitorCrawlJobStatusService;
import com.verapdf.crawler.logius.app.core.services.ODSCleanerService;
import com.verapdf.crawler.logius.app.core.validation.ValidationService;
import com.verapdf.crawler.logius.app.tools.AbstractService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AbstractServiceConfiguration {

    private ODSCleanerService odsCleanerService;
    private BingService bingService;
    private HeritrixCleanerService heritrixCleanerService;
    private MonitorCrawlJobStatusService monitorCrawlJobStatusService;
    private ValidationService validationService;

    public AbstractServiceConfiguration(ODSCleanerService odsCleanerService,
                                        BingService bingService,
                                        HeritrixCleanerService heritrixCleanerService,
                                        MonitorCrawlJobStatusService monitorCrawlJobStatusService,
                                        ValidationService validationService) {
        this.odsCleanerService = odsCleanerService;
        this.bingService = bingService;
        this.heritrixCleanerService = heritrixCleanerService;
        this.monitorCrawlJobStatusService = monitorCrawlJobStatusService;
        this.validationService = validationService;
    }

    @Bean
    public Map<String, AbstractService> availableServices() {
        Map<String, AbstractService> availableServices = new HashMap<>();
        availableServices.put(odsCleanerService.getServiceName(), odsCleanerService);
        availableServices.put(bingService.getServiceName(), bingService);
        availableServices.put(heritrixCleanerService.getServiceName(), heritrixCleanerService);
        availableServices.put(monitorCrawlJobStatusService.getServiceName(), monitorCrawlJobStatusService);
        availableServices.put(validationService.getServiceName(), validationService);
        return availableServices;
    }
}
