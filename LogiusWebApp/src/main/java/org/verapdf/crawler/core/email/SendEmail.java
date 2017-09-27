package org.verapdf.crawler.core.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.configurations.EmailServerConfiguration;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public final class SendEmail {

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

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
}