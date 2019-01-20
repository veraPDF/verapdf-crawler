package org.verapdf.crawler.logius.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.verapdf.crawler.logius.core.tasks.*;
import org.verapdf.crawler.logius.core.tasks.AbstractTask;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AbstractServiceConfiguration {

    private ODSCleanerTask odsCleanerTask;
    private BingTask bingTask;
    private HeritrixCleanerTask heritrixCleanerTask;
    private MonitorCrawlJobStatusTask monitorCrawlJobStatusTask;
    private ValidationTask validationTask;

    public AbstractServiceConfiguration(ODSCleanerTask odsCleanerTask,
                                        BingTask bingTask,
                                        HeritrixCleanerTask heritrixCleanerTask,
                                        MonitorCrawlJobStatusTask monitorCrawlJobStatusTask,
                                        ValidationTask validationTask) {
        this.odsCleanerTask = odsCleanerTask;
        this.bingTask = bingTask;
        this.heritrixCleanerTask = heritrixCleanerTask;
        this.monitorCrawlJobStatusTask = monitorCrawlJobStatusTask;
        this.validationTask = validationTask;
    }

    @Bean
    public Map<String, AbstractTask> availableServices() {
        Map<String, AbstractTask> availableServices = new HashMap<>();
        availableServices.put(odsCleanerTask.getServiceName(), odsCleanerTask);
        availableServices.put(bingTask.getServiceName(), bingTask);
        availableServices.put(heritrixCleanerTask.getServiceName(), heritrixCleanerTask);
        availableServices.put(monitorCrawlJobStatusTask.getServiceName(), monitorCrawlJobStatusTask);
        availableServices.put(validationTask.getServiceName(), validationTask);
        return availableServices;
    }
}
