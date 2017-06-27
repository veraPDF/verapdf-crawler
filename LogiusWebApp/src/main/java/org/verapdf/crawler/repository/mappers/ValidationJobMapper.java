package org.verapdf.crawler.repository.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.verapdf.crawler.domain.validation.ValidationJobData;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ValidationJobMapper implements RowMapper<ValidationJobData> {
    @Override
    public ValidationJobData mapRow(ResultSet resultSet, int i) throws SQLException {
        ValidationJobData data = new ValidationJobData();
        data.setFilepath(resultSet.getString("filepath"));
        data.setJobDirectory(resultSet.getString("job_directory"));
        data.setUri(resultSet.getString("file_url"));
        data.setTime(resultSet.getString("time_last_modified"));
        return data;
    }
}
