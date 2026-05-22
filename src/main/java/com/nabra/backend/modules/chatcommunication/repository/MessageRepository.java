package com.nabra.backend.modules.chatcommunication.repository;

import com.nabra.backend.common.model.Enums.DeliveryStatus;
import com.nabra.backend.modules.chatcommunication.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, String> {

    // ================= Messages =================
    Page<Message> findByChatIdOrderBySentAtDesc(String chatId, Pageable pageable);

    // ================= Last message =================
    Optional<Message> findTopByChatIdOrderBySentAtDesc(String chatId);

    // ================= Unread count (used in ChatService) =================
    @Query("""
            select count(m) from Message m
            where m.chat.id = :chatId
              and m.sender.id <> :userId
              and m.deliveryStatus <> com.nabra.backend.common.model.Enums.DeliveryStatus.READ
            """)
    long countUnreadMessages(
            @Param("chatId") String chatId,
            @Param("userId") String userId);

    // ================= Find ALL unread message IDs (SENT + DELIVERED)
    // =================
    @Query("""
            select m.id from Message m
            where m.chat.id = :chatId
              and m.sender.id <> :userId
              and m.deliveryStatus <> com.nabra.backend.common.model.Enums.DeliveryStatus.READ
            """)
    List<String> findAllUnreadIds(
            @Param("chatId") String chatId,
            @Param("userId") String userId);

    // ================= Bulk update status =================
    @Modifying
    @Query("""
            update Message m
            set m.deliveryStatus = :newStatus
            where m.id in :ids
            """)
    int bulkUpdateStatus(
            @Param("ids") List<String> ids,
            @Param("newStatus") DeliveryStatus newStatus);

    @Modifying
    @Query("delete from Message m where m.chat.id = :chatId")
    int deleteAllByChatId(@Param("chatId") String chatId);
}
