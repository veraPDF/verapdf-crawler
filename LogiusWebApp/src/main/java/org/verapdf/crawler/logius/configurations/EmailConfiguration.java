//package org.verapdf.crawler.configurations.newc;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.JavaMailSenderImpl;
//
//import java.util.Properties;
//
//@Configuration
//static class EmailConfiguration {
//
//    @Value("${spring.mail.host}")
//    private String host;
//    @Value("${spring.mail.port}")
//    private int port;
//    @Value("${spring.mail.port}")
//    private String username;
//    @Value("${spring.mail.password}")
//    private String password;
//    @Value("${spring.mail.properties.mail.smtp.auth}")
//    private String auth;
//    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
//    private String starttls;
//    @Value("${spring.mail.transport.protocol}")
//    private String protocol;
//    @Value("${spring.mail.debug}")
//    private String debug;
//    @Value("${spring.mail.from}")
//    private String from;
//
//    @Bean
//    static JavaMailSender javaMailSender() {
//        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
//        mailSender.setHost(host);
//        mailSender.setPort(port);
//        mailSender.setUsername(username);
//        mailSender.setPassword(password);
//
//        Properties props = new Properties();
//        props.put("mail.transport.protocol", protocol);
//        props.put("mail.smtp.auth", auth);
//        props.put("mail.smtp.starttls.enable", starttls);
//        props.put("mail.debug", debug);
//        mailSender.setJavaMailProperties(props);
//        return mailSender;
//    }
//}
