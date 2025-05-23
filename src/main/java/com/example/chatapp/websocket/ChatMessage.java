package com.example.chatapp.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    public enum MessageType {
        CHAT, JOIN, LEAVE, TYPING
    }

    private MessageType type;
    private Long senderId;
    private String senderUsername;
    private String content;
    private Long receiverId;
    private Long roomId;
    private LocalDateTime timestamp = LocalDateTime.now();

    // 添加消息ID字段，用於跟踪消息和避免重複顯示
    private Long messageId;

}