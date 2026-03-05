package com.nabra.backend.config;

import com.nabra.backend.security.websocket.JwtHandshakeInterceptor;
import com.nabra.backend.security.websocket.UserIdHandshakeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
  private final UserIdHandshakeHandler userIdHandshakeHandler;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-chat")
        .setHandshakeHandler(userIdHandshakeHandler)
        .addInterceptors(jwtHandshakeInterceptor)
        .setAllowedOriginPatterns("*")
        .withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/app");
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setUserDestinationPrefix("/user");
  }
}
