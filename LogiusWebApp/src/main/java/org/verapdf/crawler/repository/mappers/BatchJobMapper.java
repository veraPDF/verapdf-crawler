package org.verapdf.crawler.repository.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.repository.jobs.BatchJobDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BatchJobMapper implements RowMapper<BatchJob> {

    @Override
    public BatchJob mapRow(ResultSet resultSet, int i) throws SQLException {
        BatchJob result = new BatchJob(resultSet.getString(BatchJobDao.FIELD_ID),
                                resultSet.getString(BatchJobDao.FIELD_REPORT_EMAIL),
                LocalDateTime.parse(resultSet.getString(BatchJobDao.FIELD_CRAWL_SINCE),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        result.setFinished(resultSet.getBoolean(BatchJobDao.FIELD_IS_FINISHED));
        return result;
    }
}
