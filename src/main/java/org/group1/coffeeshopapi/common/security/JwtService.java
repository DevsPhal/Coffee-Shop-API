package org.group1.coffeeshopapi.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails, ACCESS_TOKEN_TYPE, expiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, REFRESH_TOKEN_TYPE, refreshExpiration);
    }

    private String buildToken(UserDetails userDetails, String tokenType, long ttlMillis) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claims(Map.of(TOKEN_TYPE_CLAIM, tokenType))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /** True only for a well-formed, unexpired access token - never a refresh token. */
    public boolean isAccessToken(String token) {
        return isParsableAndUnexpired(token) && ACCESS_TOKEN_TYPE.equals(extractTokenType(token));
    }

    /** True only for a well-formed, unexpired refresh token - never an access token. */
    public boolean isRefreshToken(String token) {
        return isParsableAndUnexpired(token) && REFRESH_TOKEN_TYPE.equals(extractTokenType(token));
    }

    private boolean isParsableAndUnexpired(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (DecodingException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
