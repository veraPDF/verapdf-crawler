package org.verapdf.crawler.logius.core.email;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;

@Service
public class SendEmail {
    private static final Logger logger = LoggerFactory.getLogger(SendEmail.class);
    private static final String SUBJECT = "Crawling finished for %s";
    private static final String EMAIL_BODY = "Crawler finished verification of documents on the domain(s): %s";
    private static final String DOMAIN_SEPARATOR = ", ";
    private final JavaMailSender emailSender;
    @Value("${spring.mail.from}")
    private String from;
    @Value("#{'${reports.notificationEmails}'.split(',')}")
    private String[] reportTargetEmails;

    @Autowired
    public SendEmail(JavaMailSender mailSender) {
        this.emailSender = mailSender;
    }


    public void sendReportNotification(String subject, String text) {
        send(subject, text, reportTargetEmails);
    }

    @Async
    public void sendFinishNotification(CrawlRequest request) {
        String emailAddress = request.getEmailAddress();
        String domainsString = generateDomainsString(request.getCrawlJobs());
        String subject = String.format(SUBJECT, domainsString);
        String body = String.format(EMAIL_BODY, domainsString);
        send(subject, body, emailAddress);
    }

    private String generateDomainsString(List<CrawlJob> crawlJobs) {
        StringBuilder builder = new StringBuilder();
        if (crawlJobs != null && !crawlJobs.isEmpty()) {
            for (CrawlJob crawlJob : crawlJobs) {
                builder.append(crawlJob.getDomain()).append(DOMAIN_SEPARATOR);
            }
        }
        return builder.substring(0, builder.length() - DOMAIN_SEPARATOR.length());
    }


    private void send(String subject, String text, String... recipientAddresses) {
        try {
            MimeMessage[] messages = new MimeMessage[recipientAddresses.length];
            for (int i = 0; i < recipientAddresses.length; i++) {
                MimeMessage message = emailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message);
                helper.setTo(recipientAddresses[i]);
                helper.setText(text);
                helper.setSubject(subject);
                helper.setFrom(from);

                messages[i] = message;
            }
            emailSender.send(messages);
        } catch (MessagingException e) {
            StringBuilder builder = new StringBuilder();
            for (String target : recipientAddresses) {
                builder.append(target).append(", ");
            }
            int length = builder.length();
            String res = length > 0 ? builder.substring(0, length - 1) : "";
            logger.error("Email sending error at addresses " + res, e);
        }

    }
}