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

        // 設置用戶在線狀態
        userService.setUserOnlineStatus(currentUser.getId(), true);

        List<UserDTO> onlineUsers = userService.getAllUsers();
        List<UserDTO> friends = userService.getUserFriends(currentUser.getId());
        List<ChatRoom> userRooms = chatRoomService.getUserChatRooms(currentUser.getId());

        model.addAttribute("currentUser", currentUser);
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

        UserDTO chatPartner = userService.getUserById(userId);
        List<MessageDTO> messages = messageService.getConversation(currentUser.getId(), userId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("chatPartner", chatPartner);
        model.addAttribute("messages", messages);
        model.addAttribute("chatType", "private");

        return "chat-window";
    }

    @GetMapping("/chat/room/{roomId}")
    public String roomChat(@PathVariable Long roomId, Model model, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        ChatRoom chatRoom = chatRoomService.getChatRoomById(roomId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("participants", chatRoom.getParticipants());
        model.addAttribute("chatType", "room");

        return "chat-room";
    }

    @GetMapping("/chat/search")
    public String searchUsers(@RequestParam String keyword, Model model, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        List<UserDTO> searchResults = userService.searchUsers(keyword);
        List<ChatRoom> roomResults = chatRoomService.searchChatRooms(keyword);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userResults", searchResults);
        model.addAttribute("roomResults", roomResults);
        model.addAttribute("keyword", keyword);

        return "search-results";
    }
}