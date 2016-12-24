package org.verapdf.crawler.resources;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.verapdf.crawler.app.LogiusWebApplication;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestCeawlJobResource {
    private static LogiusWebApplication application;

    @BeforeClass
    public static void initialize() throws Exception {
        application = new LogiusWebApplicationStub();
        String[] args = new String[]{"server","src/test/resources/config.yml"};
        application.run(args);
    }

    @Test
    public void testStaticPages() throws IOException {
        checkURL("http://localhost:9002");
        checkURL("http://localhost:9002/email");
        checkURL("http://localhost:9002/jobinfo");
    }

    @Test
    public void testProcess() throws IOException {
        String baseUrl = "http://localhost:9002/crawl-job/";
        URL url = new URL(baseUrl);
        HttpURLConnection conn;
        conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-type","application/json");
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write("{\"domain\":\"http://localhost:8080\"}");
        writer.flush();
        writer.close();
        os.close();
        Assert.assertEquals(conn.getResponseCode(),200);

        url = new URL(baseUrl + "list");
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type","application/json");
        BufferedReader reader = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String response = reader.readLine();
        String id = response.substring(2,38);
        Assert.assertTrue(response.contains("http://localhost:8080"));

        url = new URL(baseUrl + id);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type","application/json");
        reader = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        response = reader.readLine();
        System.out.println(response);
        Assert.assertTrue(response.contains("http://localhost:8080"));
        Assert.assertTrue(response.contains(id));
    }

    @Test
    public void testEmailSetting() throws IOException {
        URL url = new URL("http://localhost:9002/crawl-job/target_email");
        HttpURLConnection conn;
        conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-type","application/json");
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write("{\"emailAddress\":\"test@example.com\"}");
        writer.flush();
        writer.close();
        os.close();
        Assert.assertEquals(conn.getResponseCode(),204);
        conn.disconnect();

        url = new URL("http://localhost:9002/crawl-job/get_target_email");
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        Assert.assertEquals(conn.getResponseCode(), 200);
        BufferedReader reader = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String response = reader.readLine();
        Assert.assertTrue(response.contains("test@example.com"));
    }

    private void checkURL(String address) throws IOException {
        URL url = new URL(address);
        HttpURLConnection conn;
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        Assert.assertEquals(conn.getResponseCode(), 200);
    }
}
