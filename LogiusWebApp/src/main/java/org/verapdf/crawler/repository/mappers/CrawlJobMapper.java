package org.verapdf.crawler.repository.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.verapdf.crawler.domain.crawling.CurrentJob;

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
        CurrentJob result = new CurrentJob(resultSet.getString("id"),
                                        resultSet.getString("job_url"),
                                        resultSet.getString("crawl_url"),
                                        LocalDateTime.of(LocalDate.parse(resultSet.getString("crawl_since"),formatter), LocalTime.MIN),
                                        "",
                                        LocalDateTime.parse(resultSet.getString("start_time"), formatter));
        if(resultSet.getBoolean("is_finished")) {
            result.setFinished(true);
        }
        result.setStatus(resultSet.getString("status"));
        if(resultSet.getString("finish_time") != null && !resultSet.getString("finish_time").isEmpty()) {
            result.setFinishTime(LocalDateTime.parse(resultSet.getString("finish_time"), formatter));
        }
        return result;
    }
}
