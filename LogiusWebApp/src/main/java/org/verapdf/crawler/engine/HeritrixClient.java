package org.verapdf.crawler.engine;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.ssl.SSLContexts;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class HeritrixClient {

    public static String baseDirectory;
    private String baseUrl;
    private HttpClient httpClient;

    public HeritrixClient(String url, String username, String password) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, MalformedURLException {
        baseUrl = url;
        // Configure credential provider
        URL domain = new URL(baseUrl);
        HttpHost targetHost = new HttpHost(domain.getHost(), domain.getPort(), "https");
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                new UsernamePasswordCredentials(username, password));
        // Configure http client to ignore certificate issues
        httpClient = HttpClients.custom()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                        .loadTrustMaterial(null, (x509Certificates, s) -> true)
                        .build(), (s, sslSession) -> true)).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setDefaultCredentialsProvider(credsProvider)
                        .setRetryHandler(new StandardHttpRequestRetryHandler(3, true)).build();
    }

    public void setBaseDirectory(String baseDirectory) { HeritrixClient.baseDirectory = baseDirectory; }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public int getDownloadedCount(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        String status = getFullStatus(job);
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(status));
        Document doc = db.parse(is);
        NodeList nodes = doc.getElementsByTagName("uriTotalsReport");
        nodes = ((Element)nodes.item(0)).getElementsByTagName("downloadedUriCount");
        return Integer.parseInt(nodes.item(0).getTextContent());
    }

    public void unpauseJob(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException, AuthenticationException {
        HttpPost post = new HttpPost(baseUrl + "engine/job/" + job);
        post.setEntity(new StringEntity("action=unpause"));

        httpClient.execute(post);
        post.releaseConnection();
    }

    public void pauseJob(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HttpPost post = new HttpPost(baseUrl + "engine/job/" + job);
        post.setEntity(new StringEntity("action=pause"));

        httpClient.execute(post);
        post.releaseConnection();
    }

    public void terminateJob(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HttpPost post = new HttpPost(baseUrl + "engine/job/" + job);
        post.setEntity(new StringEntity("action=terminate"));

        httpClient.execute(post);
        post.releaseConnection();
    }

    public void teardownJob(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HttpPost post = new HttpPost(baseUrl + "engine/job/" + job);
        post.setEntity(new StringEntity("action=teardown"));

        httpClient.execute(post);
        post.releaseConnection();
    }

    public String createJob(String job, List<String> crawlUrls) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HttpPost post = new HttpPost(baseUrl + "engine/");
        post.setEntity(new StringEntity("createpath=" + job +"&action=create"));

        httpClient.execute(post);
        post.releaseConnection();

        String configurationFile = createCrawlConfiguration(job, crawlUrls, baseDirectory + job + "_configuration.cxml");
        submitConfigFile(job, configurationFile);
        new File(configurationFile).delete();

        return job;
    }

    public void launchJob(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HttpPost post = new HttpPost(baseUrl + "engine/job/" + job);
        post.setEntity(new StringEntity("action=launch"));

        httpClient.execute(post);
        post.releaseConnection();
    }

    public void buildJob(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HttpPost post = new HttpPost(baseUrl + "engine/job/" + job);
        post.setEntity(new StringEntity("action=build"));

        httpClient.execute(post);
        post.releaseConnection();
    }

    public String getCurrentJobStatus(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        String status = getFullStatus(job);

        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(status));
        Document doc = db.parse(is);
        NodeList nodes = doc.getElementsByTagName("statusDescription");
        return nodes.item(0).getTextContent();
    }

    public boolean isJobFinished(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        return getCurrentJobStatus(job).startsWith("Finished");
    }

    public List<String> getListOfCrawlUrls(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        HttpGet get = new HttpGet(baseUrl + "engine/job/" + job + "/jobdir/crawler-beans.cxml");
        String configXml = getResponseAsString(httpClient.execute(get));
        get.releaseConnection();
        return getListOfCrawlUrlsFromXml(configXml);
    }

    public static List<String> getListOfCrawlUrlsFromXml(String configXml) throws IOException {
        String urls = getBetweenStrings(configXml, "# URLS HERE", "</prop>");
        ArrayList<String> result = new ArrayList<>(Arrays.asList(urls.split("\\s+")));
        if(result.get(0).equals(""))
            result.remove(0);
        return result;
    }

    public static String createCrawlConfiguration(String job, List<String> crawlUrls, String targetfileName) throws IOException {

        StringBuilder sb = new StringBuilder();
        for(String url: crawlUrls) {
            sb.append(url);
            sb.append(" " + System.lineSeparator());
            String surt = buildSurt(url);
            sb.append(" " + System.lineSeparator());
            sb.append(surt);
        }

        File source = new File(baseDirectory + "sample_configuration.cxml");
        File destination = new File(targetfileName);
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(destination.toPath()), charset);
        content = content.replace("******", crawlUrls.get(0));
        content = content.replace("######", sb.toString());
        Files.write(destination.toPath(), content.getBytes(charset));
        return targetfileName;
    }

    public String getValidPDFReportUri(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        HttpGet get = new HttpGet(baseUrl + "engine/job/" + job);
        String status = getResponseAsString(httpClient.execute(get));
        get.releaseConnection();
        String result = getBetweenStrings(status, "<h3>Crawl Log <a href=\"", "?format=paged");
        result = baseUrl + result;
        result = result.replace("logs/crawl.log","mirror/Valid_PDF_Report.txt");
        result = result.replace("//engine", "/engine");
        return result;
    }

    public String getLogFileByURL(String url) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        HttpGet get = new HttpGet(url);
        InputStream response = httpClient.execute(get).getEntity().getContent();
        Scanner sc = new Scanner(response);
        StringBuilder result = new StringBuilder();
        while(sc.hasNext()) {
            result.append(sc.nextLine() + System.lineSeparator());
        }
        get.releaseConnection();
        if(result.toString().contains("The page you are looking for does not exist"))
            return "";
        return result.toString();
    }

    //<editor-fold desc="Private helpers">

    private String getResponseAsString(HttpResponse response) throws IOException {
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    private String getFullStatus(String job) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        HttpGet get = new HttpGet(baseUrl + "engine/job/" + job);
        get.setHeader("Accept","application/xml");

        String result = getResponseAsString(httpClient.execute(get));
        get.releaseConnection();
        return result;
    }

    private void submitConfigFile(String job, String filename) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        File file = new File(filename);
        HttpPut put = new HttpPut(baseUrl + "engine/job/" + job + "/jobdir/crawler-beans.cxml");
        FileEntity entity = new FileEntity(file);
        put.setEntity(entity);
        httpClient.execute(put);
        put.releaseConnection();
    }

    private static String getBetweenStrings(String text, String textFrom, String textTo) {

        String result;

        // Cut the beginning of the text to not occasionally meet a
        // 'textTo' value in it:
        result = text.substring(
                text.indexOf(textFrom) + textFrom.length(),
                text.length());

        // Cut the excessive ending of the text:
        result = result.substring(0, result.indexOf(textTo));

        return result;
    }

    private static String buildSurt(String url) {
        String[] parts = url.split("(?<=://)|\\.");
        parts[parts.length - 1] = parts[parts.length - 1].split("/",2)[0];
        StringBuilder builder = new StringBuilder("+");
        builder.append("http://");
        builder.append("(");
        for(int i = parts.length - 1; i > 0; i--) {
            builder.append(parts[i]);
            builder.append(",");
        }
        System.out.println(builder.toString());
        return builder.toString();
    }
    //</editor-fold>
}
