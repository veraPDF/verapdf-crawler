package org.verapdf.crawler.domain.report;

public class ValidationError {

    public static final String PART_ONE_RULE = "PDFA-Part-1-rules";
    public static final String PART_TWO_THREE_RULE = "PDFA-Parts-2-and-3-rules";

    private String clause;
    private Integer testNumber;
    private String specification;
    private String partRule;
    private String description;

    public ValidationError(String clause, Integer testNumber, String specification, String partRule, String description) {
        this.clause = clause;
        this.testNumber = testNumber;
        this.specification = specification;
        this.partRule = partRule;
        this.description = description;
    }

    @Override
    public String toString() {
        return "<p><a href =\"" + "https://github.com/veraPDF/veraPDF-validation-profiles/wiki/" + "" +
                partRule + "#rule-" +
                clause.replaceAll("\\.","") + "-" + testNumber +
                "\">Specification: " + specification +
                ", Clause: " + clause +
                ", Test number: " + testNumber + "</a></p><p>" +
                description + "</p>";
    }

    @Override
    public boolean equals(Object obj) {
        ValidationError err = (ValidationError) obj;
        return err.clause.equals(clause) && err.testNumber.equals(testNumber) && err.partRule.equals(partRule) && err.specification.equals(specification);
    }

    @Override
    public int hashCode() {
        return clause.hashCode() + testNumber.hashCode() + partRule.hashCode();
    }
}
