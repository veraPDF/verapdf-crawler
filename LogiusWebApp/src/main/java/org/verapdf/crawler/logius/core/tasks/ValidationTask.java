package org.verapdf.crawler.logius.core.tasks;

import org.springframework.stereotype.Component;
import org.verapdf.crawler.logius.core.email.SendEmail;
import org.verapdf.crawler.logius.core.validation.ValidationDeadlockException;
import org.verapdf.crawler.logius.service.ValidatorService;

@Component
public class ValidationTask extends AbstractTask {
    private static final long SLEEP_DURATION = 60 * 1000;

    private final ValidatorService validatorService;

    public ValidationTask(SendEmail sendEmail, ValidatorService validatorService) {
        super("ValidationService", SLEEP_DURATION, sendEmail);
        this.validatorService = validatorService;
    }


    @Override
    protected void onStart() {
    }

    @Override
    protected boolean onRepeat() throws ValidationDeadlockException, InterruptedException {
        return validatorService.processNextJob();
    }

}