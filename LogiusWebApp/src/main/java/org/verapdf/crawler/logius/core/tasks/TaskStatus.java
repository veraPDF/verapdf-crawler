package org.verapdf.crawler.logius.core.tasks;

import java.time.LocalDateTime;

public class TaskStatus {
    private Throwable lastException;
    private LocalDateTime lastSuccessTime;
    private LocalDateTime lastErrorTime;
    private boolean isRunning;

    public boolean isLastProcessFailed(){
        if (lastErrorTime == null){
            return false;
        }
        return lastSuccessTime == null || lastErrorTime.isAfter(lastSuccessTime);
    }

    public void processSuccess() {
        isRunning = false;
	    lastSuccessTime = LocalDateTime.now();
    }

    public void processError(Throwable e) {
        isRunning = false;
        lastException = e;
	    lastErrorTime = LocalDateTime.now();
    }

    public LocalDateTime getLastSuccessTime() {
        return lastSuccessTime;
    }

    public LocalDateTime getLastErrorTime() {
        return lastErrorTime;
    }

    public Throwable getLastException() {
        return lastException;
    }

    public String getLastExceptionMessage(){
    	return lastException == null ? null : lastException.getMessage();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void processStarted() {
        this.isRunning = true;
    }
}
