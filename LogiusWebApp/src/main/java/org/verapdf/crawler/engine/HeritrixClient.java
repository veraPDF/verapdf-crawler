package org.verapdf.crawler.engine;

import org.verapdf.crawler.helpers.debugHelpers.OpenTrustManager;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class HeritrixClient {

    private String baseUrl;
    private String username;
    private String password;

    public HeritrixClient(String url, String username, String password) {
        baseUrl = url;
        this.username = username;
        this.password = password;
    }

    /*public String doJob(String job, List<String> crawlUrls) throws NoSuchAlgorithmException, IOException, KeyManagementException, InterruptedException, ParserConfigurationException, SAXException {
        createJob(job, crawlUrls);
        buildJob(job);
        launchJob(job);
        unpauseJob(job);
        while(!isJobFinished(job)) {
            Thread.sleep(sleepTime);
        }
        return getCrawlLogUri(job);
    }*/

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

    public void unpauseJob(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HashMap<String, String> postDataParams = new HashMap<>();
        postDataParams.put("action","unpause");

        HttpURLConnection conn = buildPostRequest(postDataParams, baseUrl + "/org/verapdf/crawler/engine/job/" + job);

        conn.getResponseCode();
    }

    public void pauseJob(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HashMap<String, String> postDataParams = new HashMap<>();
        postDataParams.put("action","pause");

        HttpURLConnection conn = buildPostRequest(postDataParams, baseUrl + "/org/verapdf/crawler/engine/job/" + job);

        conn.getResponseCode();
    }

    public void terminateJob(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HashMap<String, String> postDataParams = new HashMap<>();
        postDataParams.put("action","terminate");

        HttpURLConnection conn = buildPostRequest(postDataParams, baseUrl + "/org/verapdf/crawler/engine/job/" + job);

        conn.getResponseCode();
    }

    public String createJob(String job, List<String> crawlUrls) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HashMap<String, String> postDataParams = new HashMap<>();
        postDataParams.put("createpath",job);
        postDataParams.put("action","create");

        HttpURLConnection conn = buildPostRequest(postDataParams, baseUrl + "/org/verapdf/crawler/engine/" + job);

        conn.getResponseCode();

        String configurationFile = createCrawlConfiguration(job, crawlUrls);
        submitConfigFile(job, configurationFile);
        new File(configurationFile).delete();

        return job;
    }

    public void launchJob(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HashMap<String, String> postDataParams = new HashMap<>();
        postDataParams.put("action","launch");

        HttpURLConnection conn = buildPostRequest(postDataParams, baseUrl + "/org/verapdf/crawler/engine/job/" + job);

        conn.getResponseCode();
    }

    public void buildJob(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HashMap<String, String> postDataParams = new HashMap<>();
        postDataParams.put("action","build");

        HttpURLConnection conn = buildPostRequest(postDataParams, baseUrl + "/org/verapdf/crawler/engine/job/" + job);

        conn.getResponseCode();
    }

    public String getCrawlLogUri(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        HttpURLConnection conn = buildGetRequest(baseUrl + "/org/verapdf/crawler/engine/job/" + job);
        String status = getResponseString(conn);
        String result = getBetweenStrings(status, "<h3>Crawl Log <a href=\"", "?format=paged");
        result = baseUrl + result;
        result = result.replace("logs/crawl.log","mirror/PDFReport.txt");
        return result;
    }

    public boolean isJobFinished(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        return getCurrentJobStatus(job).startsWith("Finished");
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

    public List<String> getListOfPdfFiles(String logUrl) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        HttpURLConnection conn = buildGetRequest(logUrl);

        ArrayList<String> result = new ArrayList<>();

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String output;
        while ((output = br.readLine()) != null) {
            if(output.contains("application/pdf")) {
                result.add(output);
            }
        }
        return result;
    }

    public List<String> getListOfCrawlUrls(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        HttpURLConnection conn = buildGetRequest(baseUrl + "/org/verapdf/crawler/engine/job/" + job + "/jobdir/crawler-beans.cxml");
        String configXml = getResponseString(conn);
        String urls = getBetweenStrings(configXml, "# URLS HERE", "</prop>");
        ArrayList<String> result = new ArrayList<>(Arrays.asList(urls.split("\\s+")));
        if(result.get(0).equals(""))
            result.remove(0);
        return result;
    }

    //<editor-fold desc="Private org.verapdf.crawler.helpers">
    private String getFullStatus(String job) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        HttpURLConnection conn = buildGetRequest(baseUrl + "/org/verapdf/crawler/engine/job/" + job);
        conn.addRequestProperty("Accept", "application/xml");
        return getResponseString(conn);
    }

    private void submitConfigFile(String job, String filename) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        File file = new File(filename);

        HttpURLConnection conn = buildRequest(baseUrl + "/org/verapdf/crawler/engine/job/" + job + "/jobdir/crawler-beans.cxml");
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Length", String.valueOf(file.length()));
        conn.setUseCaches(false);
        conn.setDoOutput(true);

        conn.connect();

        OutputStream outputStream = conn.getOutputStream();
        Files.copy(file.toPath(), outputStream);
        outputStream.close();

        conn.getResponseCode();
    }

    private String createCrawlConfiguration(String job, List<String> crawlUrls) throws IOException {

        StringBuilder sb = new StringBuilder();
        for(String url: crawlUrls) {
            sb.append(url + "\n");
        }

        File source = new File("src/main/org.verapdf.crawler.resources/sample_configuration.cxml");
        File destination = new File("src/main/org.verapdf.crawler.resources/" + job + "_configuration.cxml");
        Files.copy(source.toPath(), destination.toPath());

        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(destination.toPath()), charset);
        content = content.replace("******", crawlUrls.get(0));
        content = content.replace("######", sb.toString());
        Files.write(destination.toPath(), content.getBytes(charset));
        return "src/main/org.verapdf.crawler.resources/" + job + "_configuration.cxml";
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }

    private HttpURLConnection buildPostRequest(HashMap<String, String> postDataParams, String stringUrl) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HttpURLConnection conn = buildRequest(stringUrl);

        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        writer.write(getPostDataString(postDataParams));

        writer.flush();
        writer.close();
        os.close();

        return conn;
    }

    private HttpURLConnection buildGetRequest(String stringUrl) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HttpURLConnection conn = buildRequest(stringUrl);

        conn.setRequestMethod("GET");

        return conn;
    }

    private HttpURLConnection buildRequest(String stringUrl) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password.toCharArray());
            }
        });

        URL url = new URL(stringUrl);
        HttpURLConnection conn;
        conn = (HttpURLConnection) url.openConnection();

        OpenTrustManager.apply((HttpsURLConnection) conn);
        ((HttpsURLConnection) conn).setHostnameVerifier(
                new HostnameVerifier(){
                    public boolean verify(String arg0, SSLSession arg1) {
                        return true;
                    }
                }
        );

        return conn;
    }

    private String getResponseString(HttpURLConnection conn) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder sb = new StringBuilder();
        String output;
        while ((output = br.readLine()) != null) {
            sb.append(output);
        }
        return sb.toString();
    }

    private String getBetweenStrings(String text, String textFrom, String textTo) {

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
    //</editor-fold>
}
