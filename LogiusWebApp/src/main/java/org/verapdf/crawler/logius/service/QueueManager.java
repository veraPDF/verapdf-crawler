package org.verapdf.crawler.logius.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.validation.ValidationJob;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class QueueManager {
    private final static int THREAD_COUNT = 4;
    private final static long initDelay = 1000 * 60;
    private final Set<ValidatorTask> jobQueue = new LinkedHashSet<>();
    private ThreadPoolTaskExecutor service;
    private ValidationJobService validationJobService;
    private ObjectFactory<ValidatorTask> validatorTaskObjectFactory;

    public QueueManager(ValidationJobService validationJobService, ObjectFactory<ValidatorTask> validatorTaskObjectFactory) {
        this.validationJobService = validationJobService;
        this.validatorTaskObjectFactory = validatorTaskObjectFactory;
    }

    @PostConstruct
    public void init() {
        validationJobService.clean();
        service = new ThreadPoolTaskExecutor();
        service.setCorePoolSize(THREAD_COUNT);
        service.initialize();
    }

    public void process(ValidatorTask current) {
        if (current != null && service.getActiveCount() < THREAD_COUNT) {
            service.submitListenable(current).completable().thenAccept(result -> {
                synchronized (jobQueue) {
                    if (!current.getValidationJob().getStatus().equals(ValidationJob.Status.ABORTED)) {
                        validationJobService.saveResult(result, current.getValidationJob());
                    }
                    jobQueue.remove(current);
                    retirieveNextJob();
                    process(next());
                }
            });
        }
    }

    public boolean retirieveNextJob() {
        synchronized (jobQueue) {
            if (jobQueue.size() < THREAD_COUNT) {
                ValidationJob job = validationJobService.retrieveNextJob();
                if (job != null) {
                    ValidatorTask task = validatorTaskObjectFactory.getObject();
                    task.setValidationJob(job);
                    jobQueue.add(task);
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    private ValidatorTask next() {
        synchronized (jobQueue) {
            Iterator<ValidatorTask> iterator = jobQueue.iterator();
            if (!iterator.hasNext()) {
                return null;
            }
            return iterator.next();
        }
    }

    public void abortTasks(CrawlJob crawlJob) {
        synchronized (jobQueue) {
            jobQueue.stream()
                    .filter(task -> task.getValidationJob().getDocumentId().getCrawlJob().getId().equals(crawlJob.getId()))
                    .forEach(ValidatorTask::abortCurrentJob);
        }
    }

    @Scheduled(fixedDelay = initDelay)
    public void initValidationQueue() {
        while (retirieveNextJob()) {
            process(next());
        }
    }
}