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
        try {
            // 添加額外的參數驗證
            if (chatMessage.getSenderId() == null || receiverId == null) {
                logger.error("發送者或接收者ID為空");
                return;
            }
            logger.info("=====================================================");
            logger.info("接收到私聊訊息: 發送者={}, 接收者={}, 類型={}, 內容={}",
                    chatMessage.getSenderId(), receiverId, chatMessage.getType(), chatMessage.getContent());
            logger.error("接收者ID類型: {}, 值: {}",
                    receiverId.getClass().getName(), receiverId);
            logger.error("發送者ID類型: {}, 值: {}",
                    chatMessage.getSenderId().getClass().getName(), chatMessage.getSenderId());
            // 確保接收者ID已設置
            chatMessage.setReceiverId(receiverId);

            // 儲存聊天訊息到資料庫 (僅針對聊天訊息，非系統訊息)
            if (chatMessage.getType() == ChatMessage.MessageType.CHAT) {
                try {
                    MessageDTO savedMessage = messageService.sendMessage(
                            chatMessage.getSenderId(),
                            receiverId,
                            chatMessage.getContent()
                    );

                    // 創建包含保存ID的新消息對象
                    ChatMessage messageToSend = new ChatMessage();
                    messageToSend.setType(chatMessage.getType());
                    messageToSend.setSenderId(chatMessage.getSenderId());
                    messageToSend.setSenderUsername(chatMessage.getSenderUsername());
                    messageToSend.setContent(chatMessage.getContent());
                    messageToSend.setReceiverId(receiverId);
                    messageToSend.setTimestamp(savedMessage.getTimestamp());
                    messageToSend.setMessageId(savedMessage.getId());  // 設置資料庫ID

                    logger.error("訊息已儲存到資料庫, ID={}, 準備發送給接收者={}",
                            savedMessage.getId(), receiverId);

                    // 1. 發送給接收者
                    logger.error("發送訊息給接收者: {}", receiverId);
                    messagingTemplate.convertAndSendToUser(
                            receiverId.toString(),
                            "/queue/messages",
                            messageToSend
                    );
                    logger.error("訊息已發送給接收者");

                    // 2. 發送給發送者（確認）
                    logger.error("發送確認訊息給發送者: {}", chatMessage.getSenderId());
                    messagingTemplate.convertAndSendToUser(
                            chatMessage.getSenderId().toString(),
                            "/queue/messages",
                            messageToSend
                    );
                    logger.error("確認訊息已發送給發送者");

                } catch (Exception e) {
                    logger.error("儲存訊息時出錯", e);
                    e.printStackTrace(); // 輸出完整堆疊追蹤
                }
            } else {
                // 對於非聊天類型的消息（如JOIN、LEAVE等），直接轉發
                try {
                    // 發送給接收者
                    messagingTemplate.convertAndSendToUser(
                            receiverId.toString(),
                            "/queue/messages",
                            chatMessage
                    );

                    // 也發送給發送者（若非同一人）
                    messagingTemplate.convertAndSendToUser(
                            chatMessage.getSenderId().toString(),
                            "/queue/messages",
                            chatMessage
                    );
                } catch (Exception e) {
                    logger.error("發送系統訊息時出錯", e);
                }
            }
            logger.info("=====================================================");
        } catch (Exception e) {
            logger.error("處理WebSocket消息時出現未捕獲異常", e);
            e.printStackTrace();
        }

//        try {
//            // 將訊息發送給接收者 - 使用convertAndSendToUser方法
//            logger.info("發送訊息給接收者: {}", receiverId);
//            messagingTemplate.convertAndSendToUser(
//                    receiverId.toString(),
//                    "/queue/messages",
//                    chatMessage
//            );
//            logger.info("訊息已發送給接收者");
//
//            // 如果發送者和接收者不是同一人，也發送給發送者以確認
//            if (!chatMessage.getSenderId().equals(receiverId)) {
//                logger.info("發送確認訊息給發送者ID: {}", chatMessage.getSenderId());
//                messagingTemplate.convertAndSendToUser(
//                        chatMessage.getSenderId().toString(),
//                        "/queue/messages",
//                        chatMessage
//                );
//                logger.info("確認訊息已發送給發送者");
//            }
//        } catch (Exception e) {
//            logger.error("發送訊息時出錯", e);
//        }
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

    @MessageMapping("/test")
    public void handleTestMessage(@Payload Object message) {
        logger.info("收到測試消息: {}", message);
        // 回顯消息給所有用戶(僅用於測試)
        messagingTemplate.convertAndSend("/topic/test", message);
        logger.info("測試消息已發送到 /topic/test");
    }
}