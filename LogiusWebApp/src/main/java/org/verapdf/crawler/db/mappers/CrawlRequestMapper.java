package org.verapdf.crawler.db.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.verapdf.crawler.api.crawling.CrawlRequest;
import org.verapdf.crawler.db.jobs.CrawlRequestDao;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CrawlRequestMapper implements RowMapper<CrawlRequest> {

    @Override
    public CrawlRequest mapRow(ResultSet resultSet, int i) throws SQLException {
        CrawlRequest result = new CrawlRequest(resultSet.getString(CrawlRequestDao.FIELD_ID),
                                resultSet.getString(CrawlRequestDao.FIELD_REPORT_EMAIL),
                resultSet.getDate(CrawlRequestDao.FIELD_CRAWL_SINCE));
        result.setFinished(resultSet.getBoolean(CrawlRequestDao.FIELD_IS_FINISHED));
        return result;
    }
}
