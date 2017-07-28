package org.verapdf.crawler.repository.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class CrawlJobMapper implements RowMapper<CurrentJob> {
    @Override
    public CurrentJob mapRow(ResultSet resultSet, int i) throws SQLException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        CurrentJob result = new CurrentJob(resultSet.getString(CrawlJobDao.FIELD_ID),
                                        resultSet.getString(CrawlJobDao.FIELD_JOB_URL),
                                        resultSet.getString(CrawlJobDao.FIELD_CRAWL_URL),
                                        LocalDateTime.of(LocalDate.parse(resultSet.getString(CrawlJobDao.FIELD_CRAWL_SINCE),formatter), LocalTime.MIN),
                                        "",
                                        LocalDateTime.parse(resultSet.getString(CrawlJobDao.FIELD_START_TIME), formatter));
        if(resultSet.getBoolean(CrawlJobDao.FIELD_IS_FINISHED)) {
            result.setFinished(true);
        }
        result.setStatus(resultSet.getString(CrawlJobDao.FIELD_STATUS));
        if(resultSet.getString(CrawlJobDao.FIELD_FINISH_TIME) != null && !resultSet.getString(CrawlJobDao.FIELD_FINISH_TIME).isEmpty()) {
            result.setFinishTime(LocalDateTime.parse(resultSet.getString(CrawlJobDao.FIELD_FINISH_TIME), formatter));
        }
        result.setReportEmail(resultSet.getString(CrawlJobDao.FIELD_REPORT_EMAIL));
        return result;
    }
}
