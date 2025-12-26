package com.nabra.backend.modules.sessionhistory.service;

import com.nabra.backend.common.model.Enums.SessionStatus;
import com.nabra.backend.modules.sessionhistory.dto.SessionDtos;
import com.nabra.backend.modules.sessionhistory.model.Session;
import com.nabra.backend.modules.sessionhistory.repository.SessionRepository;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {

  private final SessionRepository sessionRepository;
  private final UserService userService;

  public SessionDtos.SessionResponse toDto(Session s) {
    return new SessionDtos.SessionResponse(
        s.getId(),
        s.getUser().getId(),
        s.getInputType(),
        s.getOutputType(),
        s.getStatus(),
        s.getStartedAt(),
        s.getEndedAt(),
        s.getDurationSeconds(),
        s.getResultText(),
        s.getResultAudioUrl(),
        s.getAccuracyScore()
    );
  }

  public SessionDtos.SessionResponse start(String userId, SessionDtos.StartSessionRequest req) {
    User user = userService.getById(userId);
    Session s = new Session();
    s.setUser(user);
    s.setInputType(req.inputType());
    s.setOutputType(req.outputType());
    s.setStatus(SessionStatus.ACTIVE);
    s.setStartedAt(Instant.now());
    sessionRepository.save(s);
    return toDto(s);
  }

  public SessionDtos.SessionResponse stop(String userId, String sessionId, SessionDtos.StopSessionRequest req) {
    Session s = sessionRepository.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Session not found"));
    if (!s.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("Access denied");
    }
    s.setEndedAt(Instant.now());
    s.setStatus(SessionStatus.COMPLETED);
    if (s.getStartedAt() != null) {
      s.setDurationSeconds(s.getEndedAt().getEpochSecond() - s.getStartedAt().getEpochSecond());
    }
    if (req.resultText() != null) s.setResultText(req.resultText());
    if (req.resultAudioUrl() != null) s.setResultAudioUrl(req.resultAudioUrl());
    if (req.accuracyScore() != null) s.setAccuracyScore(req.accuracyScore());
    sessionRepository.save(s);
    return toDto(s);
  }

  public Page<SessionDtos.SessionResponse> list(
      String userId,
      Optional<Instant> from,
      Optional<Instant> to,
      Optional<String> outputType,
      Optional<String> keyword,
      Pageable pageable
  ) {

    Specification<Session> spec = (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);

    if (from.isPresent()) {
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startedAt"), from.get()));
    }
    if (to.isPresent()) {
      spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("startedAt"), to.get()));
    }
    if (outputType.isPresent()) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("outputType"), Enum.valueOf(com.nabra.backend.common.model.Enums.SessionOutputType.class, outputType.get())));
    }
    if (keyword.isPresent() && !keyword.get().isBlank()) {
      String like = "%" + keyword.get().toLowerCase() + "%";
      spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("resultText")), like));
    }

    return sessionRepository.findAll(spec, pageable).map(this::toDto);
  }
}
