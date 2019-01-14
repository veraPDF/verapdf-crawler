package com.verapdf.common;

import org.apache.http.HttpRequest;

import java.io.IOException;

public class RetryFailedException extends RuntimeException {

    private int attempts;

    private long timeSpent;

    public RetryFailedException(HttpRequest request, IOException cause, int attempts, long timeSpent) {
        super("Fail to execute request " + request + " with " + attempts + " attempts in " + timeSpent + "ms", cause);
        this.attempts = attempts;
        this.timeSpent = timeSpent;
    }

    public int getAttempts() {
        return attempts;
    }

    public long getTimeSpent() {
        return timeSpent;
    }
}
