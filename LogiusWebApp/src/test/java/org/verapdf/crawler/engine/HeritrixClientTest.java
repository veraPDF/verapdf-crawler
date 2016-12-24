package org.verapdf.crawler.engine;

import org.apache.http.auth.AuthenticationException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.verapdf.crawler.app.Sample;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

public class HeritrixClientTest {
    private static HeritrixClient client;

    @BeforeClass
    public static void initializeTests() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, MalformedURLException {
        client = new HeritrixClient("https://localhost:8443/",8443, "admin", "admin");
        client.setHttpClient(new HttpClientStub());
    }

    @Test
    public void testJobCreation() throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        ArrayList<String> urls = new ArrayList<>();
        urls.add("http://localhost:8080");
        client.createJob("test1", urls);
        urls.add(0, "http://localhost:8000");
        client.createJob("test2", urls);
        assertEquals("http://localhost:8080",client.getListOfCrawlUrls("test1").get(0));
        assertEquals(urls,client.getListOfCrawlUrls("test2"));
        assertEquals("Unbuilt", client.getCurrentJobStatus("test1"));
        assertFalse(client.isJobFinished("test2"));
    }

    @Test
    public void testJobControlCommands() throws KeyManagementException, NoSuchAlgorithmException, AuthenticationException, IOException, ParserConfigurationException, SAXException {
        createTestJob();
        client.buildJob("test");
        client.launchJob("test");
        client.unpauseJob("test");
        assertEquals("Active: RUNNING",client.getCurrentJobStatus("test"));
        client.pauseJob("test");
        assertEquals("Active: PAUSED",client.getCurrentJobStatus("test"));
        client.terminateJob("test");
        assertEquals("Finished: ABORTED",client.getCurrentJobStatus("test"));
        assertTrue(client.isJobFinished("test"));
    }

    @Test
    public void testJobProcess() throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException, AuthenticationException, InterruptedException {
        createTestJob();
        client.buildJob("test");
        client.launchJob("test");
        client.unpauseJob("test");
        assertEquals("Active: RUNNING",client.getCurrentJobStatus("test"));
        int secondsCounter = 0;
        while(!client.isJobFinished("test")) {
            Thread.sleep(1000);
            secondsCounter ++;
            if(secondsCounter > 120) {
                fail("Crawl job is taking too long (120 seconds)");
            }
            System.out.println(client.getCurrentJobStatus("test"));
        }
        assertEquals("Finished: FINISHED", client.getCurrentJobStatus("test"));
        assertTrue(client.getCrawlLogUri("test").endsWith("PDFReport.txt"));
        assertEquals(client.getDownloadedCount("test"), 6);
    }

    private void createTestJob() {
        ArrayList<String> urls = new ArrayList<>();
        urls.add("http://localhost:8080");
        try {
            client.teardownJob("test");
            client.createJob("test", urls);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception came from test job creation");
        }
    }
}
