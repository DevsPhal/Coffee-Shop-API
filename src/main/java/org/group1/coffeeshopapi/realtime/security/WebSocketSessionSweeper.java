package org.group1.coffeeshopapi.realtime.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.security.CustomUserDetailsService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// A socket outlives the access token it connected with, so this closes any session whose token
// has expired or been revoked (logout), or whose account is no longer active. The client should
// refresh its token and reconnect when it sees close code 4001.
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionSweeper implements WebSocketHandlerDecoratorFactory {

    public static final CloseStatus TOKEN_INVALID = new CloseStatus(4001, "Token expired or revoked");

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessions.put(session.getId(), session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                sessions.remove(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }

    @Scheduled(fixedDelay = 30_000)
    public void closeInvalidSessions() {
        Instant now = Instant.now();
        // One account check per user per sweep, however many tabs they have open.
        Map<String, Boolean> activeByEmail = new HashMap<>();
        for (WebSocketSession session : sessions.values()) {
            Map<String, Object> attributes = session.getAttributes();
            Object expiresAt = attributes.get(StompAuthChannelInterceptor.SESSION_TOKEN_EXPIRES_AT);
            if (expiresAt == null) {
                // Not past STOMP CONNECT yet.
                continue;
            }
            String tokenId = (String) attributes.get(StompAuthChannelInterceptor.SESSION_TOKEN_ID);
            String email = (String) attributes.get(StompAuthChannelInterceptor.SESSION_EMAIL);
            boolean valid = now.isBefore((Instant) expiresAt)
                    && !isRevoked(tokenId)
                    && activeByEmail.computeIfAbsent(email, this::isActive);
            if (!valid) {
                close(session);
            }
        }
    }

    private boolean isRevoked(String tokenId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.JWT_DENYLIST_PREFIX + tokenId));
        } catch (RuntimeException ex) {
            // Redis down — don't kick everyone out over it; the token expiry still applies.
            log.warn("Could not check token denylist for WebSocket session", ex);
            return false;
        }
    }

    private boolean isActive(String email) {
        try {
            return userDetailsService.loadUserByUsername(email).isEnabled();
        } catch (UsernameNotFoundException ex) {
            return false;
        }
    }

    private void close(WebSocketSession session) {
        sessions.remove(session.getId());
        try {
            session.close(TOKEN_INVALID);
        } catch (IOException ex) {
            log.debug("WebSocket session {} was already closing", session.getId(), ex);
        }
    }
}
