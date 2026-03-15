package com.resume.analyzer.service;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件存储服务：上传、删除、获取文件 URL。
 */
@Service
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 检查 MinIO 是否可用：连接并检查/创建配置的 bucket。
     * @return 正常返回 true；异常时抛出 RuntimeException 并包含原因
     */
    public boolean checkHealth() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("MinIO 检查失败: " + e.getMessage());
        }
    }

    /**
     * 确保 bucket 存在，不存在则创建
     */
    private void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO bucket 检查/创建失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件到 MinIO。
     *
     * @param file 上传文件
     * @param objectKey 对象键，如 resume/1_123456_resume.pdf
     * @return MinIO 文件访问地址（可直接访问需 bucket 为 public，否则通过 presigned 接口获取）
     */
    public String uploadFile(MultipartFile file, String objectKey) throws Exception {
        ensureBucket();
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .build()
            );
        }
        return getFileUrl(objectKey);
    }

    /**
     * 从本地文件上传到 MinIO（用于先落盘再上传的场景，如简历解析后上传）。
     */
    public String uploadFile(File file, String contentType, String objectKey) throws Exception {
        ensureBucket();
        try (InputStream is = new FileInputStream(file)) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(is, file.length(), -1)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build()
            );
        }
        return getFileUrl(objectKey);
    }

    /**
     * 使用 InputStream 上传（用于临时文件等）。
     */
    public String uploadStream(InputStream inputStream, long size, String contentType, String objectKey) throws Exception {
        ensureBucket();
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(inputStream, size, -1)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build()
        );
        return getFileUrl(objectKey);
    }

    /**
     * 获取文件访问 URL（固定地址，格式：endpoint/bucket/objectKey）。
     * 若 bucket 为 public 则可直接访问；否则需通过 getPresignedUrl 获取临时链接。
     */
    public String getFileUrl(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) return null;
        String base = endpoint.endsWith("/") ? endpoint : endpoint + "/";
        return base + bucketName + "/" + objectKey;
    }

    /**
     * 获取预签名 URL，用于私有 bucket 临时访问。
     *
     * @param objectKey 对象键
     * @param expireSeconds 过期时间（秒）
     */
    public String getPresignedUrl(String objectKey, int expireSeconds) {
        if (objectKey == null || objectKey.isEmpty()) return null;
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry(expireSeconds, TimeUnit.SECONDS)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("获取预签名 URL 失败: " + e.getMessage());
        }
    }

    /**
     * 获取对象输入流（调用方负责关闭）。用于知识库文档解析等。
     */
    public InputStream getObjectStream(String objectKey) {
        if (objectKey == null || objectKey.isEmpty())
            throw new IllegalArgumentException("objectKey 不能为空");
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO 获取文件失败: " + e.getMessage());
        }
    }

    /**
     * 根据对象键删除 MinIO 中的文件。
     */
    public void deleteFile(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) return;
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO 删除文件失败: " + e.getMessage());
        }
    }
}
