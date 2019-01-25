package org.verapdf.crawler.logius.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.MimeMessagePreparator;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

@Configuration
public class EmailConfiguration {

    @Bean(name = "reportTargetEmails")
    public String[] reportTargetEmails(@Value("${logius.reports.notificationEmails}") String emails) {
        return emails.split(", ");
    }
}
