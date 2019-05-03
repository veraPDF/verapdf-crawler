package org.verapdf.crawler.logius.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.validation.ValidationJob;

import javax.annotation.PostConstruct;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class QueueManager {
    @Value("${logius.validationJobQueue.threadCount}")
    private int threadCount;
    private final Set<ValidatorTask> jobQueue = new LinkedHashSet<>();
    private ThreadPoolTaskExecutor service;
    private ValidationJobService validationJobService;
    private ObjectFactory<ValidatorTask> validatorTaskObjectFactory;
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    public QueueManager(ValidationJobService validationJobService, ObjectFactory<ValidatorTask> validatorTaskObjectFactory) {
        this.validationJobService = validationJobService;
        this.validatorTaskObjectFactory = validatorTaskObjectFactory;
    }

    @PostConstruct
    public void init() {
        validationJobService.clean();
        service = new ThreadPoolTaskExecutor();
        service.setCorePoolSize(threadCount);
        service.initialize();
    }

    public void process(ValidatorTask current) {
        if (current != null) {
            service.submitListenable(current)
                   .completable().thenAccept(result -> {
                synchronized (jobQueue) {
	                try {
		                if (ValidationJob.Status.ABORTED != current.getValidationJob().getStatus()) {
			                validationJobService.saveResult(result, current.getValidationJob());
		                }
	                } finally {
		                jobQueue.remove(current);
		                logger.info("current task with id " + current.getValidationJob().getDocument().getDocumentId() + " cleaned, queue size: " + jobQueue.size());
		                process(retrieveNextJob());
	                }
                }
            });
        }
    }

    public ValidatorTask retrieveNextJob() {
        synchronized (jobQueue) {
            if (jobQueue.size() < threadCount) {
                ValidationJob job = validationJobService.retrieveNextJob();
                if (job != null) {
                    ValidatorTask task = validatorTaskObjectFactory.getObject();
                    task.setValidationJob(job);
                    jobQueue.add(task);
                    logger.info("add new task with id: " + task.getValidationJob().getDocument().getDocumentId());
                    return task;
                }
            }
            return null;
        }
    }

    public void abortTasks(CrawlJob crawlJob) {
        synchronized (jobQueue) {
            jobQueue.stream()
                    .filter(task -> task.getValidationJob().getDocumentId().getCrawlJob().getId().equals(crawlJob.getId()))
                    .forEach(ValidatorTask::abortCurrentJob);
        }
    }

    @Scheduled(fixedDelayString = "#{${logius.validationJobQueue.sleepDurationInSeconds}}")
    public void initValidationQueue() {
        while (true) {
            logger.info("count of threads: " + jobQueue.size());
            ValidatorTask task = retrieveNextJob();
            if (task == null) {
                break;
            }
            process(task);
        }
    }
}