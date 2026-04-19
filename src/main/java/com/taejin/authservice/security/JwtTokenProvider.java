package com.taejin.authservice.security;

import com.taejin.authservice.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_ROLES = "roles";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        byte[] bytes = resolveSecretBytes(props.getSecret());
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    private byte[] resolveSecretBytes(String secret) {
        // Base64 디코드를 시도하고 실패하면 raw 바이트 사용. 최소 길이 보장.
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length >= 32) return decoded;
        } catch (Exception ignored) {
            // fall through
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }
        return raw;
    }

    public String createAccessToken(Long userId, String email, List<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.getAccessTokenExpirationMs());
        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.getRefreshTokenExpirationMs());
        // jti(UUID)를 추가해 동일 초에 발급되는 토큰도 고유한 JWT 문자열이 되도록 한다.
        // refresh_tokens.token 컬럼의 UNIQUE 제약 위반(rotation 직후 중복) 방지.
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(props.getIssuer())
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public String getType(String token) {
        Object t = parse(token).get(CLAIM_TYPE);
        return t == null ? null : t.toString();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object r = parse(token).get(CLAIM_ROLES);
        return r instanceof List ? (List<String>) r : List.of();
    }

    public long getRefreshTokenExpirationMs() {
        return props.getRefreshTokenExpirationMs();
    }
}
