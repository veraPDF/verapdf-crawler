package org.verapdf.crawler.logius.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VeraPDFServiceStatus {
    private final ProcessorStatus processorStatus;
    private final VeraPDFValidationResult validationResult;
    @JsonCreator
    public VeraPDFServiceStatus(@JsonProperty("processorStatus") ProcessorStatus processorStatus,
                                @JsonProperty("validationResult") VeraPDFValidationResult validationResult) {
        this.processorStatus = processorStatus;
        this.validationResult = validationResult;
    }

    public ProcessorStatus getProcessorStatus() {
        return processorStatus;
    }

    public VeraPDFValidationResult getValidationResult() {
        return validationResult;
    }

    public enum ProcessorStatus {
        IDLE,
        ACTIVE,
        FINISHED,
        ABORTED
    }
}
