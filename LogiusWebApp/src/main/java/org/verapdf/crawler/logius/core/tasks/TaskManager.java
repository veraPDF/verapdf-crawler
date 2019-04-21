package org.verapdf.crawler.logius.core.tasks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class TaskManager {
    private final HeritrixCleanerTask heritrixCleanerTask;
    private final MonitorCrawlJobStatusTask monitorCrawlJobStatusTask;
    private final ODSCleanerTask odsCleanerTask;
    private final HeritrixTotalDownloadCountTask heritrixTotalDownloadCountTask;
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;
    @Value("${logius.validationJobQueue.threadCount}")
    private int threadCount;

    public TaskManager(HeritrixCleanerTask heritrixCleanerTask,
                       MonitorCrawlJobStatusTask monitorCrawlJobStatusTask,
                       ODSCleanerTask odsCleanerTask,
                       HeritrixTotalDownloadCountTask heritrixTotalDownloadCountTask) {
        this.heritrixCleanerTask = heritrixCleanerTask;
        this.monitorCrawlJobStatusTask = monitorCrawlJobStatusTask;
        this.odsCleanerTask = odsCleanerTask;
        this.heritrixTotalDownloadCountTask = heritrixTotalDownloadCountTask;
    }

    @PostConstruct
    public void init() {
        threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(threadCount);
        threadPoolTaskScheduler.initialize();
        threadPoolTaskScheduler.scheduleAtFixedRate(heritrixCleanerTask, heritrixCleanerTask.getSleepTime());
        threadPoolTaskScheduler.scheduleAtFixedRate(monitorCrawlJobStatusTask, monitorCrawlJobStatusTask.getSleepTime());
        threadPoolTaskScheduler.scheduleAtFixedRate(odsCleanerTask, odsCleanerTask.getSleepTime());
        threadPoolTaskScheduler.scheduleAtFixedRate(heritrixTotalDownloadCountTask, heritrixTotalDownloadCountTask.getSleepTime());
    }
}
