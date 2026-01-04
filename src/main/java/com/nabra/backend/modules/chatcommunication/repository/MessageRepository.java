package com.nabra.backend.modules.chatcommunication.repository;

import com.nabra.backend.common.model.Enums.DeliveryStatus;
import com.nabra.backend.modules.chatcommunication.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {

  Page<Message> findByChatIdOrderBySentAtDesc(String chatId, Pageable pageable);
  // ✅ آخر رسالة في الشات
  Optional<Message> findTopByChatIdOrderBySentAtDesc(String chatId);

  // ✅ عدد الرسائل غير المقروءة
  long countByChatIdAndSenderIdNotAndDeliveryStatus(
      String chatId,
      String senderId,
      DeliveryStatus deliveryStatus
  );
  @Query("""
      select m.id from Message m
      where m.chat.id = :chatId
        and m.sender.id <> :userId
        and m.deliveryStatus = :status
      """)
  List<String> findIdsToUpdateStatus(@Param("chatId") String chatId,
                                     @Param("userId") String userId,
                                     @Param("status") DeliveryStatus status);

  @Modifying
  @Query("""
      update Message m
      set m.deliveryStatus = :newStatus
      where m.id in :ids
      """)
  int bulkUpdateStatus(@Param("ids") List<String> ids,
                       @Param("newStatus") DeliveryStatus newStatus);
                       
}
