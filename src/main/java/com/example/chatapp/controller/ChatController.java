package com.example.chatapp.controller;

import com.example.chatapp.dto.MessageDTO;
import com.example.chatapp.dto.UserDTO;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ChatController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatRoomService chatRoomService;

    @GetMapping("/chat")
    public String chatPage(Model model, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        UserDTO userDTO = userService.getUserById(currentUser.getId());

        // 設置用戶在線狀態
        userService.setUserOnlineStatus(userDTO.getId(), true);

        List<UserDTO> onlineUsers = userService.getAllUsers();
        List<UserDTO> friends = userService.getUserFriends(currentUser.getId());
        List<ChatRoom> userRooms = chatRoomService.getUserChatRooms(currentUser.getId());

        model.addAttribute("currentUser", userDTO);
        model.addAttribute("onlineUsers", onlineUsers);
        model.addAttribute("friends", friends);
        model.addAttribute("userRooms", userRooms);

        return "chat";
    }

    @GetMapping("/chat/user/{userId}")
    public String privateChat(@PathVariable Long userId, Model model, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        UserDTO userDTO = userService.getUserById(currentUser.getId());
        UserDTO chatPartner = userService.getUserById(userId);
        List<MessageDTO> messages = messageService.getConversation(currentUser.getId(), userId);
        List<UserDTO> friends = userService.getUserFriends(currentUser.getId());

        // 檢查是否為好友
        boolean isFriend = false;
        for (UserDTO friend : friends) {
            if (friend.getId().equals(userId)) {
                isFriend = true;
                break;
            }
        }

        model.addAttribute("currentUser", userDTO);
        model.addAttribute("chatPartner", chatPartner);
        model.addAttribute("messages", messages);
        model.addAttribute("chatType", "private");
        model.addAttribute("friends", friends);
        model.addAttribute("isFriend", isFriend);

        System.out.println("載入聊天視窗: 當前用戶=" + currentUser.getUsername() +
                ", 聊天對象=" + chatPartner.getUsername() +
                ", 訊息數量=" + (messages != null ? messages.size() : 0));

        return "chat-window";
    }

    @GetMapping("/chat/room/{roomId}")
    public String roomChat(@PathVariable Long roomId, Model model, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        ChatRoom chatRoom = chatRoomService.getChatRoomById(roomId);
        List<UserDTO> friends = userService.getUserFriends(currentUser.getId());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("participants", chatRoom.getParticipants());
        model.addAttribute("chatType", "room");
        model.addAttribute("friends", friends);  // 添加friends變數

        return "chat-room";
    }

    @GetMapping("/chat/search")
    public String searchUsers(@RequestParam(required = false) String keyword, Model model, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        // 若未提供關鍵字，則顯示空結果
        List<UserDTO> searchResults = keyword != null ? userService.searchUsers(keyword) : List.of();
        List<ChatRoom> roomResults = keyword != null ? chatRoomService.searchChatRooms(keyword) : List.of();
        List<UserDTO> friends = userService.getUserFriends(currentUser.getId());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userResults", searchResults);
        model.addAttribute("roomResults", roomResults);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("friends", friends);  // 確保 friends 變數總是被設置

        return "search-results";
    }
}