package com.nabra.backend.modules.chatcommunication.repository;

import com.nabra.backend.modules.chatcommunication.model.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, String> {
  Page<Chat> findByParticipantsIdOrderByLastMessageAtDesc(String userId, Pageable pageable);
  
}
