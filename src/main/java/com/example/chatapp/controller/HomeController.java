package com.example.chatapp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        // 已登入用戶重定向到聊天頁面，未登入用戶重定向到登入頁面
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/chat";
        } else {
            return "redirect:/login";
        }
    }
}