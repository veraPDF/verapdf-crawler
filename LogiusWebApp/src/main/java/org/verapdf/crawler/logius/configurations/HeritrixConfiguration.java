package org.verapdf.crawler.logius.configurations;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class HeritrixConfiguration {
    @Value("${heritrix.url}")
    private String url;
    @Value("${heritrix.login}")
    private String login;
    @Value("${heritrix.password}")
    private String password;

    @Bean
    public SSLConnectionSocketFactory sslConnectionSocketFactory() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        return new SSLConnectionSocketFactory(SSLContexts.custom()
                .loadTrustMaterial(null, (x509Certificates, s) -> true)
                .build(), (s, sslSession) -> true);
    }

    @Bean
    public CredentialsProvider credentialsProvider() throws MalformedURLException {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        URL domain = new URL(url);
        HttpHost targetHost = new HttpHost(domain.getHost(), domain.getPort(), "https");
        credsProvider.setCredentials(
                new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                new UsernamePasswordCredentials(login, password));

        return credsProvider;
    }
}
