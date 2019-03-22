package org.verapdf.crawler.logius.dto;


import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

public class TokenUserDetails extends User {
    private String token;

    public TokenUserDetails(String username, String password, boolean enabled, org.verapdf.crawler.logius.model.User.Roles role, String token) {
        super(username, password, enabled, true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority(role.toString())));
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}