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
import org.verapdf.crawler.configurations.ReportsConfiguration;

import java.util.List;

public final class SendEmail {

    private static final Logger logger = LoggerFactory.getLogger(SendEmail.class);

    private static final String SUBJECT = "Crawling finished for %s";
    private static final String EMAIL_BODY = "Crawler finished verification of documents on the domain(s): %s";
    private static final String DOMAIN_SEPARATOR = ", ";

    private static EmailServerConfiguration config;
    private static String[] reportTragetEmails;

    private SendEmail() {
    }

    public static void initialize(EmailServerConfiguration configuration, ReportsConfiguration reportsConfiguration) {
        config = configuration;
        String emailsFromConfig = reportsConfiguration.getNotificationEmails();
        reportTragetEmails = emailsFromConfig == null ? null : emailsFromConfig.split(",");
    }

    public static void sendReportNotification(String subject, String text) {
        send(reportTragetEmails, subject, text);
    }

    public static void send(String targetEmail, String subject, String text) {
        send(new String[]{targetEmail}, subject, text);
    }

    public static void send(String[] targetEmail, String subject, String text) {
        if (config == null) {
            throw new IllegalStateException("Initialization fail. Configuration has not been set");
        }
        Email email = new SimpleEmail();
        email.setHostName(config.getHost());
        email.setSmtpPort(config.getPort());
        email.setAuthenticator(new DefaultAuthenticator(
                config.getUser(),
                config.getPassword()));
        email.setSSLOnConnect(true);
        try {
            email.setFrom(config.getAddress());
            email.setSubject(subject);
            email.setMsg(text);
            email.addTo(targetEmail);
            email.send();
        } catch (EmailException e) {
            StringBuilder builder = new StringBuilder();
            for (String target : targetEmail) {
                builder.append(target).append(",");
            }
            int length = builder.length();
            String res = length > 0 ? builder.substring(0, length -1) : "";
            logger.error("Email sending error at addresses " + res, e);
        }
    }

    public static void sendFinishNotification(CrawlRequest request) {
        String emailAddress = request.getEmailAddress();
        String domainsString = generateDomainsString(request.getCrawlJobs());
        String subject = String.format(SUBJECT, domainsString);
        String body = String.format(EMAIL_BODY, domainsString);
        send(emailAddress, subject, body);
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