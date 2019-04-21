package org.verapdf.crawler.logius.core.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.logius.core.email.SendEmail;

/**
 * @author Maksim Bezrukov
 */

public abstract class AbstractTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTask.class);

    private static final String EMAIL_SUBJECT = "Logius health checks fails";
    private static final String EMAIL_BODY = "Service %s stopped, stop reason: %s";

    private final String serviceName;
    private final long sleepTime;
    private final SendEmail sendEmail;
    private String stopReason;

    protected AbstractTask(String serviceName, long sleepTime, SendEmail sendEmail) {
        this.sendEmail = sendEmail;
        this.serviceName = serviceName;
        this.sleepTime = sleepTime;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getStopReason() {
        return stopReason;
    }

    @Override
    public void run() {
        logger.info(this.serviceName + " started");
        try {
            process();
        } catch (Throwable e) {
            logger.error("Fatal error, stopping " + this.serviceName, e);
            this.stopReason = e.getMessage();
            sendEmail.sendReportNotification(EMAIL_SUBJECT, String.format(EMAIL_BODY, this.serviceName, this.stopReason));
        }
    }

    protected abstract void process() throws Throwable;

    public long getSleepTime() {
        return sleepTime;
    }
}
