package org.verapdf.crawler.logius.core.tasks;

import java.time.LocalDateTime;

public class TaskStatus {
    private final String serviceName;
    private String stopReason;
    private LocalDateTime lastSuccess;
    private LocalDateTime lastError;
    private boolean isErrorNotified = false;

    public TaskStatus(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void processSuccess() {
        lastSuccess = LocalDateTime.now();
        isErrorNotified = false;
    }

    public void processError(Throwable e) {
        stopReason = e.getMessage();
        lastError = LocalDateTime.now();
    }

    public boolean isErrorNotified() {
        return isErrorNotified;
    }

    public void setErrorNotified(boolean errorNotified) {
        this.isErrorNotified = errorNotified;
    }

    public String getStopReason() {
        return stopReason;
    }

    public LocalDateTime getLastSuccess() {
        return lastSuccess;
    }

    public LocalDateTime getLastError() {
        return lastError;
    }
}
