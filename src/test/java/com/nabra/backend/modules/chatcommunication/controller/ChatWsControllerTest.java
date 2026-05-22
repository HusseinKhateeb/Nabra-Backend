package com.nabra.backend.modules.chatcommunication.controller;

import com.nabra.backend.modules.chatcommunication.dto.ChatWsDtos;
import com.nabra.backend.modules.chatcommunication.model.Chat;
import com.nabra.backend.modules.chatcommunication.service.ChatService;
import com.nabra.backend.modules.chatcommunication.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWsControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ChatService chatService;
    @Mock
    private MessageService messageService;

    private ChatWsController controller;
    private Principal principal;

    @BeforeEach
    void setUp() {
        controller = new ChatWsController(messagingTemplate, chatService, messageService);
        principal = () -> "u1";
    }

    @Test
    void join_shouldMarkDeliveredAndBroadcast() {
        Chat chat = new Chat();
        chat.setId("c1");
        when(chatService.getChat("c1")).thenReturn(chat);
        when(chatService.isParticipant(chat, "u1")).thenReturn(true);
        when(messageService.markDelivered("u1", "c1")).thenReturn(List.of("m1", "m2"));

        controller.join("c1", principal);

        verify(messagingTemplate).convertAndSend(eq("/topic/chats/c1"), any(ChatWsDtos.WsDeliveredEvent.class));
    }

    @Test
    void join_whenNotParticipant_shouldThrow() {
        Chat chat = new Chat();
        chat.setId("c1");
        when(chatService.getChat("c1")).thenReturn(chat);
        when(chatService.isParticipant(chat, "u1")).thenReturn(false);

        try {
            controller.join("c1", principal);
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("Not a participant");
        }
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void send_shouldBroadcastMessage() {
        Chat chat = new Chat();
        chat.setId("c1");
        var req = new ChatWsDtos.WsSendMessage("c1", null, "hello", null, null);
        when(chatService.getChat("c1")).thenReturn(chat);
        when(chatService.isParticipant(chat, "u1")).thenReturn(true);

        controller.send(req, principal);

        verify(messagingTemplate).convertAndSend(eq("/topic/chats/c1"), any(ChatWsDtos.WsMessageEvent.class));
    }

    @Test
    void typing_shouldBroadcastTypingEvent() {
        Chat chat = new Chat();
        chat.setId("c1");
        var req = new ChatWsDtos.WsTyping("c1", true);
        when(chatService.getChat("c1")).thenReturn(chat);
        when(chatService.isParticipant(chat, "u1")).thenReturn(true);

        controller.typing(req, principal);

        verify(messagingTemplate).convertAndSend(eq("/topic/chats/c1"), any(ChatWsDtos.WsTypingEvent.class));
    }

    @Test
    void seen_shouldMarkReadAndBroadcast() {
        Chat chat = new Chat();
        chat.setId("c1");
        var req = new ChatWsDtos.WsSeen("c1", List.of("m1", "m2"));
        when(messageService.markRead("u1", "c1", List.of("m1", "m2"))).thenReturn(List.of("m1", "m2"));

        controller.seen(req, principal);

        verify(messagingTemplate).convertAndSend(eq("/topic/chats/c1"), any(ChatWsDtos.WsSeenEvent.class));
    }

    @Test
    void seen_whenNoMessagesToAck_shouldNotBroadcast() {
        var req = new ChatWsDtos.WsSeen("c1", List.of());
        when(messageService.markRead("u1", "c1", List.of())).thenReturn(List.of());

        controller.seen(req, principal);

        verifyNoInteractions(messagingTemplate);
    }
}
