package org.verapdf.crawler.logius.configurations.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.verapdf.crawler.logius.service.TokenAuthenticationUserDetailsService;
import org.verapdf.crawler.logius.service.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        prePostEnabled = true
)
public class SecureConfig {

    @Configuration
    @Order(1)
    public class BasicAuthConfig extends WebSecurityConfigurerAdapter {
        private final UserDetailsServiceImpl userDetailsServiceImpl;
        private final PasswordEncoder passwordEncoder;
        private final AuthEntryPoint authEntryPoint;

        public BasicAuthConfig(UserDetailsServiceImpl userDetailsServiceImpl, PasswordEncoder passwordEncoder, AuthEntryPoint authEntryPoint) {
            this.userDetailsServiceImpl = userDetailsServiceImpl;
            this.passwordEncoder = passwordEncoder;
            this.authEntryPoint = authEntryPoint;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .antMatcher("/api/auth/token")
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                    .httpBasic()
                    .authenticationEntryPoint(authEntryPoint)
                    .and()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.userDetailsService(userDetailsServiceImpl).passwordEncoder(passwordEncoder);
        }
    }


    @Configuration
    @Order(2)
    public class TokenAuthConfig extends WebSecurityConfigurerAdapter {
        private final AuthEntryPoint authEntryPoint;
        private TokenAuthenticationUserDetailsService service;

        @Autowired
        public TokenAuthConfig(TokenAuthenticationUserDetailsService service, AuthEntryPoint authEntryPoint) {
            this.service = service;
            this.authEntryPoint = authEntryPoint;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .authorizeRequests().antMatchers("/api/admin/**").hasAuthority("ADMIN")
                    .and()
                    .addFilterBefore(authFilter(), RequestHeaderAuthenticationFilter.class)
                    .authenticationProvider(preAuthProvider()).exceptionHandling()
                    .authenticationEntryPoint(authEntryPoint)
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

        @Bean(BeanIds.AUTHENTICATION_MANAGER)
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }
    }
}