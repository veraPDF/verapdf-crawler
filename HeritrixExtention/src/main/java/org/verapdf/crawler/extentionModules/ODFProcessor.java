package org.verapdf.crawler.extentionModules;

import org.apache.commons.httpclient.Header;
import org.archive.modules.CrawlURI;
import org.archive.modules.writer.MirrorWriterProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ODFProcessor extends MirrorWriterProcessor {
    private static final String[] supportedExtentions = {".odt",
                                                        ".odc",
                                                        ".odp",
                                                        ".doc",
                                                        ".docx",
                                                        ".ppt",
                                                        ".pptx",
                                                        ".xls",
                                                        "xlsx"};

    private static Logger logger = LoggerFactory.getLogger(ODFProcessor.class);
    @Override
    protected boolean shouldProcess(CrawlURI crawlURI) {
        boolean check = false;
        for(String extention : supportedExtentions) {
            check = check || crawlURI.toString().endsWith(extention);
        }
        return check;
    }

    @Override
    protected void innerProcess(CrawlURI crawlURI) {
        String time = "Last-Modified: Thu, 01 Jan 1970 00:00:01 GMT";
        Header header = crawlURI.getHttpMethod().getResponseHeader("Last-Modified");
        if(header != null) {
            time = header.toString();
        }

        boolean isODFFile = crawlURI.toString().endsWith(".odt") ||
                crawlURI.toString().endsWith(".odc") ||
                crawlURI.toString().endsWith(".odp");
        try {
            File baseDir = getPath().getFile();
            if(!baseDir.exists()) {
                baseDir.mkdir();
            }
            FileWriter fw;
            if(isODFFile) {
                fw = new FileWriter(baseDir.getAbsolutePath() + "/ODFReport.txt", true);
                fw.write(crawlURI.toString() + ", ");
                fw.write(time);
                fw.close();
            }
            else {
                fw = new FileWriter(baseDir.getAbsolutePath() + "/OfficeReport.txt", true);
                fw.write(crawlURI.toString() + ", ");
                fw.write(time);
                fw.close();
            }
        } catch (IOException e) {
            logger.error("ODF processor error", e);
        }
    }

}
