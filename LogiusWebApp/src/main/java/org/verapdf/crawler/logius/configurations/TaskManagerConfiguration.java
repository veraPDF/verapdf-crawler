package org.verapdf.crawler.logius.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.verapdf.crawler.logius.core.tasks.*;
import org.verapdf.crawler.logius.core.tasks.AbstractTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class TaskManagerConfiguration {

    @Bean("taskManager")
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(List<AbstractTask> availableServices){
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setBeanName("taskManager");
        threadPoolTaskScheduler.setPoolSize(availableServices.size());
        threadPoolTaskScheduler.initialize();
        availableServices.forEach(service ->
                threadPoolTaskScheduler.scheduleAtFixedRate(service, service.getSleepTime()));
        return threadPoolTaskScheduler;
    }
}
