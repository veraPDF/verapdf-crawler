package org.verapdf.crawler.logius.tools;

public class DomainUtils {

    public static String trimUrl(String url) {
//        Pattern pattern = Pattern.compile("[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})");
//        Matcher matcher = pattern.matcher(url);
//        if (matcher.find()){
//            return matcher.group(0);
//        }
//        return null;
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
