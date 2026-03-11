package com.nabra.backend.security.jwt;

import com.nabra.backend.security.principal.DbUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final DbUserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

      String token = null;
      String header = request.getHeader("Authorization");
      if (header != null && header.startsWith("Bearer ")) {
        token = header.substring(7);
      }

      // WebSocket handshake: check for token in query string
      if (token == null) {
        String uri = request.getRequestURI();
        if (uri != null && uri.contains("/ws-chat")) {
          String query = request.getQueryString();
          if (query != null) {
            for (String param : query.split("&")) {
              String[] kv = param.split("=");
              if (kv.length == 2 && kv[0].equals("token")) {
                token = kv[1];
                break;
              }
            }
          }
        }
      }

      if (token == null) {
        filterChain.doFilter(request, response);
        return;
      }

      try {
        Jws<Claims> jws = jwtService.parse(token);
        String userId = jws.getBody().getSubject();

        if (userId != null &&
            SecurityContextHolder.getContext().getAuthentication() == null) {

          var principal = userDetailsService.loadById(userId);
          var auth = new UsernamePasswordAuthenticationToken(
              principal,
              null,
              principal.getAuthorities()
          );
          auth.setDetails(
              new WebAuthenticationDetailsSource().buildDetails(request)
          );
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      } catch (Exception ignored) {
        // Token invalid
      }

      filterChain.doFilter(request, response);
  }

  // ❗ تجاهل auth endpoints
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return request.getServletPath().startsWith("/api/v1/auth/");
  }
}
