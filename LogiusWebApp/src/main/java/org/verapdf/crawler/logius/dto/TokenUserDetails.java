package org.verapdf.crawler.logius.dto;


import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TokenUserDetails extends User {
    private String token;

    public TokenUserDetails(String username, String password, String token, boolean enabled,
                            Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.token = token;
    }

    public TokenUserDetails(org.verapdf.crawler.logius.model.User user, String token){
        super(user.getEmail(), user.getPassword(), user.isEnabled(),
                true, true, true,
                getAuthorities(user.getRole().toString()));
        this.token = token;
    }

    public TokenUserDetails(DecodedJWT token){
        super(token.getSubject(), token.getSubject(), token.getClaim("isEnabled").asBoolean(),
                true, true, true,
                getAuthorities(token.getClaim("role").asString()));
        this.token =  token.getToken();
    }

    public String getToken() {
        return token;
    }

    private static  List<SimpleGrantedAuthority> getAuthorities(String role) {
        List<SimpleGrantedAuthority> roles = new ArrayList<>();
        roles.add(new SimpleGrantedAuthority(role));
        return roles;
    }
}