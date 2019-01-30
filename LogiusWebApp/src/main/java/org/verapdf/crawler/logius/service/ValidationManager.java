package org.verapdf.crawler.logius.service;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.validation.ValidationJob;

import java.util.ArrayList;
import java.util.List;

@Service
public class ValidationManager {
    private final int MAX_SIZE = 4;
    private final ThreadPoolTaskExecutor executor;
    private final ValidationJobService validationJobService;
    private final ValidatorService validatorService;

    public ValidationManager(ValidationJobService validationJobService,
                             ValidatorService validatorService) {
        this.validationJobService = validationJobService;
        this.validatorService = validatorService;
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(MAX_SIZE);
        executor.initialize();
        validationJobService.clean();
        updateState();
    }

    private void validate(ValidationJob validationJob) {
        executor.submitListenable(() -> validatorService.processJob(validationJob))
                .completable().thenAccept(result -> updateState());
    }

    private boolean isNotFull() {
        return executor.getActiveCount() < MAX_SIZE;
    }

    public void updateState() {
        synchronized (executor) {
            while (isNotFull()) {
                ValidationJob job = validationJobService.retrieveNextJob();
                if (job == null) {
                    return;
                }
                this.validate(job);
            }
        }
    }
}
