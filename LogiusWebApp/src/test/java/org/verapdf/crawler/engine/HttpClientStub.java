package org.verapdf.crawler.engine;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

public class HttpClientStub implements HttpClient{

    private HashMap<String, List<String>> crawlUrls = new HashMap<>();
    private HashMap<String,Integer> jobProgress = new HashMap<>();

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException {
        if(request instanceof HttpPost) {
            HttpPost post = (HttpPost) request;
            BufferedReader rd = new BufferedReader(new InputStreamReader(post.getEntity().getContent()));
            String data = rd.readLine();
            // Create job
            if(data.startsWith("createpath")) {
                jobProgress.put(getJobFromRequest(post),0);
                return new BasicHttpResponse(new BasicStatusLine(new HttpVersion(1,1),200,"OK"));
            }
            // Launch job
            if(data.endsWith("=launch")) {
                jobProgress.put(getJobFromRequest(post), 1);
                return new BasicHttpResponse(new BasicStatusLine(new HttpVersion(1,1),200,"OK"));
            }
            // Pause job
            if(data.endsWith("=pause")) {
                jobProgress.put(getJobFromRequest(post), -1);
                return new BasicHttpResponse(new BasicStatusLine(new HttpVersion(1,1),200,"OK"));
            }
            // Terminate job
            if(data.endsWith("=terminate")) {
                jobProgress.put(getJobFromRequest(post), -2);
                return new BasicHttpResponse(new BasicStatusLine(new HttpVersion(1,1),200,"OK"));
            }
        }
        if(request instanceof HttpGet) {
            HttpGet get = (HttpGet) request;
            // Crawl configuration request
            if(get.getURI().getPath().endsWith("crawler-beans.cxml")) {
                String job = getJobFromRequest(get);
                String filename = HeritrixClient.createCrawlConfiguration(job,
                        crawlUrls.get(job),
                        "src/test/resources/" + job + "_configuration.cxml");
                HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new HttpVersion(1,1),200,"OK"));
                response.setEntity(new FileEntity(new File(filename)));
                return response;
            }

            // Job status XML request
            if(get.containsHeader("Accept")) {
                Assert.assertEquals(get.getHeaders("Accept")[0].getValue(), "application/xml");
                String job = getJobFromRequest(get);
                String message = "<?xml version=\"1.0\" standalone='yes'?><job><statusDescription>" +
                        getJobStatus(job) + "</statusDescription><uriTotalsReport><downloadedUriCount>6" +
                        "</downloadedUriCount></uriTotalsReport></job>";
                HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new HttpVersion(1,1),200,"OK"));
                response.setEntity(new StringEntity(message));
                return response;
            }

            // Job status HTML request
            String message = "<h3>Crawl Log <a href=\"/engine/anypath/jobs/test/20161224163617/logs/crawl.log?format=paged";
            HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new HttpVersion(1,1),200,"OK"));
            response.setEntity(new StringEntity(message));
            return response;
        }
        if(request instanceof HttpPut) {
            HttpPut put = (HttpPut) request;
            String configXml = getResponseAsString(put);
            crawlUrls.put(getJobFromRequest(put), HeritrixClient.getListOfCrawlUrlsFromXml(configXml));
            return new BasicHttpResponse(new BasicStatusLine(new HttpVersion(1,1),200,"OK"));
        }
        return new BasicHttpResponse(new BasicStatusLine(new HttpVersion(1,1),200,"OK"));
    }

    private String getJobFromRequest(HttpUriRequest request) {
        String[] urlParts = request.getURI().toString().split("/");
        for(int i = 0; i < urlParts.length; i++) {
            if (urlParts[i].equals("job")) {
                return urlParts[i + 1];
            }
        }
        return null;
    }

    private String getJobStatus(String job) {
        String result;
        switch (jobProgress.get(job)) {
            case 0: result = "Unbuilt";
                break;
            case 6: result = "Finished: FINISHED";
                break;
            case -1: result = "Active: PAUSED";
                break;
            case -2: result = "Finished: ABORTED";
                break;
            default: result = "Active: RUNNING";
            jobProgress.put(job, jobProgress.get(job) + 1);
        }
        return result;
    }

    private String getResponseAsString(HttpPut response) throws IOException {
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    //<editor-fold desc="Unused overwrites">
    @Override
    public HttpParams getParams() {
        return null;
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return null;
    }

    @Override
    public HttpResponse execute(HttpUriRequest httpUriRequest, HttpContext httpContext) throws IOException, ClientProtocolException {
        return null;
    }

    @Override
    public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest) throws IOException {
        return null;
    }

    @Override
    public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) throws IOException {
        return null;
    }

    @Override
    public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler) throws IOException {
        return null;
    }

    @Override
    public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException {
        return null;
    }

    @Override
    public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler) throws IOException {
        return null;
    }

    @Override
    public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException {
        return null;
    }
    //</editor-fold>
}
