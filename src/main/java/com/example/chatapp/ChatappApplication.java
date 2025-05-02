package com.example.chatapp;

import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class ChatappApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatappApplication.class, args);
	}

	@Autowired
	private UserRepository userRepository;

	@PostConstruct
	public void init() {
		// 應用程序啟動時將所有用戶設置為離線
		List<User> users = userRepository.findAll();
		for (User user : users) {
			user.setOnline(false);
			userRepository.save(user);
		}
	}
}
