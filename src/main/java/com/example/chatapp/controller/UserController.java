package com.example.chatapp.controller;

import com.example.chatapp.dto.UserDTO;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

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
        model.addAttribute("user", userDTO);

        return "profile";
    }

    @GetMapping("/edit")
    public String editProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        UserDTO userDTO = userService.getUserById(currentUser.getId());
        model.addAttribute("userDTO", userDTO);

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
            model.addAttribute("user profile update error", e.getMessage());
            return "edit-profile";
        }
    }

    @GetMapping("/friends")
    public String friendsList(Model model, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        model.addAttribute("friends", userService.getUserFriends(currentUser.getId()));

        return "friends";
    }

    @PostMapping("/friends/add/{friendId}")
    public String addFriend(@PathVariable Long friendId, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        userService.addFriend(currentUser.getId(), friendId);

        return "redirect:/user/friends?added";
    }

    @PostMapping("/friends/remove/{friendId}")
    public String removeFriend(@PathVariable Long friendId, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用戶不存在"));

        userService.removeFriend(currentUser.getId(), friendId);

        return "redirect:/user/friends?removed";
    }
}