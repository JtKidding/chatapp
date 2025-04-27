package com.example.chatapp.websocket;

import com.example.chatapp.dto.MessageDTO;
import com.example.chatapp.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

    // 處理私人聊天訊息
    @MessageMapping("/chat.private.{receiverId}")
    public void sendPrivateMessage(@DestinationVariable Long receiverId,
                                   @Payload ChatMessage chatMessage,
                                   SimpMessageHeaderAccessor headerAccessor) {

        // 儲存訊息到資料庫
        if (chatMessage.getType() == ChatMessage.MessageType.CHAT) {
            MessageDTO savedMessage = messageService.sendMessage(
                    chatMessage.getSenderId(),
                    receiverId,
                    chatMessage.getContent()
            );

            chatMessage.setTimestamp(savedMessage.getTimestamp());
        }

        // 將訊息發送給接收者
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/messages",
                chatMessage
        );

        // 也發送給發送者以確認
        messagingTemplate.convertAndSendToUser(
                chatMessage.getSenderId().toString(),
                "/queue/messages",
                chatMessage
        );
    }

    // 處理聊天室訊息
    @MessageMapping("/chat.room.{roomId}")
    public void sendRoomMessage(@DestinationVariable Long roomId,
                                @Payload ChatMessage chatMessage) {

        chatMessage.setRoomId(roomId);

        // 廣播訊息給所有訂閱該聊天室的用戶
        messagingTemplate.convertAndSend(
                "/topic/room." + roomId,
                chatMessage
        );
    }

    // 處理打字狀態通知
    @MessageMapping("/chat.typing.{receiverId}")
    public void sendTypingNotification(@DestinationVariable Long receiverId,
                                       @Payload ChatMessage chatMessage) {

        chatMessage.setType(ChatMessage.MessageType.TYPING);

        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/typing",
                chatMessage
        );
    }
}