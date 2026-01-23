package com.nabra.backend.security.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class UserIdHandshakeHandler extends DefaultHandshakeHandler {

  @Override
  protected Principal determineUser(ServerHttpRequest request,
                                    WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) {

    Object userId = attributes.get(JwtHandshakeInterceptor.ATTR_USER_ID);
    String name = userId == null ? "anonymous" : userId.toString();
    return () -> name; // Principal.getName() = userId
  }
}
