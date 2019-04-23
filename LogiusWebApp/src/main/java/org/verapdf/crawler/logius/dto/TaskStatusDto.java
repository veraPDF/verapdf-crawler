package org.verapdf.crawler.logius.dto;

import org.verapdf.crawler.logius.core.tasks.TaskStatus;

import java.time.LocalDateTime;

public class TaskStatusDto {
    private String stopReasonMessage;
    private LocalDateTime lastSuccessTime;
    private LocalDateTime lastErrorTime;
    private boolean isLastProcessFailed;
    private boolean isRunning;

    public TaskStatusDto(TaskStatus taskStatus) {
        this.stopReasonMessage = taskStatus.getLastExceptionMessage();
        this.lastSuccessTime = taskStatus.getLastSuccessTime();
        this.lastErrorTime = taskStatus.getLastErrorTime();
        this.isLastProcessFailed = taskStatus.isLastProcessFailed();
        this.isRunning = taskStatus.isRunning();
    }

    public String getStopReasonMessage() {
        return stopReasonMessage;
    }

    public void setStopReasonMessage(String stopReasonMessage) {
        this.stopReasonMessage = stopReasonMessage;
    }

    public LocalDateTime getLastSuccessTime() {
        return lastSuccessTime;
    }

    public void setLastSuccessTime(LocalDateTime lastSuccessTime) {
        this.lastSuccessTime = lastSuccessTime;
    }

    public LocalDateTime getLastErrorTime() {
        return lastErrorTime;
    }

    public void setLastErrorTime(LocalDateTime lastErrorTime) {
        this.lastErrorTime = lastErrorTime;
    }

    public boolean isLastProcessFailed() {
        return isLastProcessFailed;
    }

    public void setLastProcessFailed(boolean lastProcessFailed) {
        isLastProcessFailed = lastProcessFailed;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }
}
