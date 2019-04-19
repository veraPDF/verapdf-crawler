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
    private HeritrixCleanerTask heritrixCleanerTask;
    private MonitorCrawlJobStatusTask monitorCrawlJobStatusTask;

    public AbstractServiceConfiguration(ODSCleanerTask odsCleanerTask,
                                        HeritrixCleanerTask heritrixCleanerTask,
                                        MonitorCrawlJobStatusTask monitorCrawlJobStatusTask) {
        this.odsCleanerTask = odsCleanerTask;
        this.heritrixCleanerTask = heritrixCleanerTask;
        this.monitorCrawlJobStatusTask = monitorCrawlJobStatusTask;
    }

    @Bean
    public Map<String, AbstractTask> availableServices() {
        Map<String, AbstractTask> availableServices = new HashMap<>();
        availableServices.put(odsCleanerTask.getServiceName(), odsCleanerTask);
        availableServices.put(heritrixCleanerTask.getServiceName(), heritrixCleanerTask);
        availableServices.put(monitorCrawlJobStatusTask.getServiceName(), monitorCrawlJobStatusTask);
        return availableServices;
    }
}
