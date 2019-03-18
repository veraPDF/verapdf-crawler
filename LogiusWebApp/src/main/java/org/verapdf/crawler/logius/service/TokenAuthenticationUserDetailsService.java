package org.verapdf.crawler.logius.service;


import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.dto.TokenUserDetails;

@Service
public class TokenAuthenticationUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private TokenService tokenService;

    @Autowired
    public TokenAuthenticationUserDetailsService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken authentication) throws UsernameNotFoundException {
        if (authentication.getPrincipal() != null && authentication.getPrincipal() instanceof String && authentication.getCredentials() instanceof String) {
            try {
                DecodedJWT token = tokenService.decode((String) authentication.getPrincipal());
                return new TokenUserDetails(token);
            } catch (Exception ex) {
                throw new UsernameNotFoundException("Token has been expired", ex);
            }

        } else {
            throw new UsernameNotFoundException("Could not retrieve user details for " + authentication.getPrincipal());
        }
    }
}