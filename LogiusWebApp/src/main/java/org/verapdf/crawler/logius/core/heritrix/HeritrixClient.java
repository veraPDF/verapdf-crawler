package org.verapdf.crawler.logius.core.heritrix;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.jopendocument.util.StringInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.verapdf.common.GracefulHttpClient;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.exception.HeritrixException;
import org.verapdf.crawler.logius.model.Role;
import org.verapdf.crawler.logius.monitoring.HeritrixCrawlJobStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class HeritrixClient {

    private static final Logger logger = LoggerFactory.getLogger(HeritrixClient.class);

    private static final long POST_CONNECTION_INTERVAL = 60 * 1000;
    private static final int POST_MAX_CONNECTION_RETRIES = 5;
    private static final long GET_CONNECTION_INTERVAL = 5 * 1000;
    private static final int GET_MAX_CONNECTION_RETRIES = 2;
    private static final String ZERO = "0";
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final String STATUS_DESCRIPTION_XPATH = "/job/statusDescription";
    private final CredentialsProvider credsProvider;
    private final SSLConnectionSocketFactory sslConnectionSocketFactory;
    @Value("${logius.heritrix.configTemplatePath}")
    private String configTemplatePath;
    @Value("${logius.heritrix.maxDocumentCount}")
    private String maxDocumentsCount;
    @Value("${logius.heritrix.logiusAppUrl}")
    private String logiusAppUrl;
    @Value("${logius.heritrix.jobsFolder}")
    private String jobsFolder;
    @Value("${logius.heritrix.url}engine")
    private String engineUrl;
    @Value("${logius.heritrix.url}engine/job/")
    private String baseJobUrl;


    public HeritrixClient(CredentialsProvider credsProvider, SSLConnectionSocketFactory sslConnectionSocketFactory) {
        this.credsProvider = credsProvider;
        this.sslConnectionSocketFactory = sslConnectionSocketFactory;
        logger.info("heritrix client created, url {s}", engineUrl);
    }

    private static String buildSurt(String url) {
        String[] parts = url.split("(?<=://)|\\.");
        parts[parts.length - 1] = parts[parts.length - 1].split("/", 2)[0];
        StringBuilder builder = new StringBuilder("+");
        builder.append("http://");
        builder.append("(");
        for (int i = parts.length - 1; i > 0; i--) {
            builder.append(parts[i]);
            builder.append(",");
        }
        builder.append(System.lineSeparator());
        return builder.toString();
    }

    public String getEngineUrl() {
        return engineUrl;
    }

//    static int getDownloadedCount(String heritrixJobId) throws IOException, ParserConfigurationException, SAXException {
//        String status = getFullStatus(heritrixJobId);
//        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//        InputSource is = new InputSource();
//        is.setCharacterStream(new StringReader(status));
//        Document doc = db.parse(is);
//        NodeList nodes = doc.getElementsByTagName("uriTotalsReport");
//        nodes = ((Element)nodes.item(0)).getElementsByTagName("downloadedUriCount");
//        return Integer.parseInt(nodes.item(0).getTextContent());
//    }

    public boolean testHeritrixAvailability() throws IOException {
        HttpGet get = new HttpGet(this.engineUrl);
        try (CloseableHttpClient httpClient = buildHttpClient(GET_MAX_CONNECTION_RETRIES, GET_CONNECTION_INTERVAL)) {
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                return response.getStatusLine().getStatusCode() == 200;
            }
        }
    }

    public void unpauseJob(String heritrixJobId) {
        postJobAction(heritrixJobId, "unpause");
    }

    public void pauseJob(String heritrixJobId) {
        postJobAction(heritrixJobId, "pause");
    }

    public void terminateJob(String heritrixJobId) {
        postJobAction(heritrixJobId, "terminate");
    }

    public void teardownJob(String heritrixJobId) {
        postJobAction(heritrixJobId, "teardown");
    }

    public void launchJob(String heritrixJobId) {
        postJobAction(heritrixJobId, "launch");
    }

    public void buildJob(String heritrixJobId) {
        postJobAction(heritrixJobId, "build");
    }

    public String createJob(CrawlJob crawlJob) throws IOException {
        String heritrixJobId = crawlJob.getHeritrixJobId();
        String domain = crawlJob.getDomain();
        boolean isAdmin = crawlJob.getUser().getRole() == Role.ADMIN;
        doPost(this.engineUrl, "createpath=" + heritrixJobId + "&action=create");

        ArrayList<String> crawlUrls = new ArrayList<>();
        crawlUrls.add("https://" + domain);
        crawlUrls.add("http://" + domain);

        File configurationFile = createCrawlConfiguration(heritrixJobId, isAdmin, crawlUrls);
        submitConfigFile(heritrixJobId, configurationFile);
        configurationFile.delete();

        return heritrixJobId;
    }

    public boolean deleteJobFolder(String heritrixJobId) throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        String currentJobStatus = getStatusDescription(heritrixJobId);
        if (currentJobStatus.equalsIgnoreCase("unbuilt")) {
            String path = this.jobsFolder.endsWith("/") ?
                    this.jobsFolder + heritrixJobId :
                    this.jobsFolder + "/" + heritrixJobId;
            try {
                FileUtils.deleteDirectory(new File(path));
                return true;
            } catch (IOException e) {
                logger.error("Can't delete heritrix job folder", e);
                return false;
            }
        }
        return false;
    }

    public boolean isJobFinished(String heritrixJobId) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        String currentJobStatus = getStatusDescription(heritrixJobId);
        currentJobStatus = currentJobStatus.trim().toLowerCase();
        return currentJobStatus.startsWith("finished");
    }

    private String getStatusDescription(String heritrixJobId) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        try (InputStream status = new StringInputStream(getFullStatus(heritrixJobId))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(status);
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();
            String res = (String) xpath.evaluate(STATUS_DESCRIPTION_XPATH, document, XPathConstants.STRING);
            return res.trim();
        }
    }

