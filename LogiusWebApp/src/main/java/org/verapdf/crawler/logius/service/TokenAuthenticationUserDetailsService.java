package org.verapdf.crawler.logius.service;


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
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken authentication) throws UsernameNotFoundException {
        if (authentication.getPrincipal() != null && authentication.getPrincipal() instanceof String && authentication.getCredentials() instanceof String) {
            try {
                String token = (String) authentication.getPrincipal();
                User user = userDao.getByEmail(tokenService.getSubject(token));
                tokenService.verify(token, user.getSecret());
                return new TokenUserDetails(user.getId(), user.getEmail(), user.getPassword(), user.isEnabled(), user.getRole(), token);

            } catch (Exception ex) {
                throw new UsernameNotFoundException("Token has been expired", ex);
            }

        } else {
            throw new UsernameNotFoundException("Could not retrieve user details for " + authentication.getPrincipal());
        }
    }
}