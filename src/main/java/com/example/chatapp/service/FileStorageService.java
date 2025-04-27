package com.example.chatapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("無法創建文件上傳目錄", ex);
        }
    }

    /**
     * 保存 Base64 圖片到文件系統
     * @param base64Image Base64 編碼的圖片數據
     * @return 保存的文件名
     */
    public String storeBase64Image(String base64Image) {
        try {
            // 處理 data:image/png;base64, 前綴
            String base64Data = base64Image;
            String fileExtension = "png";  // 默認為 PNG

            if (base64Image.contains(",")) {
                String[] parts = base64Image.split(",");
                base64Data = parts[1];

                if (parts[0].contains("image/jpeg")) {
                    fileExtension = "jpg";
                } else if (parts[0].contains("image/png")) {
                    fileExtension = "png";
                } else if (parts[0].contains("image/gif")) {
                    fileExtension = "gif";
                }
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "." + fileExtension;

            // 解碼 Base64 數據
            byte[] imageData = Base64.getDecoder().decode(base64Data);

            // 保存到文件系統
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.write(targetLocation, imageData);

            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("無法保存圖片", ex);
        }
    }

    /**
     * 保存 MultipartFile 到文件系統
     * @param file 上傳的文件
     * @return 保存的文件名
     */
    public String storeFile(MultipartFile file) {
        // 原始文件名
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // 檢查文件名是否包含無效字符
            if (originalFileName.contains("..")) {
                throw new RuntimeException("文件名包含無效路徑序列 " + originalFileName);
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

            // 保存到文件系統
            try (InputStream inputStream = file.getInputStream()) {
                Path targetLocation = this.fileStorageLocation.resolve(fileName);
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
                return fileName;
            }
        } catch (IOException ex) {
            throw new RuntimeException("無法保存文件 " + originalFileName, ex);
        }
    }

    /**
     * 獲取文件的存儲路徑
     * @param fileName 文件名
     * @return 文件的完整路徑
     */
    public Path getFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName);
    }

    /**
     * 刪除文件
     * @param fileName 文件名
     * @return 是否刪除成功
     */
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            return false;
        }
    }
}