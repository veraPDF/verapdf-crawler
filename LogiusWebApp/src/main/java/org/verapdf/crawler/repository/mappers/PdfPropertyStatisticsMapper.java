package org.verapdf.crawler.repository.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.verapdf.crawler.domain.report.PdfPropertyStatistics;
import org.verapdf.crawler.repository.document.ValidatedPDFDao;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PdfPropertyStatisticsMapper implements RowMapper<PdfPropertyStatistics> {

    @Override
    public PdfPropertyStatistics mapRow(ResultSet resultSet, int i) throws SQLException {
        return new PdfPropertyStatistics(resultSet.getString(String.format("any_value(%s)", ValidatedPDFDao.FIELD_PDF_PROPERTY_READABLE_NAME)),
                resultSet.getString(String.format("any_value(%s.%s)", ValidatedPDFDao.PROPERTIES_TABLE_NAME, ValidatedPDFDao.FIELD_PROPERTY_VALUE)),
                resultSet.getInt("number"));
    }
}
