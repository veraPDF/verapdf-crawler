package org.verapdf.crawler.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Maksim Bezrukov
 */
public class DateParam {

	private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

	private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

	private Date date;

	public DateParam(String dateString) {
		try {
			this.date = df.parse(dateString);
		} catch (ParseException e) {
			logger.error("Error in parsing date string " + dateString, e);
		}
	}

	public Date getDate() {
		return date;
	}

	public static Date getDateFromParam(DateParam date) {
		return date == null ? null : date.getDate();
	}
}
