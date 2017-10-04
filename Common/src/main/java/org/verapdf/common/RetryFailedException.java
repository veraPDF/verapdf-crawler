package org.verapdf.common;

import org.apache.http.client.ClientProtocolException;

import java.io.IOException;

public class RetryFailedException extends ClientProtocolException {

    private int attempts;

    private long timeSpent;

    public RetryFailedException(IOException cause, int attempts, long timeSpent) {
        super(cause);
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
