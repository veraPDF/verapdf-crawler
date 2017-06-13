package org.verapdf.crawler.app.email_utils;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;


public class GMailAuthenticator extends Authenticator {
    private final String user;
    private final String pw;
    public GMailAuthenticator (String username, String password)
    {
        super();
        this.user = username;
        this.pw = password;
    }
    public PasswordAuthentication getPasswordAuthentication()
    {
        return new PasswordAuthentication(user, pw);
    }
}