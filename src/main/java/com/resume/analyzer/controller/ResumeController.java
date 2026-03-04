package com.resume.analyzer.controller;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.dto.ResumeListItemDTO;
import com.resume.analyzer.service.ResumeService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/resume")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping("/upload")
    public Result upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId
    ) throws Exception {

        return resumeService.upload(file, userId);
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