package com.nabra.backend.modules.chatcommunication.model;

import com.nabra.backend.common.model.BaseEntity;
import com.nabra.backend.modules.usermanagement.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chats", indexes = {
    @Index(name = "idx_chats_lastMessageAt", columnList = "lastMessageAt")
})
@Getter
@Setter
public class Chat extends BaseEntity {

  @Column(nullable = false)
  private boolean groupChat = false;

  /** For groups. */
  @Column(length = 120)
  private String title;

  // ✅ هذا الحقل كان ناقص
  @Column
  private Instant lastMessageAt;

  @ManyToMany
  @JoinTable(
      name = "chat_participants",
      joinColumns = @JoinColumn(name = "chat_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id")
  )
  private Set<User> participants = new HashSet<>();
}
