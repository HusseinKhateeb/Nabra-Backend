package com.nabra.backend.modules.sessionhistory.repository;

import com.nabra.backend.modules.sessionhistory.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SessionRepository extends JpaRepository<Session, String>, JpaSpecificationExecutor<Session> {
}
