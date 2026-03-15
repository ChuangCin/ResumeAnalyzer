package com.resume.analyzer.controller;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.dto.ResumeListItemDTO;
import com.resume.analyzer.service.ResumeService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resume")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping("/list/page")
    public Result<Map<String, Object>> listPage(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size
    ) {
        return Result.success(resumeService.listByUserIdPage(userId, page, size));
    }

    @PostMapping("/upload")
    public Result<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId
    ) {
        try {
            return resumeService.upload(file, userId);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg.contains("Connection") || msg.contains("MinIO") || msg.contains("bucket")) {
                msg = "文件存储服务异常，请确认 MinIO 已启动且配置正确（endpoint、accessKey、secretKey、bucketName）。" + " 详情: " + msg;
            }
            return Result.error(msg);
        }
    }

    @GetMapping("/list")
    public Result<List<ResumeListItemDTO>> list(@RequestParam("userId") Long userId) {
        return Result.success(resumeService.listByUserId(userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        return resumeService.deleteResume(id, userId);
    }
}