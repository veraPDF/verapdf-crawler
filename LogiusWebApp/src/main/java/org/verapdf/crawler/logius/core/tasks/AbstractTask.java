package org.verapdf.crawler.logius.core.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.verapdf.crawler.logius.core.email.SendEmailService;

/**
 * @author Maksim Bezrukov
 */

public abstract class AbstractTask implements Runnable, BeanNameAware {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTask.class);

    private static final String EMAIL_SUBJECT = "Logius health checks fails";
    private static final String EMAIL_BODY = "Service %s stopped, stop reason: %s";

    private final long sleepTime;
    private final SendEmailService sendEmailService;
    private TaskStatus taskStatus;
    private String serviceName;
    private boolean isNotificationRequired;

    protected AbstractTask(long sleepTime, SendEmailService sendEmailService) {
        this.sendEmailService = sendEmailService;
        this.sleepTime = sleepTime;
        this.taskStatus = new TaskStatus();
        this.isNotificationRequired = true;
    }

    @Override
    public void run() {
        logger.info(serviceName + " started");
        taskStatus.processStarted();
        try {
            process();
            taskStatus.processSuccess();
            logger.info(serviceName + " processed successfully");
            isNotificationRequired = true;
        } catch (Throwable e) {
            logger.error("Fatal error, stopping " + serviceName, e);
            taskStatus.processError(e);
            if (isNotificationRequired) {
                sendEmailService.sendReportNotification(EMAIL_SUBJECT, String.format(EMAIL_BODY, serviceName, taskStatus.getLastException()));
                isNotificationRequired = false;
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

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public void setBeanName(String name) {
        this.serviceName = name;
    }
}
