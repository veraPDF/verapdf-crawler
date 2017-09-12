package org.verapdf.crawler.domain.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidationError {
    private String specification;
    private String clause;
    private String testNumber;
    private String description;

    public ValidationError() {
    }

    public ValidationError(String description) {
        this(null, null, null, description);
    }

    public ValidationError(String specification, String clause, String testNumber, String description) {
        this.specification = specification;
        this.clause = clause;
        this.testNumber = testNumber;
        this.description = description;
    }

    @JsonIgnore
    public boolean isRuleBasedError() {
        return this.specification != null && this.clause != null && this.testNumber != null;
    }

    @JsonProperty
    public String getSpecification() {
        return specification;
    }

    @JsonProperty
    public void setSpecification(String specification) {
        this.specification = specification;
    }

    @JsonProperty
    public String getClause() {
        return clause;
    }

    @JsonProperty
    public void setClause(String clause) {
        this.clause = clause;
    }

    @JsonProperty
    public String getTestNumber() {
        return testNumber;
    }

    @JsonProperty
    public void setTestNumber(String testNumber) {
        this.testNumber = testNumber;
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
