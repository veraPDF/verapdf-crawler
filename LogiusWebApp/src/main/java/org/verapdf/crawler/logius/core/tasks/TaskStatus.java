package org.verapdf.crawler.logius.core.tasks;

import java.time.LocalDateTime;

public class TaskStatus {
    private Throwable stopReasonException;
    private LocalDateTime lastSuccess;
    private LocalDateTime lastError;
    private boolean  isRunning;


    public boolean isHasError(){
        if (lastError == null){
            return false;
        }
        return lastSuccess == null || lastError.isAfter(lastSuccess);
    }

    public void processSuccess() {
        lastSuccess = LocalDateTime.now();
        isRunning = false;
    }

    public void processError(Throwable e) {
        stopReasonException = e;
        lastError = LocalDateTime.now();
        isRunning = false;
    }

    public LocalDateTime getLastSuccess() {
        return lastSuccess;
    }

    public LocalDateTime getLastError() {
        return lastError;
    }

    public Throwable getStopReasonException() {
        return stopReasonException;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void processStarted() {
        this.isRunning = true;
    }
}
