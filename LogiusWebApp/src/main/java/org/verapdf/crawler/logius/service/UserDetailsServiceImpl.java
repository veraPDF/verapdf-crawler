package org.verapdf.crawler.logius.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.dto.TokenUserDetails;
import org.verapdf.crawler.logius.model.User;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UserService userService;
    private TokenService tokenService;

    @Autowired
    public UserDetailsServiceImpl(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Trying to authenticate ", email);
        try {
            User user = userService.findUserByEmail(email);
            return new TokenUserDetails(user, tokenService.encode(user));
        } catch (UsernameNotFoundException ex) {
            throw new UsernameNotFoundException("Account for '" + email + "' not found", ex);
        }
    }
}
