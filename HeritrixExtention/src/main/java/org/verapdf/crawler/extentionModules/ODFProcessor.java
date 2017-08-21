package org.verapdf.crawler.extentionModules;

import org.apache.commons.httpclient.Header;
import org.archive.modules.CrawlURI;
import org.archive.modules.writer.MirrorWriterProcessor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

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

    public String getLogiusUrl() {
        return logiusUrl;
    }

    public void setLogiusUrl(String logiusUrl) {
        this.logiusUrl = logiusUrl;
    }

    private String logiusUrl;

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

        String baseDir = getPath().getFile().getAbsolutePath();
        String time = "Last-Modified: Thu, 01 Jan 1970 00:00:01 GMT";
        Header header = crawlURI.getHttpMethod().getResponseHeader("Last-Modified");
        if(header != null) {
            time = header.toString().substring(0, header.toString().length() - 2);
        }

        String[] parts = baseDir.split("/");
        String jobId = parts[parts.length - 3];

        try {
            URL url = new URL(logiusUrl + "api/office_document");
            String data = "{\"jobId\":\"" + jobId + "\", \"fileUrl\":\"" +
                    crawlURI.toString() + "\", \"" + "lastModified\":\"" + time + "\"}";
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type","application/json");
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();
            int code = connection.getResponseCode();
            if(code != 204) {
                System.out.println("Response code from logius: " + code);
            }
        } catch (IOException e) {
            System.out.println("ODF processor error");
            e.printStackTrace();
        }
    }

}
