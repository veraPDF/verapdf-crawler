package org.verapdf.crawler.logius.service;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.model.User;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class TokenService {
    private final UserService userService;
    @Value("${logius.jwtExpirationInMs}")
    private int jwtExpirationInMs;
    @Value("${logius.jwtSecret}")
    private String jwtSecret;

    @Autowired
    public TokenService(UserService userService) {
        this.userService = userService;
    }

    public DecodedJWT decode(String token) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret + userService.getUserSecretByEmail(getUserEmailFromJWT(token)));
        JWTVerifier verifier = JWT.require(algorithm).acceptExpiresAt(0).build();
        return verifier.verify(token);
    }

    public String encode(User user) {
        try {
            LocalDateTime now = LocalDateTime.now();
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret + user.getSecret());
            return JWT.create()
                    .withSubject(user.getEmail())
                    .withIssuedAt(Date
                            .from(now.atZone(ZoneId.systemDefault())
                                    .toInstant()))
                    .withExpiresAt(Date
                            .from(now.plusSeconds(jwtExpirationInMs)
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()))
                    .withClaim("role", user.getRole().toString())
                    .withClaim("isEnabled", user.isEnabled())
                    .sign(algorithm);
        } catch (JWTCreationException ex) {
            throw new IllegalArgumentException("Cannot properly create token", ex);
        }
    }

    private String getUserEmailFromJWT(String token) {
        return JWT.decode(token).getSubject();
    }
}