//    static List<String> getListOfCrawlUrls(String heritrixJobId) throws IOException {
//        HttpGet get = new HttpGet(this.baseJobUrl + heritrixJobId + "/jobdir/crawler-beans.cxml");
//        String configXml = getResponseAsString(buildHttpClient(GET_MAX_CONNECTION_RETRIES, GET_CONNECTION_INTERVAL)
//                .execute(get));
//        get.releaseConnection();
//        return getListOfCrawlUrlsFromXml(configXml);
//    }

//    static static List<String> getListOfCrawlUrlsFromXml(String configXml) {
//        String urls = getBetweenStrings(configXml, "# URLS HERE", "</prop>");
//        ArrayList<String> result = new ArrayList<>(Arrays.asList(urls.split("\\s+")));
//        if(result.get(0).equals(""))
//            result.remove(0);
//        return result;
//    }

//    static String getConfig(String jobUrl) throws IOException {
//        String anypath = "anypath/";
//        String jobDirectory = jobUrl.substring(jobUrl.indexOf(anypath) + anypath.length());
//        File file = new File(jobDirectory + "sample_configuration.cxml");
//        if(!file.exists()) {
//            file = new File(jobDirectory + "crawler-beans.cxml");
//        }
//        byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
//        /*String config = getLogFileByURL(jobUrl + "sample_configuration.cxml");
//        if(config.equals("")) {
//            config = getLogFileByURL(jobUrl + "crawler-beans.cxml");
//        }*/
//        return new String(encoded);
//    }

    public HeritrixCrawlJobStatus getHeritrixStatus(String heritrixJobId) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        try (InputStream status = new StringInputStream(getFullStatus(heritrixJobId))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(status);
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();
            String statusDescription = (String) xpath.evaluate(STATUS_DESCRIPTION_XPATH, document, XPathConstants.STRING);

            String downloadedUriCountXpath = "/job/uriTotalsReport/downloadedUriCount";
            String downloadedUriCountString = (String) xpath.evaluate(downloadedUriCountXpath, document, XPathConstants.STRING);
            String queuedUriCountXpath = "/job/uriTotalsReport/queuedUriCount";
            String queuedUriCountString = (String) xpath.evaluate(queuedUriCountXpath, document, XPathConstants.STRING);
            String totalUriCountXpath = "/job/uriTotalsReport/totalUriCount";
            String totalUriCountString = (String) xpath.evaluate(totalUriCountXpath, document, XPathConstants.STRING);
            HeritrixCrawlJobStatus.HeritrixURITotalsStatus uriTotalsStatus = null;
            try {
                long downloadedUriCount = Long.parseLong(downloadedUriCountString);
                long queuedUriCount = Long.parseLong(queuedUriCountString);
                long totalUriCount = Long.parseLong(totalUriCountString);
                uriTotalsStatus = new HeritrixCrawlJobStatus.HeritrixURITotalsStatus(downloadedUriCount, queuedUriCount, totalUriCount);
            } catch (NumberFormatException e) {
                // No need to log. This block of code can be reached only in cases
                // when there is no uri information
            }

            String jobLogTailXpath = "/job/jobLogTail/value";
            NodeList jobLogTailNodes = (NodeList) xpath.evaluate(jobLogTailXpath,
                    document,
                    XPathConstants.NODESET);
            List<String> jobLogTail = new ArrayList<>();
            for (int i = 0; i < jobLogTailNodes.getLength(); ++i) {
                Node value = jobLogTailNodes.item(i);
                jobLogTail.add(value.getTextContent().trim());
            }
            return new HeritrixCrawlJobStatus(statusDescription, uriTotalsStatus, jobLogTail);
        }
    }

    private void postJobAction(String heritrixJobId, String action) {
        doPost(this.baseJobUrl + heritrixJobId, "action=" + action);
    }

    private void doPost(String path, String entity) {
        try {
            HttpPost post = new HttpPost(path);
            post.setEntity(new StringEntity(entity));
            try (CloseableHttpClient httpClient = buildHttpClient(POST_MAX_CONNECTION_RETRIES, POST_CONNECTION_INTERVAL)) {
                CloseableHttpResponse response = httpClient.execute(post);
                response.close();
            }
        } catch (IOException e) {
            logger.error("Can't execute post request to heritrix ", e);
            throw new HeritrixException(e);
        }
    }

