package org.verapdf.crawler.logius.core.email;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SendEmail {
    private static final Logger logger = LoggerFactory.getLogger(SendEmail.class);
    private static final String SUBJECT = "Crawling finished for %s";
    private static final String EMAIL_BODY = "Crawler finished verification of documents on the domain(s): %s";
    private final JavaMailSender emailSender;
    @Qualifier("reportTargetEmails")
    private String[] reportTargetEmails;

    @Autowired
    public SendEmail(JavaMailSender mailSender, String[] reportTargetEmails) {
        this.emailSender = mailSender;
        this.reportTargetEmails = reportTargetEmails;
    }

    @Async
    public void sendReportNotification(String subject, String text) {
        send(subject, text, reportTargetEmails);
    }

    @Async
    public void sendFinishNotification(CrawlRequest request) {
        String emailAddress = request.getEmailAddress();
        String domainsString = request.getCrawlJobs() == null ? "" : generateDomainsString(request.getCrawlJobs());
        String subject = String.format(SUBJECT, domainsString);
        String body = String.format(EMAIL_BODY, domainsString);
        send(subject, body, emailAddress);
    }

    private String generateDomainsString(List<CrawlJob> crawlJobs) {
        return crawlJobs.stream().map(CrawlJob::getDomain).collect(Collectors.joining(", "));
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

                messages[i] = message;
            }
            emailSender.send(messages);
        } catch (MessagingException e) {
            logger.error("Email sending error at addresses " + String.join(", ", recipientAddresses), e);
        }

    }
}