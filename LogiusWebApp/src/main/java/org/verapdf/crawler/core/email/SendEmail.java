package org.verapdf.crawler.core.email;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.api.crawling.CrawlRequest;
import org.verapdf.crawler.configurations.EmailServerConfiguration;

import java.util.List;

public final class SendEmail {

    private static final Logger logger = LoggerFactory.getLogger(SendEmail.class);

    private static final String SUBJECT = "Crawling finished for %s";
    private static final String EMAIL_BODY = "Crawler finished verification of documents on the domain(s): %s";
    private static final String DOMAIN_SEPARATOR = ", ";

    private SendEmail() {
    }

    public static void send(String targetEmail, String subject, String text, EmailServerConfiguration emailServerConfiguration) {
        Email email = new SimpleEmail();
        email.setHostName(emailServerConfiguration.getHost());
        email.setSmtpPort(emailServerConfiguration.getPort());
        email.setAuthenticator(new DefaultAuthenticator(
                emailServerConfiguration.getUser(),
                emailServerConfiguration.getPassword()));
        email.setSSLOnConnect(true);
        try {
            email.setFrom(emailServerConfiguration.getAddress());
            email.setSubject(subject);
            email.setMsg(text);
            email.addTo(targetEmail);
            email.send();
        } catch (EmailException e) {
            logger.error("Email sending error at address " + targetEmail, e);
        }

    }

    public static void sendFinishNotification(CrawlRequest request, EmailServerConfiguration serverConfiguration) {
        String emailAddress = request.getEmailAddress();
        String domainsString = generateDomainsString(request.getCrawlJobs());
        String subject = String.format(SUBJECT, domainsString);
        String body = String.format(EMAIL_BODY, domainsString);
        send(emailAddress, subject, body, serverConfiguration);
    }

    private static String generateDomainsString(List<CrawlJob> crawlJobs) {
        StringBuilder builder = new StringBuilder();
        if (crawlJobs != null && !crawlJobs.isEmpty()) {
            for (CrawlJob crawlJob : crawlJobs) {
                builder.append(crawlJob.getDomain()).append(DOMAIN_SEPARATOR);
            }
        }
        return builder.substring(0, builder.length() - DOMAIN_SEPARATOR.length());
    }
}