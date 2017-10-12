package org.verapdf.crawler.api.validation.error;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Rule {
    @Column(name = "specification")
    @JsonProperty
    private String specification;

    @Column(name = "clause")
    @JsonProperty
    private String clause;

    @Column(name = "test_number")
    @JsonProperty
    private String testNumber;

    public Rule() {
    }

    public Rule(String specification, String clause, String testNumber) {
        this.specification = specification;
        this.clause = clause;
        this.testNumber = testNumber;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getClause() {
        return clause;
    }

    public void setClause(String clause) {
        this.clause = clause;
    }

    public String getTestNumber() {
        return testNumber;
    }

    public void setTestNumber(String testNumber) {
        this.testNumber = testNumber;
    }
}
