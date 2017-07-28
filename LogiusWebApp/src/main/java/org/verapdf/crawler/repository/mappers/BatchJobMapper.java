package org.verapdf.crawler.repository.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.repository.jobs.BatchJobDao;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BatchJobMapper implements RowMapper<BatchJob> {

    @Override
    public BatchJob mapRow(ResultSet resultSet, int i) throws SQLException {
        BatchJob result = new BatchJob(resultSet.getString(BatchJobDao.FIELD_ID),
                                resultSet.getString(BatchJobDao.FIELD_REPORT_EMAIL),
                                resultSet.getString(BatchJobDao.FIELD_CRAWL_SINCE));
        result.setFinished(resultSet.getBoolean(BatchJobDao.FIELD_IS_FINISHED));
        return result;
    }
}
