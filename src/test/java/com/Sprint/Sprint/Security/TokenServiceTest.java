package com.Sprint.Sprint.Security;

import com.Sprint.Sprint.Entity.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenServiceTest {

    private static final String SECRET = "test-secret";

    private final TokenService tokenService = new TokenService(SECRET);

    private static User user(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    @Test
    void validTokenRoundTripsToUsername() {
        String token = tokenService.generateToken(user("alice"));

        assertEquals("alice", tokenService.validateToken(token));
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        String token = new TokenService("another-secret").generateToken(user("alice"));

        assertEquals("", tokenService.validateToken(token));
    }

    @Test
    void tokenSignedWithHardcodedLiteralSecretIsRejected() {
        String forged = JWT.create()
                .withIssuer("sprint-api")
                .withSubject("admin")
                .withExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .sign(Algorithm.HMAC256("secret"));

        assertEquals("", tokenService.validateToken(forged));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = tokenService.generateToken(user("alice"));
        String[] parts = token.split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"iss\":\"sprint-api\",\"sub\":\"admin\"}".getBytes());
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        assertEquals("", tokenService.validateToken(tampered));
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = JWT.create()
                .withIssuer("sprint-api")
                .withSubject("alice")
                .withExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .sign(Algorithm.HMAC256(SECRET));

        assertEquals("", tokenService.validateToken(expired));
    }

    @Test
    void tokenWithWrongIssuerIsRejected() {
        String token = JWT.create()
                .withIssuer("someone-else")
                .withSubject("alice")
                .withExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .sign(Algorithm.HMAC256(SECRET));

        assertEquals("", tokenService.validateToken(token));
    }

    @Test
    void malformedTokenIsRejected() {
        assertEquals("", tokenService.validateToken("not-a-jwt"));
    }
}