//    private String getLogFileByURL(String url) throws IOException {
//        HttpGet get = new HttpGet(url);
//        InputStream response = httpClient.execute(get).getEntity().getContent();
//        Scanner sc = new Scanner(response);
//        StringBuilder result = new StringBuilder();
//        while(sc.hasNext()) {
//            result.append(sc.nextLine() + System.lineSeparator());
//        }
//        get.releaseConnection();
//        if(result.toString().contains("The page you are looking for does not exist"))
//            return "";
//        return result.toString();
//    }

    //<editor-fold desc="Private helpers">

    private File createCrawlConfiguration(String heritrixJobId, boolean isAdmin, List<String> crawlUrls) throws IOException {

        StringBuilder sb = new StringBuilder();
        for (String url : crawlUrls) {
            sb.append(url);
            sb.append(" ").append(System.lineSeparator());
            String surt = buildSurt(url);
            sb.append(" ").append(System.lineSeparator());
            sb.append(surt);
        }
        File source = new File(configTemplatePath);
        File destination = File.createTempFile(heritrixJobId, ".cxml");
        String content = new String(Files.readAllBytes(source.toPath()), DEFAULT_CHARSET);
        String maxCount = isAdmin ? ZERO : maxDocumentsCount;
        content = content.replace("${logiusHeritrixJobId}", heritrixJobId);
        content = content.replace("${logiusOperatorContactUrl}", crawlUrls.get(0));
        content = content.replace("${logiusUrls}", sb.toString());
        content = content.replace("${logiusAppUrl}", logiusAppUrl);
        content = content.replace("${maxDocumentsCount}", maxCount);

        Files.write(destination.toPath(), content.getBytes(DEFAULT_CHARSET));
        return destination;
    }

    private String getResponseAsString(HttpResponse response) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()))) {
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    private String getFullStatus(String heritrixJobId) throws IOException {
        HttpGet get = new HttpGet(this.baseJobUrl + heritrixJobId);
        get.setHeader("Accept", "application/xml");
        try (CloseableHttpClient httpClient = buildHttpClient(GET_MAX_CONNECTION_RETRIES, GET_CONNECTION_INTERVAL)) {
            try (CloseableHttpResponse httpResponse = httpClient.execute(get)) {
                return getResponseAsString(httpResponse);
            }
        }
    }

//    private static String getBetweenStrings(String text, String textFrom, String textTo) {
//
//        String result;
//
//        // Cut the beginning of the text to not occasionally meet a
//        // 'textTo' value in it:
//        result = text.substring(
//                text.indexOf(textFrom) + textFrom.length(),
//                text.length());
//
//        // Cut the excessive ending of the text:
//        result = result.substring(0, result.indexOf(textTo));
//
//        return result;
//    }

    private void submitConfigFile(String heritrixJobId, File configFile) throws IOException {
        HttpPut put = new HttpPut(this.baseJobUrl + heritrixJobId + "/jobdir/crawler-beans.cxml");
        FileEntity entity = new FileEntity(configFile);
        put.setEntity(entity);
        try (CloseableHttpClient httpClient = buildHttpClient(POST_MAX_CONNECTION_RETRIES, POST_CONNECTION_INTERVAL)) {
            CloseableHttpResponse response = httpClient.execute(put);
            response.close();
        }
    }

    private CloseableHttpClient buildHttpClient(int maxRetries, long retryInterval) {
        // Configure http client to ignore certificate issues
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslConnectionSocketFactory).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setDefaultCredentialsProvider(this.credsProvider)
                .setRetryHandler(new StandardHttpRequestRetryHandler(3, true)).build();
        return new GracefulHttpClient(httpClient, maxRetries, retryInterval);
    }
}
