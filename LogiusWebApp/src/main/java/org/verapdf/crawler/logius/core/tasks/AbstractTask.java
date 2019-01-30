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
    private boolean running;
    private String stopReason;

    protected AbstractTask(String serviceName, long sleepTime, SendEmail sendEmail) {
        this.sendEmail = sendEmail;
        this.running = false;
        this.serviceName = serviceName;
        this.sleepTime = sleepTime;

        if (!running) {
            running = true;
            stopReason = null;
            new Thread(this, "Thread-" + this.serviceName).start();
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public boolean isRunning() {
        return running;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void start() {
        if (!running) {
            running = true;
            stopReason = null;
            new Thread(this, "Thread-" + this.serviceName).start();
        }
    }

    @Override
    public void run() {
        logger.info(this.serviceName + " started");
        try {
            onStart();
            while (running) {
                if (onRepeat()) {
                    Thread.sleep(this.sleepTime);
                }
            }
        } catch (Throwable e) {
            logger.error("Fatal error, stopping " + this.serviceName, e);
            this.stopReason = e.getMessage();

            sendEmail.sendReportNotification(EMAIL_SUBJECT, String.format(EMAIL_BODY, this.serviceName, this.stopReason));
        } finally {
            running = false;
        }
    }

    protected abstract void onStart() throws Throwable;

    /**
     * @return true if we have to sleep after action has been finished
     */
    protected abstract boolean onRepeat() throws Throwable;
}
