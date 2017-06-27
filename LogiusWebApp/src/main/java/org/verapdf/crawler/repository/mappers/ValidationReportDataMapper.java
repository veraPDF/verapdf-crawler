package org.verapdf.crawler.repository.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.verapdf.crawler.domain.validation.ValidationReportData;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ValidationReportDataMapper implements RowMapper<ValidationReportData> {

    @Override
    public ValidationReportData mapRow(ResultSet resultSet, int i) throws SQLException {
        ValidationReportData report = new ValidationReportData();
        report.setPassedRules(resultSet.getInt("passed_rules"));
        report.setFailedRules(resultSet.getInt("failed_rules"));
        report.setUrl(resultSet.getString("file_url"));
        report.setLastModified(resultSet.getString("last_modified"));
        report.setValid(resultSet.getBoolean("valid"));
        return report;
    }
}
