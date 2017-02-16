package org.verapdf.crawler.extentionModules;

import org.apache.commons.httpclient.Header;
import org.archive.modules.CrawlURI;
import org.archive.modules.writer.MirrorWriterProcessor;
import org.verapdf.crawler.helpers.synchronization.FileAccessManager;

import java.io.*;

public class PDFProcessor extends MirrorWriterProcessor {
    @Override
    protected void innerProcess(CrawlURI curi) {
        super.innerProcess(curi);
        String baseDir = getPath().getFile().getAbsolutePath();
        String mps = (String)curi.getData().get(A_MIRROR_PATH);
        String time = "Last-Modified: Thu, 01 Jan 1970 00:00:01 GMT";
        Header header = curi.getHttpMethod().getResponseHeader("Last-Modified");
        if(header != null) {
            time = header.toString().substring(0, header.toString().length() - 2);
        }

        try {
            String data = "{\"filepath\":\"" + baseDir + File.separator + mps +
                    "\", \"jobDirectory\":\"" + baseDir + "\", \"" +
                    "time\":\"" + time + "\", \"uri\":\"" + curi.getURI() + "\"}";

            FileAccessManager.getInstance().makeRecord(baseDir + "/../../../../validation/validation-jobs.txt", data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}