package org.verapdf.crawler.core.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.crawling.CrawlRequest;
import org.verapdf.crawler.configurations.EmailServerConfiguration;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;

public final class SendEmail {

    private static final Logger logger = LoggerFactory.getLogger(SendEmail.class);

    private static final String SUBJECT = "Crawling finished for %s";
    private static final String EMAIL_BODY = "Crawler finished verification of documents on the domain(s): %s";
    private static final String DOMAIN_SEPARATOR = ", ";

    private SendEmail() {
    }

    public static void send(String targetEmail, String subject, String text, EmailServerConfiguration emailServerConfiguration) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.port", emailServerConfiguration.getPort());
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.transport.protocol", "smtp");
        properties.setProperty("mail.smtp.host", emailServerConfiguration.getHost());
        Session session = Session.getDefaultInstance(properties, new GMailAuthenticator(emailServerConfiguration.getUser(), emailServerConfiguration.getPassword()));

        try {
            Transport transport = session.getTransport();
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailServerConfiguration.getAddress()));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(targetEmail));
            message.setSubject(subject);
            message.setText(text);

            transport.connect();
            Transport.send(message);
            transport.close();
            logger.info("Notification email was sent at" + targetEmail);
        }catch (MessagingException mex) {
            logger.error("Email sending error at address " + targetEmail, mex);
        }
    }

    public static void sendFinishNotification(CrawlRequest request, EmailServerConfiguration serverConfiguration) {
        String emailAddress = request.getEmailAddress();
        List<String> domains = request.getDomains();
        String domainsString = generatedomainsString(domains);
        String subject = String.format(SUBJECT, domainsString);
        String body = String.format(EMAIL_BODY, domainsString);
        send(emailAddress, subject, body, serverConfiguration);
    }

    private static String generatedomainsString(List<String> domains) {
        StringBuilder builder = new StringBuilder();
        if (domains != null && !domains.isEmpty()) {
            for (String domain : domains) {
                builder.append(domain).append(DOMAIN_SEPARATOR);
            }
        }
        return builder.substring(0, builder.length() - DOMAIN_SEPARATOR.length());
    }
}