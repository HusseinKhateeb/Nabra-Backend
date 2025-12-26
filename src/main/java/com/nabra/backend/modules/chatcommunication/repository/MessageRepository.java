package com.nabra.backend.modules.chatcommunication.repository;

import com.nabra.backend.modules.chatcommunication.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, String> {
  Page<Message> findByChatIdOrderBySentAtDesc(String chatId, Pageable pageable);
}
