package com.resume.analyzer.controller;

import com.resume.analyzer.service.MinioService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 头像访问：优先从本地 uploads/avatars 读取（兼容旧数据），否则重定向到 MinIO 预签名 URL。
 */
@RestController
public class AvatarController {

    private final MinioService minioService;
    private final Path localAvatarsDir;

    public AvatarController(MinioService minioService) {
        this.minioService = minioService;
        this.localAvatarsDir = Paths.get(System.getProperty("user.dir"), "uploads", "avatars");
    }

    @GetMapping("/avatars/{filename:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        if (filename == null || filename.contains("..")) {
            return ResponseEntity.notFound().build();
        }
        File localFile = localAvatarsDir.resolve(filename).toFile();
        if (localFile.exists() && localFile.isFile()) {
            Resource resource = new FileSystemResource(localFile);
            String contentType = contentTypeFromFilename(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }
        try {
            String presigned = minioService.getPresignedUrl("avatars/" + filename, 3600);
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, presigned)
                    .build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private static String contentTypeFromFilename(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
