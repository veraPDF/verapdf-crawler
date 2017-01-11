package org.verapdf.crawler.extentionModules;

import org.archive.modules.CrawlURI;
import org.archive.modules.writer.MirrorWriterProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ODFProcessor extends MirrorWriterProcessor {
    private static Integer count;

    public ODFProcessor() {
        super();
        count = 0;
    }

    @Override
    protected boolean shouldProcess(CrawlURI crawlURI) {
        return crawlURI.toString().endsWith(".odt") ||
                crawlURI.toString().endsWith(".odc") ||
                crawlURI.toString().endsWith(".odp");
    }

    @Override
    protected void innerProcess(CrawlURI crawlURI) {
        count++;
        try {
            File baseDir = getPath().getFile();
            if(!baseDir.exists()) {
                baseDir.mkdir();
            }
            FileWriter fw = new FileWriter(baseDir.getAbsolutePath() + "/ODFReport.txt");
            fw.write(count.toString());
            fw.write(System.lineSeparator());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
