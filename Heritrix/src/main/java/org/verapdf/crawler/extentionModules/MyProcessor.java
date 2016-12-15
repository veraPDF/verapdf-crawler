package org.verapdf.crawler.extentionModules;

import org.archive.modules.CrawlURI;
import org.archive.modules.writer.MirrorWriterProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MyProcessor extends MirrorWriterProcessor {
    @Override
    protected void innerProcess(CrawlURI curi) {
        super.innerProcess(curi);
        String baseDir = getPath().getFile().getAbsolutePath();
        String mps = (String)curi.getData().get(A_MIRROR_PATH);
        try {
            FileWriter fw = new FileWriter(baseDir + File.separator + "PDFReport.txt", true);
            fw.write(curi.getURI() + "\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}