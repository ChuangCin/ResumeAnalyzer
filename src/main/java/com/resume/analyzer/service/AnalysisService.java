package com.resume.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.analyzer.common.Result;
import com.resume.analyzer.entity.AnalysisResult;
import com.resume.analyzer.entity.Resume;
import com.resume.analyzer.repository.AnalysisRepository;
import com.resume.analyzer.repository.ResumeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnalysisService {

    private final ResumeRepository resumeRepository;
    private final AnalysisRepository analysisRepository;
    private final AIService aiService;

    private final ObjectMapper mapper = new ObjectMapper();

    public AnalysisService(ResumeRepository resumeRepository,
                           AnalysisRepository analysisRepository,
                           AIService aiService) {

        this.resumeRepository = resumeRepository;
        this.analysisRepository = analysisRepository;
        this.aiService = aiService;
    }

    public Result<AnalysisResult> analyze(Long resumeId) {

        Resume resume = resumeRepository.findById(resumeId).orElse(null);

        if (resume == null) {
            return Result.error("简历不存在");
        }

        String content = resume.getContent();

        if (content == null || content.isEmpty()) {
            return Result.error("简历内容为空");
        }

        // 调用AI
        String aiJson = aiService.analyzeResume(content);

        AnalysisResult result = new AnalysisResult();
        result.setResumeId(resumeId);
        result.setCreateTime(LocalDateTime.now());

        try {

            JsonNode root = mapper.readTree(aiJson);

            // 核心评价
            result.setSummary(root.get("summary").asText());

            // 亮点
            StringBuilder highlights = new StringBuilder();
            for (JsonNode node : root.get("highlights")) {
                highlights.append(node.asText()).append(",");
            }
            result.setHighlights(highlights.toString());

            // 分数
            JsonNode scores = root.get("scores");

            result.setSkillScore(scores.get("skill").asInt());
            result.setExperienceScore(scores.get("experience").asInt());
            result.setStructureScore(scores.get("structure").asInt());
            result.setExpressionScore(scores.get("expression").asInt());
            result.setCompletenessScore(scores.get("completeness").asInt());

            result.setScore(root.get("totalScore").asInt());

            // ⭐ 改进建议
            if (root.has("suggestions")) {
                result.setSuggestions(root.get("suggestions").toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("AI解析失败");
        }

        analysisRepository.save(result);

        return Result.success(result);
    }
    public List<AnalysisResult> getByResumeId(Long resumeId) {
        return analysisRepository.findByResumeId(resumeId);
    }
}