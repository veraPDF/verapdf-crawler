package org.verapdf.crawler.logius.dto;

import org.verapdf.crawler.logius.core.tasks.TaskStatus;

import java.time.LocalDateTime;

public class TaskStatusDto {
    private String stopReasonMessage;
    private LocalDateTime lastSuccessDate;
    private LocalDateTime lastErrorDate;
    private boolean isLastError;
    private boolean isRunning;

    public TaskStatusDto(TaskStatus taskStatus) {
        this.stopReasonMessage = taskStatus.getStopReasonException() == null ? null : taskStatus
                .getStopReasonException()
                .getMessage();
        this.lastSuccessDate = taskStatus.getLastSuccess();
        this.lastErrorDate = taskStatus.getLastError();
        this.isLastError = taskStatus.isHasError();
        this.isRunning = taskStatus.isRunning();
    }

    public String getStopReasonMessage() {
        return stopReasonMessage;
    }

    public void setStopReasonMessage(String stopReasonMessage) {
        this.stopReasonMessage = stopReasonMessage;
    }

    public LocalDateTime getLastSuccessDate() {
        return lastSuccessDate;
    }

    public void setLastSuccessDate(LocalDateTime lastSuccessDate) {
        this.lastSuccessDate = lastSuccessDate;
    }

    public LocalDateTime getLastErrorDate() {
        return lastErrorDate;
    }

    public void setLastErrorDate(LocalDateTime lastErrorDate) {
        this.lastErrorDate = lastErrorDate;
    }

    public boolean isLastError() {
        return isLastError;
    }

    public void setLastError(boolean lastError) {
        isLastError = lastError;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }
}
