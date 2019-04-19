package org.verapdf.crawler.logius.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.db.UserDao;
import org.verapdf.crawler.logius.dto.user.TokenUserDetails;
import org.verapdf.crawler.logius.model.User;

@Service
public class TokenUserDetailsService implements UserDetailsService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UserDao userDao;
    private TokenService tokenService;

    public TokenUserDetailsService(UserDao userDao, TokenService tokenService) {
        this.userDao = userDao;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Trying to authenticate ", email);
        User user = userDao.getByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("Account for '" + email + "' not found");
        }
        return new TokenUserDetails(user.getId(), user.getEmail(), user.getPassword(), user.isEnabled(), user.isActivated(), tokenService.encode(user), user.getRole());
    }
}
