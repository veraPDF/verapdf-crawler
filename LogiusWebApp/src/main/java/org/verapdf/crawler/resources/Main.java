package org.verapdf.crawler.resources;

import org.verapdf.crawler.report.HeritrixReporter;

public class Main {
    public static void main(String[] args) {
        String report = "";
        System.out.println(HeritrixReporter.removeEarlyLines(report, null));
    }
}
