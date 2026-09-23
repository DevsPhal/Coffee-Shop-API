package org.group1.coffeeshopapi.realtime.security;

import org.group1.coffeeshopapi.common.security.CustomUserDetailsService;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.realtime.RealtimeDestinations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private MessageChannel channel;
    @InjectMocks private StompAuthChannelInterceptor interceptor;

    @Test
    void connectWithoutATokenIsRefused() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void staffCanWatchOrdersAndInventoryButNotFeedback() {
        assertAllowed("ROLE_BARISTA", RealtimeDestinations.STAFF_ORDERS);
        assertAllowed("ROLE_BARISTA", RealtimeDestinations.INVENTORY);
        assertRefused("ROLE_BARISTA", RealtimeDestinations.FEEDBACK);
        assertAllowed("ROLE_ADMIN", RealtimeDestinations.FEEDBACK);
    }

    @Test
    void customersOnlyGetTheCatalogAndTheirOwnQueue() {
        assertAllowed("ROLE_CUSTOMER", RealtimeDestinations.CATALOG);
        assertAllowed("ROLE_CUSTOMER", "/user" + RealtimeDestinations.USER_ORDERS);
        assertRefused("ROLE_CUSTOMER", RealtimeDestinations.STAFF_ORDERS);
        assertRefused("ROLE_CUSTOMER", RealtimeDestinations.INVENTORY);
        // Exact match only — a lookalike topic isn't a way around the rules.
        assertRefused("ROLE_CUSTOMER", RealtimeDestinations.CATALOG + "/../orders");
    }

    @Test
    void subscribingBeforeConnectingIsRefused() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(RealtimeDestinations.CATALOG);

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    private void assertAllowed(String role, String destination) {
        assertThatCode(() -> interceptor.preSend(subscribe(role, destination), channel)).doesNotThrowAnyException();
    }

    private void assertRefused(String role, String destination) {
        assertThatThrownBy(() -> interceptor.preSend(subscribe(role, destination), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    private Message<byte[]> subscribe(String role, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                "user@example.com", null, List.of(new SimpleGrantedAuthority(role))));
        return message(accessor);
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
