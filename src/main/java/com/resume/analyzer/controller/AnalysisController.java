package com.resume.analyzer.controller;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.web.bind.annotation.*;
import com.resume.analyzer.common.Result;
import com.resume.analyzer.entity.AnalysisResult;
import com.resume.analyzer.service.AnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/{resumeId}")
    public Result<AnalysisResult> analyze(@PathVariable Long resumeId) {
        return analysisService.analyze(resumeId);
    }

    @GetMapping("/resume/{id}")
    public Result<?> getByResumeId(@PathVariable Long id) {

        List<AnalysisResult> list = analysisService.getByResumeId(id);

        if (list.isEmpty()) {
            return Result.error("没有分析结果");
        }

        AnalysisResult result = list.get(list.size() - 1);
        Map<String, Object> map = new HashMap<>();

        map.put("summary", result.getSummary());
        map.put("highlights", result.getHighlights());
        map.put("score", result.getScore());
        map.put("createTime", result.getCreateTime());

        map.put("skillScore", result.getSkillScore());
        map.put("experienceScore", result.getExperienceScore());
        map.put("structureScore", result.getStructureScore());
        map.put("expressionScore", result.getExpressionScore());
        map.put("completenessScore", result.getCompletenessScore());

        // ⭐ 这里解析 suggestions
        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode suggestionJson =
                    mapper.readTree(result.getSuggestions());

            map.put("suggestions", suggestionJson);

        } catch (Exception e) {
            map.put("suggestions", null);
        }

        return Result.success(map);
    }
}