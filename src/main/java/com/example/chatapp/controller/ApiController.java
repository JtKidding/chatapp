package com.example.chatapp.controller;

import com.example.chatapp.dto.MessageDTO;
import com.example.chatapp.dto.UserDTO;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepository;

    // 更新用戶狀態（在線/離線）
    @PostMapping("/user/status")
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @RequestParam boolean online,
            Authentication authentication) {

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        userService.setUserOnlineStatus(currentUser.getId(), online);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // 獲取未讀消息數量
    @GetMapping("/messages/unread/count")
    public ResponseEntity<Integer> getUnreadMessageCount(Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        int unreadCount = messageService.getUnreadMessageCount(currentUser.getId());

        return ResponseEntity.ok(unreadCount);
    }

    // 獲取未讀消息列表
    @GetMapping("/messages/unread")
    public ResponseEntity<List<MessageDTO>> getUnreadMessages(Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        List<MessageDTO> unreadMessages = messageService.getUnreadMessages(currentUser.getId());

        return ResponseEntity.ok(unreadMessages);
    }

    // 獲取在線用戶列表
    @GetMapping("/users/online")
    public ResponseEntity<List<UserDTO>> getOnlineUsers() {
        List<UserDTO> onlineUsers = userService.getAllUsers().stream()
                .filter(UserDTO::isOnline)
                .collect(Collectors.toList());

        return ResponseEntity.ok(onlineUsers);
    }

    // 獲取用戶頭像
    @GetMapping("/user/{userId}/avatar")
    public ResponseEntity<Map<String, String>> getUserAvatar(@PathVariable Long userId) {
        UserDTO userDTO = userService.getUserById(userId);

        Map<String, String> response = new HashMap<>();
        response.put("avatar", userDTO.getProfilePicture() != null ?
                userDTO.getProfilePicture() : "/images/default-avatar.png");

        return ResponseEntity.ok(response);
    }

    // 獲取用戶統計數據
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<Map<String, Integer>> getUserStatistics(
            @PathVariable Long userId,
            Authentication authentication) {

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        // 確保用戶只能查看自己的統計數據
        if (!currentUser.getId().equals(userId) && !currentUser.getUsername().equals("admin")) {
            return ResponseEntity.status(403).build();
        }

        // 這裡只是示例，實際專案中需要從數據庫中獲取真實數據
        Map<String, Integer> statistics = new HashMap<>();
        statistics.put("friendsCount", userService.getUserFriends(userId).size());
        statistics.put("messagesCount", 0); // 需要實際實現
        statistics.put("roomsCount", 0);    // 需要實際實現

        return ResponseEntity.ok(statistics);
    }

    // 獲取最近聊天列表
    @GetMapping("/chats/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentChats(Authentication authentication) {
        // 此方法需要實際實現，這裡只是返回空列表作為示例
        return ResponseEntity.ok(List.of());
    }
}