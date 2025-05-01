package com.fisa.solra.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // JWT 비킬키
    @Value("${jwt.secret}")
    private String secretKey;

    // JWT 만료시간 (밀리초 단위)
    @Value("${jwt.expiration}") // milliseconds
    private long expirationMillis;

    private Key key;

    // 애플리케이션 시작 시 비밀키를 기반으로 Key 초기화
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // 1. JWT 생성
    public String generateToken(Long userId, Long orgId, Long deptId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))            // sub = 사용자 ID
                .claim("orgId", orgId)                      // 사용자 조직 정보
                .claim("deptId", deptId)                    // 사용자 부서 정보
                .claim("role", role)                        // 사용자 역할
                .setIssuedAt(now)                              // 토큰 발급 시각
                .setExpiration(expiry)                         // 토큰 만료 시각
                .signWith(key, SignatureAlgorithm.HS256)       // 비밀키로 서명
                .compact();
    }


    // 2. JWT 검증 유효하면 true, 아니면 false
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 3. JWT 토큰에서 각 정보 추출
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public Long getOrgId(String token) {
        return getClaims(token).get("orgId", Long.class);
    }

    public Long getDeptId(String token) {
        return getClaims(token).get("deptId", Long.class);
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // 내부적으로 Claims(토큰 내용) 파싱
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


}
