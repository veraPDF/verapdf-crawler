package org.verapdf.crawler.logius.core.email;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SendEmail {
    private static final String SUBJECT = "Crawling finished for %s";
    private static final String EMAIL_VERIFICATION_SUBJECT = "Email verification";
    private static final String EMAIL_VERIFICATION_BODY = "Email verification\nLink: %s";
    private static final String PASSWORD_RESET_SUBJECT = "Password reset";
    private static final String PASSWORD_RESET_BODY = "Password reset\nLink: %s";
    private static final String EMAIL_BODY = "Crawler finished verification of documents on the domain(s): %s";
    private final JavaMailSender emailSender;

    @Value("${logius.reports.notificationEmails}")
    private String[] reportTargetEmails;
    @Value("${logius.uri.path}")
    private String httpPath;
    @Value("${logius.email.from}")
    private String emailFrom;

    @Autowired
    public SendEmail(JavaMailSender mailSender) {
        this.emailSender = mailSender;
    }

    @Async
    public void sendReportNotification(String subject, String text) {
        send(subject, text, reportTargetEmails);
    }

    @Async
    public void sendEmailConfirm(String token, String email) {
        send(EMAIL_VERIFICATION_SUBJECT, String.format(EMAIL_VERIFICATION_BODY, httpPath + "/email-confirm.html?token=" + token), email);
    }

    @Async
    public void sendPasswordResetToken(String token, String email) {
        send(PASSWORD_RESET_SUBJECT, String.format(PASSWORD_RESET_BODY, httpPath + "/password-reset.html?token=" + token), email);
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
        for (String recipientAddress : recipientAddresses) {
            emailSender.send(mimeMessage -> {
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
                helper.setTo(recipientAddress);
                helper.setText(text);
                helper.setSubject(subject);
                helper.setFrom(emailFrom);
            });
        }
    }
}