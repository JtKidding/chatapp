package com.example.chatapp.controller;

import com.example.chatapp.dto.RegisterDTO;
import com.example.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registerDTO") RegisterDTO registerDTO,
                               BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(registerDTO);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}