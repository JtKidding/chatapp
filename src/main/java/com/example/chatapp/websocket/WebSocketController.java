package com.example.chatapp.websocket;

import com.example.chatapp.dto.MessageDTO;
import com.example.chatapp.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

    // 處理私人聊天訊息
    @MessageMapping("/chat.private.{receiverId}")
    public void sendPrivateMessage(@DestinationVariable Long receiverId,
                                   @Payload ChatMessage chatMessage,
                                   SimpMessageHeaderAccessor headerAccessor) {

        logger.info("接收到私聊訊息: 發送者={}, 接收者={}, 類型={}, 內容={}",
                chatMessage.getSenderId(), receiverId, chatMessage.getType(), chatMessage.getContent());

        // 儲存訊息到資料庫
        if (chatMessage.getType() == ChatMessage.MessageType.CHAT) {
            try {
                MessageDTO savedMessage = messageService.sendMessage(
                        chatMessage.getSenderId(),
                        receiverId,
                        chatMessage.getContent()
                );

                logger.info("訊息已儲存到資料庫, ID={}", savedMessage.getId());
                chatMessage.setTimestamp(savedMessage.getTimestamp());
            } catch (Exception e) {
                logger.error("儲存訊息時出錯", e);
            }
        }

        try {
            // 將訊息發送給接收者
            logger.info("發送訊息給接收者: {}", receiverId);
            messagingTemplate.convertAndSendToUser(
                    receiverId.toString(),
                    "/queue/messages",
                    chatMessage
            );

            // 也發送給發送者以確認
            logger.info("發送確認訊息給發送者: {}", chatMessage.getSenderId());
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getSenderId().toString(),
                    "/queue/messages",
                    chatMessage
            );
        } catch (Exception e) {
            logger.error("發送訊息時出錯", e);
        }
    }

    // 處理聊天室訊息
    @MessageMapping("/chat.room.{roomId}")
    public void sendRoomMessage(@DestinationVariable Long roomId,
                                @Payload ChatMessage chatMessage) {

        logger.info("接收到聊天室訊息: 發送者={}, 聊天室={}", chatMessage.getSenderId(), roomId);

        chatMessage.setRoomId(roomId);

        try {
            // 廣播訊息給所有訂閱該聊天室的用戶
            logger.info("廣播訊息到聊天室: {}", roomId);
            messagingTemplate.convertAndSend(
                    "/topic/room." + roomId,
                    chatMessage
            );
        } catch (Exception e) {
            logger.error("廣播聊天室訊息時出錯", e);
        }
    }

    // 處理打字狀態通知
    @MessageMapping("/chat.typing.{receiverId}")
    public void sendTypingNotification(@DestinationVariable Long receiverId,
                                       @Payload ChatMessage chatMessage) {

        logger.debug("接收到打字通知: 發送者={}, 接收者={}", chatMessage.getSenderId(), receiverId);

        chatMessage.setType(ChatMessage.MessageType.TYPING);

        try {
            messagingTemplate.convertAndSendToUser(
                    receiverId.toString(),
                    "/queue/typing",
                    chatMessage
            );
        } catch (Exception e) {
            logger.error("發送打字通知時出錯", e);
        }
    }
}