package org.group1.coffeeshopapi.realtime.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.constant.SecurityConstants;
import org.group1.coffeeshopapi.common.security.CustomUserDetailsService;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.realtime.RealtimeDestinations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

// Authenticates the STOMP CONNECT frame with the same JWT rules as JwtAuthFilter, and checks
// who may subscribe to what.
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Set<String> STAFF_ROLES = Set.of("ROLE_ADMIN", "ROLE_BARISTA", "ROLE_SUPER_ADMIN");
    private static final Set<String> ADMIN_ROLES = Set.of("ROLE_ADMIN", "ROLE_SUPER_ADMIN");

    // Who may subscribe to each topic. Anything not listed here (or under /user/) is refused.
    private static final Map<String, Set<String>> TOPIC_ROLES = Map.of(
            RealtimeDestinations.STAFF_ORDERS, STAFF_ROLES,
            RealtimeDestinations.INVENTORY, STAFF_ROLES,
            RealtimeDestinations.STAFF_CALLS, STAFF_ROLES,
            RealtimeDestinations.FEEDBACK, ADMIN_ROLES,
            RealtimeDestinations.CATALOG, Set.of());

    public static final String SESSION_TOKEN_ID = "tokenId";
    public static final String SESSION_TOKEN_EXPIRES_AT = "tokenExpiresAt";
    public static final String SESSION_EMAIL = "email";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            accessor.setUser(authenticate(accessor));
        } else if (command == StompCommand.SUBSCRIBE) {
            authorizeSubscribe(accessor.getUser(), accessor.getDestination());
        } else if (command == StompCommand.SEND) {
            // Nothing is handled on /app yet — the socket is push-only.
            throw new MessageDeliveryException("Sending messages is not supported");
        }
        return message;
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(SecurityConstants.JWT_HEADER);
        if (header == null || !header.startsWith(SecurityConstants.JWT_PREFIX)) {
            throw new MessageDeliveryException("Missing bearer token");
        }
        try {
            Claims claims = jwtUtil.extractClaims(header.substring(SecurityConstants.JWT_PREFIX.length()));
            if (!jwtUtil.isAccessToken(claims)
                    || Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.JWT_DENYLIST_PREFIX + claims.getId()))) {
                throw new MessageDeliveryException("Invalid token");
            }
            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
            if (!userDetails.isEnabled()) {
                throw new MessageDeliveryException("Account is not active");
            }
            // Kept on the session so WebSocketSessionSweeper can close it once the token is no
            // longer valid.
            Map<String, Object> attributes = accessor.getSessionAttributes();
            if (attributes != null) {
                attributes.put(SESSION_TOKEN_ID, claims.getId());
                attributes.put(SESSION_TOKEN_EXPIRES_AT, claims.getExpiration().toInstant());
                attributes.put(SESSION_EMAIL, claims.getSubject());
            }
            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            throw new MessageDeliveryException("Invalid token");
        }
    }

    private void authorizeSubscribe(Principal user, String destination) {
        if (!(user instanceof Authentication auth) || !auth.isAuthenticated()) {
            throw new MessageDeliveryException("Not authenticated");
        }
        if (destination == null) {
            throw new MessageDeliveryException("Missing destination");
        }
        // Spring resolves /user/** to this session's own queue, so any signed-in user may use it.
        if (destination.startsWith("/user/")) {
            return;
        }
        Set<String> allowedRoles = TOPIC_ROLES.get(destination);
        if (allowedRoles != null && (allowedRoles.isEmpty() || hasAnyRole(auth, allowedRoles))) {
            return;
        }
        throw new MessageDeliveryException("Not allowed to subscribe to " + destination);
    }

    private boolean hasAnyRole(Authentication auth, Set<String> roles) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (roles.contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
