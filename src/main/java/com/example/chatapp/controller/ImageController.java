package com.example.chatapp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class ImageController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 提供上傳的頭像圖片，如果不存在則提供默認頭像
     */
    @GetMapping("/uploads/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            // 檢查文件是否存在並可讀
            if (resource.exists() && resource.isReadable()) {
                MediaType mediaType = determineMediaType(filename);
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .body(resource);
            } else {
                // 如果文件不存在，則返回默認頭像
                Resource defaultAvatar = new ClassPathResource("static/images/default-avatar.png");
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(defaultAvatar);
            }
        } catch (MalformedURLException e) {
            // 如果URL有問題，返回默認頭像
            try {
                Resource defaultAvatar = new ClassPathResource("static/images/default-avatar.png");
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(defaultAvatar);
            } catch (Exception ex) {
                return ResponseEntity.notFound().build();
            }
        }
    }

    /**
     * 根據文件名確定媒體類型
     */
    private MediaType determineMediaType(String filename) {
        if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else if (filename.toLowerCase().endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (filename.toLowerCase().endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 提供默認頭像
     */
    @GetMapping("/images/default-avatar.png")
    @ResponseBody
    public ResponseEntity<Resource> defaultAvatar() {
        Resource defaultAvatar = new ClassPathResource("static/images/default-avatar.png");
        if (defaultAvatar.exists() && defaultAvatar.isReadable()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(defaultAvatar);
        }

        return ResponseEntity.notFound().build();
    }
}