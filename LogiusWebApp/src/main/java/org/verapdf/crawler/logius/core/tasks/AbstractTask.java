package org.verapdf.crawler.logius.core.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.logius.core.email.SendEmailService;

/**
 * @author Maksim Bezrukov
 */

public abstract class AbstractTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTask.class);

    private static final String EMAIL_SUBJECT = "Logius health checks fails";
    private static final String EMAIL_BODY = "Service %s stopped, stop reason: %s";

    private final long sleepTime;
    private final SendEmailService sendEmailService;
    private TaskStatus taskStatus;

    protected AbstractTask(String serviceName, long sleepTime, SendEmailService sendEmailService) {
        this.sendEmailService = sendEmailService;
        this.sleepTime = sleepTime;
        this.taskStatus = new TaskStatus(serviceName);
    }

    @Override
    public void run() {
        logger.info(taskStatus.getServiceName() + " started");
        try {
            process();
            taskStatus.processSuccess();
        } catch (Throwable e) {
            logger.error("Fatal error, stopping " + taskStatus.getServiceName(), e);
            taskStatus.processError(e);
            if (!taskStatus.isErrorNotified()) {
                sendEmailService.sendReportNotification(EMAIL_SUBJECT, String.format(EMAIL_BODY, taskStatus.getServiceName() , taskStatus.getStopReason()));
                taskStatus.setErrorNotified(true);
            }
        }
    }

    protected abstract void process() throws Throwable;

    public long getSleepTime() {
        return sleepTime;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }
}
