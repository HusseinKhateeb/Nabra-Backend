package com.nabra.backend.modules.sessionhistory.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.sessionhistory.dto.SessionDtos;
import com.nabra.backend.modules.sessionhistory.service.SessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/{userId}/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions")
public class UserSessionHistoryController {

    private final SessionService sessionService;

    @GetMapping({ "", "/" })
    public ResponseEntity<Page<SessionDtos.SessionResponse>> listAvsrHistory(
            @PathVariable("userId") String userId,
            @PageableDefault(sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        var p = SecurityUtils.currentPrincipal();
        return ResponseEntity.ok(sessionService.listAvsrHistory(p.getUserId(), userId, pageable));
    }
}
