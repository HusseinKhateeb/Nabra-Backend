package com.nabra.backend.security.websocket;

import com.nabra.backend.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

  public static final String ATTR_USER_ID = "WS_USER_ID";
  private final JwtService jwtService;

  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {

    // 1) Try Authorization header: "Bearer <token>"
    List<String> authHeaders = request.getHeaders().get("Authorization");
    String token = null;

    if (authHeaders != null && !authHeaders.isEmpty()) {
      String v = authHeaders.get(0);
      if (v != null && v.startsWith("Bearer ")) token = v.substring(7);
    }

    // 2) Fallback: token in query param ?token=...
    if (token == null) {
      var params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
      token = params.getFirst("token");
    }

    if (token == null || token.isBlank()) return false;

    try {
      Jws<Claims> parsed = jwtService.parse(token);
      String userId = parsed.getBody().getSubject(); // userId stored as subject
      attributes.put(ATTR_USER_ID, userId);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                             WebSocketHandler wsHandler, Exception exception) {
  }
}
