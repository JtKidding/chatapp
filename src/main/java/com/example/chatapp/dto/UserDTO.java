package com.example.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;

    @NotBlank(message = "使用者不能為空")
    @Size(min = 3, max = 50, message = "使用者長度必須在3-50之間")
    private String username;

    @Email(message = "信箱格是不正確")
    @NotBlank(message = "信箱不能為空")
    private String email;

    private String fullName;

    private String profilePicture;

    private String bio;

    private boolean online;
}