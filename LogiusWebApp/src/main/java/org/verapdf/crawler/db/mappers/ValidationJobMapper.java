package org.verapdf.crawler.db.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.verapdf.crawler.api.validation.ValidationJobData;
import org.verapdf.crawler.db.jobs.ValidationJobDao;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ValidationJobMapper implements RowMapper<ValidationJobData> {
    @Override
    public ValidationJobData mapRow(ResultSet resultSet, int i) throws SQLException {
        ValidationJobData data = new ValidationJobData();
        data.setFilepath(resultSet.getString(ValidationJobDao.FIELD_FILEPATH));
        data.setJobDirectory(resultSet.getString(ValidationJobDao.FIELD_JOB_DIRECTORY));
        data.setUri(resultSet.getString(ValidationJobDao.FIELD_FILE_URL));
        data.setTime(resultSet.getString(ValidationJobDao.FIELD_LAST_MODIFIED));
        return data;
    }
}
