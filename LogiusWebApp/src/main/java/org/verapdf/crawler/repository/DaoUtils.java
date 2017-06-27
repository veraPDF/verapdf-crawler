package org.verapdf.crawler.repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DaoUtils {
    public static String getSqlTimeFromLastmodified(String lastModified) {
        lastModified = lastModified.substring(15);
        DateTimeFormatter lastModifiedFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        LocalDateTime time = LocalDateTime.parse(lastModified, lastModifiedFormatter);
        DateTimeFormatter sqlFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return time.format(sqlFormatter);
    }
}
