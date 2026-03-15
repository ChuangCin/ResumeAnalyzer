package com.resume.analyzer.service;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.dto.ResumeListItemDTO;
import com.resume.analyzer.entity.AnalysisResult;
import com.resume.analyzer.entity.Resume;
import com.resume.analyzer.repository.AnalysisRepository;
import com.resume.analyzer.repository.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.resume.analyzer.utils.PdfUtil;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final AnalysisRepository analysisRepository;
    private final MinioService minioService;

    public ResumeService(ResumeRepository resumeRepository, AnalysisRepository analysisRepository, MinioService minioService) {
        this.resumeRepository = resumeRepository;
        this.analysisRepository = analysisRepository;
        this.minioService = minioService;
    }

    /**
     * 上传简历：先落临时文件解析 PDF，再上传到 MinIO，最后保存数据库并返回带 fileUrl 的 Resume。
     */
    public Result upload(MultipartFile file, Long userId) throws IOException {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf";
        String folder = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(folder);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File tempFile = new File(folder + System.currentTimeMillis() + "_" + fileName);
        try {
            file.transferTo(tempFile);
            String content = PdfUtil.parsePdf(tempFile.getAbsolutePath());

            String objectKey = "resume/" + userId + "_" + System.currentTimeMillis() + "_" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String contentType = file.getContentType() != null ? file.getContentType() : "application/pdf";
            String fileUrl = null;
            String pathToStore = objectKey;

            try {
                fileUrl = minioService.uploadFile(tempFile, contentType, objectKey);
            } catch (Exception e) {
                // MinIO 不可用时回退到本地存储，保留原有能力
                String localPath = tempFile.getAbsolutePath();
                pathToStore = localPath;
                File dest = new File(folder + fileName);
                for (int i = 0; dest.exists(); i++) {
                    dest = new File(folder + (i == 0 ? fileName : i + "_" + fileName));
                }
                tempFile.renameTo(dest);
                pathToStore = dest.getAbsolutePath();
            }

            Resume resume = new Resume();
            resume.setUserId(userId);
            resume.setFileName(fileName);
            resume.setFilePath(pathToStore);
            resume.setFileUrl(fileUrl);
            resume.setContent(content);
            resume.setUploadTime(LocalDateTime.now());
            resumeRepository.save(resume);
            return Result.success(resume);
        } catch (Exception e) {
            if (tempFile.exists()) tempFile.delete();
            throw new IOException("简历上传或解析失败: " + e.getMessage());
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public List<ResumeListItemDTO> listByUserId(Long userId) {
        List<Resume> resumes = resumeRepository.findByUserIdOrderByUploadTimeDesc(userId);
        List<ResumeListItemDTO> items = new ArrayList<>();
        for (Resume r : resumes) {
            AnalysisResult latest = analysisRepository.findTopByResumeIdOrderByCreateTimeDesc(r.getId());
            Integer score = latest != null ? latest.getScore() : null;
            String status = latest != null ? "已完成" : "待面试";
            items.add(new ResumeListItemDTO(
                r.getId(),
                r.getFileName(),
                r.getUploadTime(),
                score,
                status
            ));
        }
        return items;
    }

    /** 分页查询当前用户的简历列表，返回 { list, total } */
    public Map<String, Object> listByUserIdPage(Long userId, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Resume> pageResult = resumeRepository.findByUserIdOrderByUploadTimeDesc(userId, pageable);
        List<ResumeListItemDTO> items = new ArrayList<>();
        for (Resume r : pageResult.getContent()) {
            AnalysisResult latest = analysisRepository.findTopByResumeIdOrderByCreateTimeDesc(r.getId());
            Integer score = latest != null ? latest.getScore() : null;
            String status = latest != null ? "已完成" : "待面试";
            items.add(new ResumeListItemDTO(
                r.getId(),
                r.getFileName(),
                r.getUploadTime(),
                score,
                status
            ));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", items);
        result.put("total", pageResult.getTotalElements());
        return result;
    }

    /** 用户删除自己的简历及其分析记录 */
    public Result<Void> deleteResume(Long resumeId, Long userId) {
        if (resumeId == null || userId == null) {
            return Result.error("参数错误");
        }
        Resume r = resumeRepository.findById(resumeId).orElse(null);
        if (r == null) {
            return Result.error("简历不存在");
        }
        if (!r.getUserId().equals(userId)) {
            return Result.error("无权删除该简历");
        }
        if (r.getFilePath() != null && r.getFilePath().startsWith("resume/")) {
            try {
                minioService.deleteFile(r.getFilePath());
            } catch (Exception e) {
                // 忽略 MinIO 删除失败（如文件已不存在）
            }
        }
        List<AnalysisResult> analyses = analysisRepository.findByResumeId(resumeId);
        for (AnalysisResult ar : analyses) {
            analysisRepository.delete(ar);
        }
        resumeRepository.delete(r);
        return Result.success(null);
    }
}