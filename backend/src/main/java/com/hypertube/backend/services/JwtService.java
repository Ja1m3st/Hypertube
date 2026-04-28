package com.hypertube.backend.services;

import com.hypertube.backend.models.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    
    @Value("${hypertube.security.jwt.secret-key}")
    private String secretKey;

    @Value("${hypertube.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${hypertube.security.jwt.reset-password-expiration:900000}") 
    private long resetPasswordExpiration;

    public String generateToken(User user) {
        return generateToken(new HashMap<>(), user);
    }

    public String generateToken(Map<String, Object> extraClaims, User user) {
        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(String.valueOf(user.getId())) 
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String generatePasswordResetToken(User user) {
        return generateToken(new HashMap<>(), user);
    }

    public String generatePasswordResetToken(Map<String, Object> extraClaims, User user) {
        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(String.valueOf(user.getId())) 
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + resetPasswordExpiration))
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return (Keys.hmacShaKeyFor(keyBytes));
    }

    public Long extractUserId(String token) {
        String subject = extractClaim(token, Claims::getSubject);
        return Long.parseLong(subject);
    }

    public boolean isTokenValid(String token, User user) {
        final Long tokenId = extractUserId(token);
        return ((tokenId.equals(user.getId())) && !isTokenExpired(token));
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return (extractClaim(token, Claims::getExpiration));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSignInKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
