package org.verapdf.crawler.logius.service;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.model.User;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class TokenService {
    @Value("${logius.jwtExpirationInSeconds}")
    private int jwtExpirationInMs;
    @Value("${logius.jwtPasswordResetExpirationInSeconds}")
    private int jwtPasswordResetExpirationInMs;
    private byte[] jwtSecret;

    @Autowired
    public TokenService(@Value("${logius.jwtSecret}") String jwtSecret) {
        this.jwtSecret = jwtSecret.getBytes();
    }

    public DecodedJWT verify(String token, byte[] userSecret) {
        Algorithm algorithm = Algorithm.HMAC256(mergeSecrets(jwtSecret, userSecret));
        return JWT.require(algorithm).build().verify(token);
    }

    private String encode(User user, int jwtExpirationInMs, String... scopes){
        try {
            Instant now = Instant.now();
            Algorithm algorithm = Algorithm.HMAC256(mergeSecrets(jwtSecret, user.getSecret()));
            return JWT.create()
                    .withSubject(user.getEmail())
                    .withIssuedAt(Date.from(now))
                    .withExpiresAt(Date.from(now.plusSeconds(jwtExpirationInMs)))
                    .withArrayClaim("scope", scopes)
                    .sign(algorithm);
        } catch (JWTCreationException ex) {
            throw new IllegalArgumentException("Cannot properly create token", ex);
        }
    }

    public String encode(User user) {
        return encode(user, jwtExpirationInMs);
    }

    public String encodePasswordToken(User user) {
        return encode(user, jwtExpirationInMs, "RESET_PASSWORD");
    }

    public String encodeEmailVerificationToken(User user) {
        return encode(user, jwtExpirationInMs, "EMAIL_VERIFICATION");
    }

    public DecodedJWT decode(String token){
        return JWT.decode(token);
    }

    public String getSubject(DecodedJWT token) {
        return token.getSubject();
    }

    public String[] getScopes(String token) {
        return JWT.decode(token).getClaim("scope").asArray(String.class);
    }

    private byte[] mergeSecrets(byte[] secret, byte[] userSecret) {
        byte[] concatBytes = ArrayUtils.addAll(secret, userSecret);
        return DigestUtils.sha256(concatBytes);
    }
}