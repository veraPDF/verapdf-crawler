package org.verapdf.crawler.domain.validation;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidationErrorWithdescription extends ValidationError {
    private String description;

    public ValidationErrorWithdescription(String specification, String clause, String testNumber, String description) {
        super(specification, clause, testNumber);
        this.description = description;
    }

    @JsonProperty
    public String getDescription() {
        return description;
    }

    @JsonProperty
    public void setDescription(String description) {
        this.description = description;
    }
}
