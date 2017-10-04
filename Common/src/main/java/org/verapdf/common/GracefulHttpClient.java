package org.verapdf.common;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * Custom HttpClient which allows to retry in cases when standard HttpClient does not, e.g. in case of Connection refused.
 * Inspired by org.apache.http.impl.execchain.ServiceUnavailableRetryExec, but unlike it retries also in case of exceptions
 */
public class GracefulHttpClient extends CloseableHttpClient {

    private CloseableHttpClient httpClient = HttpClients.createDefault();

    /**
     * Maximum number of allowed retries if the server responds with a HTTP code
     * in our retry code list. Default value is 1.
     */
    private final int maxRetries;

    /**
     * Retry interval between subsequent requests, in milliseconds. Default
     * value is 1 second.
     */
    private final long retryInterval;

    public GracefulHttpClient(int maxRetries, long retryInterval) {
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        int attempt = 0;
        while (true) {
            long start = System.currentTimeMillis();
            try {
                return httpClient.execute(target, request, context);
            } catch (IOException e) {
                if (attempt++ >= maxRetries) {
                    long timeSpent = System.currentTimeMillis() - start;
                    throw new RetryFailedException(e, attempt, timeSpent);
                }

                if (retryInterval > 0) {
                    try {
                        Thread.sleep(retryInterval);
                    } catch (final InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException();
                    }
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    @Override
    public HttpParams getParams() {
        return httpClient.getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return httpClient.getConnectionManager();
    }
}
