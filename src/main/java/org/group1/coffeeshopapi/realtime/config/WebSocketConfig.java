package org.group1.coffeeshopapi.realtime.config;

import org.group1.coffeeshopapi.common.properties.CorsProperties;
import org.group1.coffeeshopapi.realtime.security.StompAuthChannelInterceptor;
import org.group1.coffeeshopapi.realtime.security.WebSocketSessionSweeper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

// STOMP over WebSocket at /ws. Clients connect with an "Authorization: Bearer <token>" STOMP
// header, then subscribe to the topics in RealtimeDestinations.
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String ENDPOINT = "/ws";

    private final CorsProperties corsProperties;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final WebSocketSessionSweeper webSocketSessionSweeper;
    private final TaskScheduler brokerTaskScheduler;

    public WebSocketConfig(CorsProperties corsProperties,
                           StompAuthChannelInterceptor stompAuthChannelInterceptor,
                           WebSocketSessionSweeper webSocketSessionSweeper,
                           @Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler brokerTaskScheduler) {
        this.corsProperties = corsProperties;
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.webSocketSessionSweeper = webSocketSessionSweeper;
        this.brokerTaskScheduler = brokerTaskScheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = corsProperties.getAllowedOrigins().toArray(String[]::new);
        registry.addEndpoint(ENDPOINT).setAllowedOrigins(origins);
        // SockJS fallback for networks that block raw WebSocket.
        registry.addEndpoint(ENDPOINT + "/sockjs").setAllowedOrigins(origins).withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Heartbeats let both sides notice a dead connection within ~10s.
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {10_000, 10_000})
                .setTaskScheduler(brokerTaskScheduler);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(webSocketSessionSweeper);
    }
}
