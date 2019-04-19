package org.verapdf.crawler.logius.configurations.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.verapdf.crawler.logius.service.TokenAuthenticationUserDetailsService;
import org.verapdf.crawler.logius.service.TokenUserDetailsService;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecureConfig {
    @Bean
    public AuthHandler authHandler(ObjectMapper mapper) {
        return new AuthHandler(mapper);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Configuration
    @Order(1)
    public class BasicAuthConfig extends WebSecurityConfigurerAdapter {
        private final TokenUserDetailsService userDetailsService;
        private final AuthHandler authHandler;

        public BasicAuthConfig(TokenUserDetailsService userDetailsService, AuthHandler authHandler) {
            this.userDetailsService = userDetailsService;
            this.authHandler = authHandler;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .antMatcher("/api/auth/token")
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                    .httpBasic()
                    .authenticationEntryPoint(authHandler)
                    .and()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and().csrf().disable();
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        }
    }


    @Configuration
    @Order(2)
    public class TokenAuthConfig extends WebSecurityConfigurerAdapter {
        private final TokenAuthenticationUserDetailsService service;
        private final AuthHandler authHandler;

        @Autowired
        public TokenAuthConfig(TokenAuthenticationUserDetailsService service, AuthHandler authHandler) {
            this.service = service;
            this.authHandler = authHandler;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .antMatchers("/api/user/password-reset-confirm").hasAuthority("RESET_PASSWORD")
                    .antMatchers("/api/user/email-confirm").hasAuthority("EMAIL_VERIFICATION")
                    .antMatchers("/api/admin/**").access("hasAuthority('GENERAL') and hasAuthority('ADMIN')")
                    .antMatchers("/api/**").access("hasAuthority('GENERAL') or isAnonymous()")
                    .and()
                    .addFilterBefore(authFilter(), RequestHeaderAuthenticationFilter.class)
                    .authenticationProvider(preAuthProvider()).exceptionHandling().accessDeniedHandler(authHandler)
                    .authenticationEntryPoint(authHandler)
                    .and()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .csrf().disable();
        }

        @Bean
        public TokenAuthFilter authFilter() throws Exception {
            TokenAuthFilter filter = new TokenAuthFilter();
            filter.setAuthenticationManager(authenticationManagerBean());
            return filter;
        }

        @Bean
        public AuthenticationProvider preAuthProvider() {
            PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
            provider.setPreAuthenticatedUserDetailsService(service);
            return provider;
        }

        @Bean
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }
    }
}