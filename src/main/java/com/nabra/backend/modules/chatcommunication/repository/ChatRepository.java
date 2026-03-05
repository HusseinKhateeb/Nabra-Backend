package com.nabra.backend.modules.chatcommunication.repository;

import com.nabra.backend.modules.chatcommunication.model.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRepository extends JpaRepository<Chat, String> {
  Page<Chat> findByParticipantsIdOrderByLastMessageAtDesc(String userId, Pageable pageable);
  
  @Query("SELECT COUNT(c) FROM Chat c JOIN c.participants p WHERE p.id = :userId")
  long countChatsByUserId(@Param("userId") String userId);
}
