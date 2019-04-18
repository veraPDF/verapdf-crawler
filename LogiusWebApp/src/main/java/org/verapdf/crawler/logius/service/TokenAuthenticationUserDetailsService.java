package org.verapdf.crawler.logius.service;


import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.db.UserDao;
import org.verapdf.crawler.logius.dto.user.TokenUserDetails;
import org.verapdf.crawler.logius.model.User;
import org.verapdf.crawler.logius.tools.SecretKeyUtils;

import java.util.Arrays;
import java.util.stream.Stream;

@Service
public class TokenAuthenticationUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private final TokenService tokenService;
    private final UserDao userDao;

    @Autowired
    public TokenAuthenticationUserDetailsService(TokenService tokenService, UserDao userDao) {
        this.tokenService = tokenService;
        this.userDao = userDao;
    }

    @Transactional
    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken authentication) {
        if (authentication.getPrincipal() != null
                && authentication.getPrincipal() instanceof String
                && authentication.getCredentials() instanceof String) {
            try {
                String token = (String) authentication.getPrincipal();
                DecodedJWT decodedToken = tokenService.decode(token);
                User user = userDao.getByEmail(tokenService.getSubject(decodedToken));
                tokenService.verify(token, user.getSecret());
                String[] scopes = tokenService.getScopes(token);
                boolean isActivated = Arrays.asList(scopes).contains("EMAIL_VERIFICATION") || user.isActivated();
                return new TokenUserDetails(user.getId(), user.getEmail(), user.getPassword(),
                        user.isEnabled(), isActivated, token, user.getRole(), tokenService.getScopes(token));

            } catch (Exception ex) {
                throw new UsernameNotFoundException("Token has been expired", ex);
            }
        } else {
            throw new UsernameNotFoundException("Could not retrieve user details for " + authentication.getPrincipal());
        }
    }
}