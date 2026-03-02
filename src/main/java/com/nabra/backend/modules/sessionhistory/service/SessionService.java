package com.nabra.backend.modules.sessionhistory.service;

import com.nabra.backend.common.exception.BadRequestException;
import com.nabra.backend.common.exception.ForbiddenException;
import com.nabra.backend.common.exception.NotFoundException;
import com.nabra.backend.common.model.Enums.SessionOutputType;
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
import org.springframework.transaction.annotation.Transactional;

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
        s.getAccuracyScore(),
        s.getDeviceInfo(),
        s.getModelVersion(),
        s.getIsOffline()
    );
  }

  @Transactional
  public SessionDtos.SessionResponse start(String userId, SessionDtos.StartSessionRequest req) {
    User user = userService.getById(userId);
    Session s = new Session();
    s.setUser(user);
    s.setInputType(req.inputType());
    s.setOutputType(req.outputType());
    s.setStatus(SessionStatus.ACTIVE);
    s.setStartedAt(Instant.now());
    s.setDeviceInfo(req.deviceInfo());
    s.setModelVersion(req.modelVersion());
    s.setIsOffline(req.isOffline() != null ? req.isOffline() : false);

    sessionRepository.save(s);
    return toDto(s);
  }

  @Transactional
  public SessionDtos.SessionResponse stop(String userId, String sessionId, SessionDtos.StopSessionRequest req) {
    Session s = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found with id: " + sessionId));

    if (!s.getUser().getId().equals(userId)) {
      throw new ForbiddenException("You do not have permission to stop this session");
    }

    s.setEndedAt(Instant.now());
    s.setStatus(SessionStatus.COMPLETED);
    if (s.getStartedAt() != null) {
      s.setDurationSeconds(s.getEndedAt().getEpochSecond() - s.getStartedAt().getEpochSecond());
    }

    if (req.content() != null) s.setContent(req.content());
    if (req.resultText() != null) s.setResultText(req.resultText());
    if (req.resultAudioUrl() != null) s.setResultAudioUrl(req.resultAudioUrl());
    if (req.accuracyScore() != null) s.setAccuracyScore(req.accuracyScore());

    sessionRepository.save(s);
    return toDto(s);
  }

  @Transactional(readOnly = true)
  public SessionDtos.SessionResponse getById(String userId, String sessionId) {
    Session s = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found with id: " + sessionId));

    if (!s.getUser().getId().equals(userId)) {
      throw new ForbiddenException("You do not have permission to view this session");
    }

    return toDto(s);
  }

  @Transactional
  public void deleteById(String userId, String sessionId) {
    Session s = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found with id: " + sessionId));

    if (!s.getUser().getId().equals(userId)) {
      throw new ForbiddenException("You do not have permission to delete this session");
    }

    sessionRepository.deleteById(sessionId);
  }

  @Transactional
  public void deleteAllUserSessions(String userId) {
    // Verify user exists
    userService.getById(userId);

    // Delete all sessions for this user
    sessionRepository.deleteAll(sessionRepository.findAll((root, query, cb) ->
        cb.equal(root.get("user").get("id"), userId)));
  }

  @Transactional(readOnly = true)
  public Page<SessionDtos.SessionResponse> list(
      String userId,
      Optional<Instant> from,
      Optional<Instant> to,
      Optional<String> status,
      Optional<String> outputType,
      Optional<String> keyword,
      Optional<Long> minDuration,
      Optional<Long> maxDuration,
      Pageable pageable
  ) {
    Specification<Session> spec = (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);

    if (from.isPresent()) {
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startedAt"), from.get()));
    }
    if (to.isPresent()) {
      spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("startedAt"), to.get()));
    }

    if (status.isPresent() && !status.get().isBlank()) {
      spec = spec.and((root, query, cb) -> {
        try {
          SessionStatus st = SessionStatus.valueOf(status.get().toUpperCase());
          return cb.equal(root.get("status"), st);
        } catch (IllegalArgumentException e) {
          throw new BadRequestException("Invalid status: " + status.get());
        }
      });
    }

    if (outputType.isPresent() && !outputType.get().isBlank()) {
      spec = spec.and((root, query, cb) -> {
        try {
          SessionOutputType ot = SessionOutputType.valueOf(outputType.get().toUpperCase());
          return cb.equal(root.get("outputType"), ot);
        } catch (IllegalArgumentException e) {
          throw new BadRequestException("Invalid outputType: " + outputType.get());
        }
      });
    }

    if (keyword.isPresent() && !keyword.get().isBlank()) {
      String like = "%" + keyword.get().toLowerCase() + "%";
      spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("resultText")), like));
    }

    if (minDuration.isPresent()) {
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("durationSeconds"), minDuration.get()));
    }
    if (maxDuration.isPresent()) {
      spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("durationSeconds"), maxDuration.get()));
    }

    return sessionRepository.findAll(spec, pageable).map(this::toDto);
  }
}

