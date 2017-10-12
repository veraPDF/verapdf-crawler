package org.verapdf.crawler.core.validation;

public class ValidationDeadlockException extends Exception {

    public static final String VALIDATOR_STATE_LOCKED = "locked";
    public static final String VALIDATOR_STATE_IDLE = "idle";
    public static final String VALIDATOR_STATE_UNKNOWN = "unknown";

    private String validatorState;

    public ValidationDeadlockException(String validatorState) {
        super("Validation service is locked, validator state: " + validatorState);
    }
}
