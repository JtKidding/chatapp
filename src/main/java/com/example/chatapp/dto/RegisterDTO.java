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
public class RegisterDTO {

    @NotBlank(message = "使用者不能為空")
    @Size(min = 3, max = 50, message = "使用者長度必須在3-50之間")
    private String username;

    @NotBlank(message = "密碼不能為空")
    @Size(min = 6, max = 100, message = "密碼長度必須在6-100之間")
    private String password;

    @NotBlank(message = "確認密碼不能為空")
    private String confirmPassword;

    @Email(message = "信箱格式不正確")
    @NotBlank(message = "信箱不能為空")
    private String email;

    private String fullName;
}