package com.resume.analyzer.service;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.dto.ResumeListItemDTO;
import com.resume.analyzer.entity.AnalysisResult;
import com.resume.analyzer.entity.Resume;
import com.resume.analyzer.repository.AnalysisRepository;
import com.resume.analyzer.repository.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.resume.analyzer.utils.PdfUtil;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final AnalysisRepository analysisRepository;

    public ResumeService(ResumeRepository resumeRepository, AnalysisRepository analysisRepository) {
        this.resumeRepository = resumeRepository;
        this.analysisRepository = analysisRepository;
    }

    public Result upload(MultipartFile file, Long userId) throws IOException {

        // 保存路径
        String folder = System.getProperty("user.dir") + "/uploads/";//绝对路径
        File dir = new File(folder);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = file.getOriginalFilename();
        String filePath = folder + fileName;

        file.transferTo(new File(filePath));
        // 解析PDF
        String content = PdfUtil.parsePdf(filePath);

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(fileName);
        resume.setFilePath(filePath);
        resume.setContent(content);
        resume.setUploadTime(LocalDateTime.now());

        resumeRepository.save(resume);

        return Result.success(resume);
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
        List<AnalysisResult> analyses = analysisRepository.findByResumeId(resumeId);
        for (AnalysisResult ar : analyses) {
            analysisRepository.delete(ar);
        }
        resumeRepository.delete(r);
        return Result.success(null);
    }
}