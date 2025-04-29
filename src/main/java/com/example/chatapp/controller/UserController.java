package com.example.chatapp.controller;

import com.example.chatapp.dto.UserDTO;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/profile")
    public String userProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        UserDTO userDTO = userService.getUserById(currentUser.getId());
        List<UserDTO> friends = userService.getUserFriends(currentUser.getId());

        model.addAttribute("user", userDTO);
        model.addAttribute("friends", friends);  // 添加friends變數

        return "profile";
    }

    @GetMapping("/profile/{userId}")
    public String viewUserProfile(@PathVariable Long userId, Model model, Authentication authentication) {
        // 獲取當前用戶
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        // 獲取要查看的用戶資料
        UserDTO userToView = userService.getUserById(userId);
        List<UserDTO> friends = userService.getUserFriends(currentUser.getId());

        // 檢查是否為好友
        boolean isFriend = false;
        for (UserDTO friend : friends) {
            if (friend.getId().equals(userId)) {
                isFriend = true;
                break;
            }
        }

        model.addAttribute("user", userToView);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isSelf", currentUser.getId().equals(userId));
        model.addAttribute("isFriend", isFriend);
        model.addAttribute("friends", friends);

        return "view-profile";  // 使用一個新的模板來展示其他用戶的資料
    }

    @GetMapping("/edit")
    public String editProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        UserDTO userDTO = userService.getUserById(currentUser.getId());
        List<UserDTO> friends = userService.getUserFriends(currentUser.getId());

        model.addAttribute("userDTO", userDTO);
        model.addAttribute("friends", friends);  // 添加friends變數

        return "edit-profile";
    }

    @PostMapping("/edit")
    public String updateProfile(@Valid @ModelAttribute("userDTO") UserDTO userDTO,
                                BindingResult result,
                                Model model,
                                Authentication authentication) {

        if (result.hasErrors()) {
            return "edit-profile";
        }

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        try {
            userService.updateUser(currentUser.getId(), userDTO);
            return "redirect:/user/profile?updated";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "edit-profile";
        }
    }

    @GetMapping("/friends")
    public String friendsList(Model model, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        List<UserDTO> friends = userService.getUserFriends(currentUser.getId());

        model.addAttribute("friends", friends);
        model.addAttribute("successMessage", null);  // 添加這些空值，防止模板出錯
        model.addAttribute("errorMessage", null);

        return "friends";
    }

    @PostMapping("/friends/add/{friendId}")
    public String addFriend(@PathVariable Long friendId, Authentication authentication, RedirectAttributes redirectAttributes,
                            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("用戶不存在"));

            System.out.println("正在處理添加好友請求: 當前用戶=" + currentUser.getId() + ", 好友ID=" + friendId);

            // 添加好友邏輯
            userService.addFriend(currentUser.getId(), friendId);

            // 添加成功消息
            redirectAttributes.addFlashAttribute("successMessage", "成功添加好友！");

            // 獲取來源頁面
            String referer = request.getHeader("Referer");
            System.out.println("Referer: " + referer);

            // 如果是從用戶資料頁面來的，則返回該頁面
            if (referer != null && referer.contains("/user/profile/")) {
                return "redirect:" + referer;
            }

            return "redirect:/user/friends?added";
        } catch (Exception e) {
            // 記錄錯誤
            System.err.println("添加好友出錯: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();

            // 添加錯誤消息
            String errorMsg = e.getMessage() != null ? e.getMessage() : "未知錯誤";
            redirectAttributes.addFlashAttribute("errorMessage", "添加好友失敗: " + errorMsg);

            return "redirect:/user/friends?error";
        }
    }

    @PostMapping("/friends/remove/{friendId}")
    public String removeFriend(@PathVariable Long friendId, Authentication authentication, RedirectAttributes redirectAttributes,
                               jakarta.servlet.http.HttpServletRequest request) {
        try {
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("用戶不存在"));

            // 執行移除好友邏輯
            userService.removeFriend(currentUser.getId(), friendId);

            // 添加成功消息
            redirectAttributes.addFlashAttribute("successMessage", "已成功移除好友關係");

            // 獲取來源頁面
            String referer = request.getHeader("Referer");
            // 如果是從用戶資料頁面來的，則返回該頁面
            if (referer != null && referer.contains("/user/profile/")) {
                return "redirect:" + referer;
            }

            return "redirect:/user/friends?removed";
        } catch (Exception e) {
            // 記錄錯誤
            System.err.println("移除好友出錯: " + e.getMessage());
            e.printStackTrace();

            // 添加錯誤消息
            redirectAttributes.addFlashAttribute("errorMessage", "移除好友失敗: " + e.getMessage());

            return "redirect:/user/friends?error";
        }
    }
}