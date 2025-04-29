package com.example.chatapp.service;

import com.example.chatapp.dto.RegisterDTO;
import com.example.chatapp.dto.UserDTO;
import com.example.chatapp.exception.ResourceNotFoundException;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

    public UserDTO registerUser(RegisterDTO registerDTO) {
        // 驗證用戶名和信箱是否已存在
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new IllegalArgumentException("用戶名已存在");
        }

        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new IllegalArgumentException("信箱已被註冊");
        }

        // 驗證密碼和確認密碼是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("兩次輸入的密碼不一致");
        }

        // 創建新用戶
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setFullName(registerDTO.getFullName());
        user.setCreatedAt(LocalDateTime.now());
        user.setOnline(false);

        User savedUser = userRepository.save(user);

        return convertToDTO(savedUser);
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + id + "的用戶"));

        return convertToDTO(user);
    }

    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("找不到用戶名為" + username + "的用戶"));

        return convertToDTO(user);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserDTO> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + id + "的用戶"));

        // 檢查用戶名是否被其他用戶使用
        if (!user.getUsername().equals(userDTO.getUsername()) &&
                userRepository.existsByUsername(userDTO.getUsername())) {
            throw new IllegalArgumentException("用戶名已存在");
        }

        // 檢查信箱是否被其他用戶使用
        if (!user.getEmail().equals(userDTO.getEmail()) &&
                userRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("信箱已被註冊");
        }

        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setFullName(userDTO.getFullName());
        user.setBio(userDTO.getBio());

        // 處理頭像上傳
        if (userDTO.getProfilePicture() != null && !userDTO.getProfilePicture().isEmpty()) {
            // 檢查是否是新上傳的頭像（base64格式）
            if (userDTO.getProfilePicture().startsWith("data:image")) {
                // 刪除舊頭像文件（如果有）
                if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()
                        && !user.getProfilePicture().startsWith("http")) {
                    fileStorageService.deleteFile(user.getProfilePicture());
                }

                // 保存新頭像文件
                String fileName = fileStorageService.storeBase64Image(userDTO.getProfilePicture());
                user.setProfilePicture(fileName);
            } else if (!userDTO.getProfilePicture().equals(user.getProfilePicture())) {
                // 已經是文件名但與數據庫不同
                user.setProfilePicture(userDTO.getProfilePicture());
            }
            // 如果相同，則不更新
        }

        User updatedUser = userRepository.save(user);

        return convertToDTO(updatedUser);
    }

    public void setUserOnlineStatus(Long userId, boolean status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + userId + "的用戶"));

        user.setOnline(status);

        if (status) {
            user.setLastLogin(LocalDateTime.now());
        }

        userRepository.save(user);
    }

    public void addFriend(Long userId, Long friendId) {
        System.out.println("嘗試添加好友: userId=" + userId + ", friendId=" + friendId);

        // 檢查是否嘗試添加自己為好友
        if (userId.equals(friendId)) {
            System.out.println("錯誤: 嘗試添加自己為好友");
            throw new IllegalArgumentException("不能添加自己為好友");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    System.out.println("錯誤: 找不到用戶 " + userId);
                    return new ResourceNotFoundException("找不到ID為" + userId + "的用戶");
                });

        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> {
                    System.out.println("錯誤: 找不到好友 " + friendId);
                    return new ResourceNotFoundException("找不到ID為" + friendId + "的用戶");
                });

        System.out.println("用戶: " + user.getUsername() + ", 好友: " + friend.getUsername());

        // 檢查是否已經是好友
        if (user.getFriends().contains(friend)) {
            System.out.println("錯誤: 已經是好友關係");
            throw new IllegalArgumentException("已經是好友關係");
        }

        // 添加好友關係（雙向）
        user.getFriends().add(friend);
        friend.getFriends().add(user);

        System.out.println("儲存好友關係");
        userRepository.save(user);
        userRepository.save(friend);
        System.out.println("好友關係儲存成功");
    }

    public void removeFriend(Long userId, Long friendId) {
        // 檢查是否嘗試移除自己
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("不能移除自己");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + userId + "的用戶"));

        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + friendId + "的用戶"));

        // 檢查是否已經是好友
        if (!user.getFriends().contains(friend)) {
            throw new IllegalArgumentException("該用戶不是您的好友");
        }

        // 移除好友關係（雙向）
        user.getFriends().remove(friend);
        friend.getFriends().remove(user);

        userRepository.save(user);
        userRepository.save(friend);
    }

    public List<UserDTO> getUserFriends(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID為" + userId + "的用戶"));

        return user.getFriends().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 輔助方法：將實體類轉換為DTO
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());

        // 處理頭像URL
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            // 如果已經是完整URL
            if (user.getProfilePicture().startsWith("http")) {
                dto.setProfilePicture(user.getProfilePicture());
            } else {
                // 轉換為文件訪問URL
                dto.setProfilePicture("/uploads/" + user.getProfilePicture());
            }
        } else {
            dto.setProfilePicture(null);
        }

        dto.setBio(user.getBio());
        dto.setOnline(user.isOnline());

        return dto;
    }
}