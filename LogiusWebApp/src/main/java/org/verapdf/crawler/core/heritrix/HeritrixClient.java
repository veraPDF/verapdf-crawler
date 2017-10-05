package org.verapdf.crawler.core.heritrix;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
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
import org.verapdf.crawler.configurations.HeritrixConfiguration;
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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HeritrixClient {

    private String configTemplatePath;
    private final String engineUrl;
    private final String baseJobUrl;
    private final String logiusAppUrl;
    private HttpClient httpClient;

    public HeritrixClient(HeritrixConfiguration config) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, MalformedURLException {
        String baseUrl = config.getUrl();
        this.engineUrl = baseUrl + "engine";
        this.baseJobUrl = this.engineUrl + "/job/";
        this.logiusAppUrl = config.getLogiusAppUrl();
        // Configure credential provider
        URL domain = new URL(baseUrl);
        HttpHost targetHost = new HttpHost(domain.getHost(), domain.getPort(), "https");
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                new UsernamePasswordCredentials(config.getLogin(), config.getPassword()));
        // Configure http client to ignore certificate issues
        httpClient = HttpClients.custom()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                        .loadTrustMaterial(null, (x509Certificates, s) -> true)
                        .build(), (s, sslSession) -> true)).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setDefaultCredentialsProvider(credsProvider)
                        .setRetryHandler(new StandardHttpRequestRetryHandler(3, true)).build();
        configTemplatePath = config.getConfigTemplatePath();
    }

    public boolean testHeritrixAvailability() throws IOException {
        HttpGet get = new HttpGet(this.engineUrl);
        boolean result = httpClient.execute(get).getStatusLine().getStatusCode() == 200;
        get.releaseConnection();
        return result;
    }

    public int getDownloadedCount(String heritrixJobId) throws IOException, ParserConfigurationException, SAXException {
        String status = getFullStatus(heritrixJobId);
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(status));
        Document doc = db.parse(is);
        NodeList nodes = doc.getElementsByTagName("uriTotalsReport");
        nodes = ((Element)nodes.item(0)).getElementsByTagName("downloadedUriCount");
        return Integer.parseInt(nodes.item(0).getTextContent());
    }

    public void unpauseJob(String heritrixJobId) throws IOException {
        postJobAction(heritrixJobId, "unpause");
    }

    public void pauseJob(String heritrixJobId) throws IOException{
        postJobAction(heritrixJobId, "pause");
    }

    public void terminateJob(String heritrixJobId) throws IOException{
        postJobAction(heritrixJobId, "terminate");
    }

    public void teardownJob(String heritrixJobId) throws IOException{
        postJobAction(heritrixJobId, "teardown");
    }

    public void launchJob(String heritrixJobId) throws IOException{
        postJobAction(heritrixJobId, "launch");
    }

    public void buildJob(String heritrixJobId) throws IOException{
        postJobAction(heritrixJobId, "build");
    }

    public String createJob(String heritrixJobId, String domain) throws IOException{
        doPost(this.engineUrl, "createpath=" + heritrixJobId +"&action=create");

        ArrayList<String> crawlUrls = new ArrayList<>();
        crawlUrls.add("https://" + domain);
        crawlUrls.add("http://" + domain);

        File configurationFile = createCrawlConfiguration(heritrixJobId, crawlUrls);
        submitConfigFile(heritrixJobId, configurationFile);
        configurationFile.delete();

        return heritrixJobId;
    }

    public String getCurrentJobStatus(String heritrixJobId) throws IOException, ParserConfigurationException, SAXException {
        String status = getFullStatus(heritrixJobId);

        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(status));
        Document doc = db.parse(is);
        NodeList nodes = doc.getElementsByTagName("statusDescription"); // TODO: check either crawlControllerState or crawlExitStatus
        String statusDescription = nodes.item(0).getTextContent();
        if (statusDescription.contains(":")) {
            statusDescription = statusDescription.split(": ")[1];
        }
        return statusDescription.toLowerCase();
    }

    public boolean isJobFinished(String heritrixJobId) throws IOException, ParserConfigurationException, SAXException {
        String currentJobStatus = getCurrentJobStatus(heritrixJobId);
        return currentJobStatus.startsWith("finished") || currentJobStatus.startsWith("aborted");
    }

    public List<String> getListOfCrawlUrls(String heritrixJobId) throws IOException {
        HttpGet get = new HttpGet(this.baseJobUrl + heritrixJobId + "/jobdir/crawler-beans.cxml");
        String configXml = getResponseAsString(httpClient.execute(get));
        get.releaseConnection();
        return getListOfCrawlUrlsFromXml(configXml);
    }

    public static List<String> getListOfCrawlUrlsFromXml(String configXml) {
        String urls = getBetweenStrings(configXml, "# URLS HERE", "</prop>");
        ArrayList<String> result = new ArrayList<>(Arrays.asList(urls.split("\\s+")));
        if(result.get(0).equals(""))
            result.remove(0);
        return result;
    }

    public String getConfig(String jobUrl) throws IOException {
        String anypath = "anypath/";
        String jobDirectory = jobUrl.substring(jobUrl.indexOf(anypath) + anypath.length());
        File file = new File(jobDirectory + "sample_configuration.cxml");
        if(!file.exists()) {
            file = new File(jobDirectory + "crawler-beans.cxml");
        }
        byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        /*String config = getLogFileByURL(jobUrl + "sample_configuration.cxml");
        if(config.equals("")) {
            config = getLogFileByURL(jobUrl + "crawler-beans.cxml");
        }*/
        return new String(encoded);
    }

    private void postJobAction(String heritrixJobId, String action) throws IOException {
        doPost(this.baseJobUrl + heritrixJobId, "action=" + action);
    }

    private void doPost(String path, String entity) throws IOException {
        HttpPost post = new HttpPost(path);
        post.setEntity(new StringEntity(entity));
        httpClient.execute(post);
        post.releaseConnection();
    }

    private File createCrawlConfiguration(String heritrixJobId, List<String> crawlUrls) throws IOException {

        StringBuilder sb = new StringBuilder();
        for(String url: crawlUrls) {
            sb.append(url);
            sb.append(" ").append(System.lineSeparator());
            String surt = buildSurt(url);
            sb.append(" ").append(System.lineSeparator());
            sb.append(surt);
        }

        File source = new File(configTemplatePath);
        File destination = File.createTempFile(heritrixJobId, ".cxml");
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(destination.toPath()), charset);
        content = content.replace("${logiusHeritrixJobId}", heritrixJobId);
        content = content.replace("${logiusOperatorContactUrl}", crawlUrls.get(0));
        content = content.replace("${logiusUrls}", sb.toString());
        content = content.replace("${logiusAppUrl}", logiusAppUrl);
        Files.write(destination.toPath(), content.getBytes(charset));
        return destination;
    }

    /*private String getLogFileByURL(String url) throws IOException {
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
    }*/

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

    private String getFullStatus(String heritrixJobId) throws IOException {
        HttpGet get = new HttpGet(this.baseJobUrl + heritrixJobId);
        get.setHeader("Accept","application/xml");

        String result = getResponseAsString(httpClient.execute(get));
        get.releaseConnection();
        return result;
    }

    private void submitConfigFile(String heritrixJobId, File configFile) throws IOException {
        HttpPut put = new HttpPut(this.baseJobUrl + heritrixJobId + "/jobdir/crawler-beans.cxml");
        FileEntity entity = new FileEntity(configFile);
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
        builder.append(System.lineSeparator());
        return builder.toString();
    }
    //</editor-fold>
}
