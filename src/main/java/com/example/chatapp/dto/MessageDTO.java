package com.example.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    private Long id;

    private Long senderId;

    private String senderUsername;

    private Long receiverId;

    private String receiverUsername;

    private String content;

    private LocalDateTime timestamp;

    private boolean read;
}