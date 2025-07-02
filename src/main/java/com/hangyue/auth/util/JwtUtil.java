package com.hangyue.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        //byte[] keyBytes = HexFormat.of().parseHex(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    // 密钥生成器（只需运行一次）
    public static String generateSecureKey() {
        // 生成256位(32字节)的安全密钥
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        // 转换为Base64字符串存储
        return Base64.getEncoder().encodeToString(key.getEncoded());
        // 如果需要十六进制格式（64字符）
        // return HexFormat.of().formatHex(key.getEncoded());
    }
    public static void main(String[] args) {
        // 生成安全密钥（只需运行一次）
        String secureKey = generateSecureKey();
        System.out.println("安全密钥 (Base64): " + secureKey);

    }

    public static String convertTo32Hex(String input) {
        // 验证输入是否为16位字符串
        if (input == null || input.length() != 16) {
            throw new IllegalArgumentException("输入必须是16位长度的字符串");
        }
        // 将字符串转换为字节数组
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        // 使用HexFormat将字节数组转换为32位十六进制字符串
        return HexFormat.of().formatHex(bytes);
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}