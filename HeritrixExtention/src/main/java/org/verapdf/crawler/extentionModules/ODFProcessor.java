package org.verapdf.crawler.extentionModules;

import org.archive.modules.CrawlURI;
import org.archive.modules.writer.MirrorWriterProcessor;

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
                fw.write(crawlURI.toString());
            }
            else {
                fw = new FileWriter(baseDir.getAbsolutePath() + "/OfficeReport.txt", true);
                fw.write(crawlURI.toString());
            }
            fw.write(System.lineSeparator());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
