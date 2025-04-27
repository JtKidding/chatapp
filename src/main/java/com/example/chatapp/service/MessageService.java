package com.example.chatapp.service;

import com.example.chatapp.dto.MessageDTO;
import com.example.chatapp.exception.ResourceNotFoundException;
import com.example.chatapp.model.Message;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    public MessageDTO sendMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + senderId + "的用户"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + receiverId + "的用户"));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setRead(false);

        Message savedMessage = messageRepository.save(message);

        return convertToDTO(savedMessage);
    }

    public List<MessageDTO> getConversation(Long user1Id, Long user2Id) {
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + user1Id + "的用户"));

        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + user2Id + "的用户"));

        List<Message> messages = messageRepository.findConversation(user1, user2);

        // 将发送给user1的消息标记為已读
        messages.stream()
                .filter(m -> m.getReceiver().getId().equals(user1Id) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                });

        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<MessageDTO> getUnreadMessages(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + userId + "的用户"));

        List<Message> unreadMessages = messageRepository.findByReceiverAndReadFalse(user);

        return unreadMessages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public int getUnreadMessageCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + userId + "的用户"));

        return messageRepository.countByReceiverAndReadFalse(user);
    }

    public void markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + messageId + "的訊息"));

        message.setRead(true);
        messageRepository.save(message);
    }

    // 輔助方法：將實體類轉換為DTO
    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setReceiverUsername(message.getReceiver().getUsername());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getTimestamp());
        dto.setRead(message.isRead());

        return dto;
    }
}