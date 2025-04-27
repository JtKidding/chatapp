package com.example.chatapp.service;

import com.example.chatapp.exception.ResourceNotFoundException;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ChatRoomService {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    public ChatRoom createChatRoom(String name, String description, Long creatorId, boolean isPrivate) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + creatorId + "的用戶"));

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(name);
        chatRoom.setDescription(description);
        chatRoom.setCreator(creator);
        chatRoom.setCreatedAt(LocalDateTime.now());
        chatRoom.setPrivate(isPrivate);

        // 將創建者添加為參與者
        chatRoom.getParticipants().add(creator);

        return chatRoomRepository.save(chatRoom);
    }

    public ChatRoom getChatRoomById(Long id) {
        return chatRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + id + "的聊天室"));
    }

    public List<ChatRoom> getPublicChatRooms() {
        return chatRoomRepository.findByIsPrivateFalse();
    }

    public List<ChatRoom> getUserChatRooms(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + userId + "的用戶"));

        return chatRoomRepository.findByParticipant(user);
    }

    public void addUserToChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + chatRoomId + "的聊天室"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + userId + "的用戶"));

        chatRoom.getParticipants().add(user);
        chatRoomRepository.save(chatRoom);
    }

    public void removeUserFromChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + chatRoomId + "的聊天室"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + userId + "的用戶"));

        // 如果是創建者，不允許移除
        if (chatRoom.getCreator().getId().equals(userId)) {
            throw new IllegalArgumentException("創建者不能被移出聊天室");
        }

        chatRoom.getParticipants().remove(user);
        chatRoomRepository.save(chatRoom);
    }

    public Set<User> getChatRoomParticipants(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + chatRoomId + "的聊天室"));

        return chatRoom.getParticipants();
    }

    public List<ChatRoom> searchChatRooms(String keyword) {
        return chatRoomRepository.searchRooms(keyword);
    }

    public void updateChatRoom(Long chatRoomId, String name, String description, boolean isPrivate) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + chatRoomId + "的聊天室"));

        chatRoom.setName(name);
        chatRoom.setDescription(description);
        chatRoom.setPrivate(isPrivate);

        chatRoomRepository.save(chatRoom);
    }

    public void deleteChatRoom(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + chatRoomId + "的聊天室"));

        chatRoomRepository.delete(chatRoom);
    }
}