package com.nabra.backend.security.principal;

import com.nabra.backend.modules.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepository.findByUsername(username)
        .map(UserPrincipal::new)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  public UserPrincipal loadById(String userId) {
    return userRepository.findById(userId)
        .map(UserPrincipal::new)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}
