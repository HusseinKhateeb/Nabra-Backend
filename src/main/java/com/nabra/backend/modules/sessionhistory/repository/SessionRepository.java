package com.nabra.backend.modules.sessionhistory.repository;

import com.nabra.backend.modules.sessionhistory.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, String>, JpaSpecificationExecutor<Session> {
  
  @Query("SELECT COUNT(s) FROM Session s WHERE s.user.id = :userId")
  long countSessionsByUserId(@Param("userId") String userId);
}
