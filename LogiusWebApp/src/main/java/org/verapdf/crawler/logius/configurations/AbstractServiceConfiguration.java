package org.verapdf.crawler.logius.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.verapdf.crawler.logius.core.tasks.*;
import org.verapdf.crawler.logius.core.tasks.AbstractTask;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AbstractServiceConfiguration {

    private ODSCleanerTask odsCleanerTask;
    private HeritrixCleanerTask heritrixCleanerTask;
    private HeritrixTotalDownloadCountTask heritrixTotalDownloadCountTask;
    private MonitorCrawlJobStatusTask monitorCrawlJobStatusTask;

    public AbstractServiceConfiguration(ODSCleanerTask odsCleanerTask,
                                        HeritrixCleanerTask heritrixCleanerTask,
                                        HeritrixTotalDownloadCountTask heritrixTotalDownloadCountTask, MonitorCrawlJobStatusTask monitorCrawlJobStatusTask) {
        this.odsCleanerTask = odsCleanerTask;
        this.heritrixCleanerTask = heritrixCleanerTask;
        this.heritrixTotalDownloadCountTask = heritrixTotalDownloadCountTask;
        this.monitorCrawlJobStatusTask = monitorCrawlJobStatusTask;
    }

    @Bean
    public Map<String, AbstractTask> availableServices() {
        Map<String, AbstractTask> availableServices = new HashMap<>();
        availableServices.put(odsCleanerTask.getServiceName(), odsCleanerTask);
        availableServices.put(heritrixCleanerTask.getServiceName(), heritrixCleanerTask);
        availableServices.put(monitorCrawlJobStatusTask.getServiceName(), monitorCrawlJobStatusTask);
        availableServices.put(heritrixTotalDownloadCountTask.getServiceName(), heritrixTotalDownloadCountTask);
        return availableServices;
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(Map<String, AbstractTask> availableServices){
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setBeanName("taskManager");
        threadPoolTaskScheduler.setPoolSize(availableServices.size());
        threadPoolTaskScheduler.initialize();
        availableServices.values().forEach(service ->
                threadPoolTaskScheduler.scheduleAtFixedRate(service, service.getSleepTime()));
        return threadPoolTaskScheduler;
    }
}
