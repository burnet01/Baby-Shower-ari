package com.tennantari.gallery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${gallery.upload-dir}")
    private String uploadDir;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/svg+xml"
    );

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
        "video/mp4", "video/webm", "video/ogg", "video/quicktime", "video/x-msvideo"
    );

    public String storeFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String storedFilename = UUID.randomUUID().toString() + extension;
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return storedFilename;
    }

    public void deleteFile(String storedFilename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(storedFilename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + storedFilename, e);
        }
    }

    public static boolean isImage(String contentType) {
        return contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType);
    }

    public static boolean isVideo(String contentType) {
        return contentType != null && ALLOWED_VIDEO_TYPES.contains(contentType);
    }

    public static boolean isAllowed(String contentType) {
        return isImage(contentType) || isVideo(contentType);
    }
}
