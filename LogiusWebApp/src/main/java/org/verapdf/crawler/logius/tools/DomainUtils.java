package org.verapdf.crawler.logius.tools;

public class DomainUtils {

    public static String trimUrl(String url) {
        if (url.contains("://")) {
            url = url.substring(url.indexOf("://") + 3);
        }
        if (url.contains("/")) {
            url = url.substring(0, url.indexOf("/"));
        }
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        return url;
    }
}
