package com.resume.analyzer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.analyzer.common.Result;
import com.resume.analyzer.entity.MatchResult;
import com.resume.analyzer.service.MatchService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/match")
public class MatchController {

    private final MatchService matchService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public MatchResult match(
            @RequestParam Long resumeId,
            @RequestParam Long jobId
    ) {
        return matchService.match(resumeId, jobId);
    }

    /** 一键匹配：异步执行，立即返回，避免长时间阻塞与超时 */
    @PostMapping("/resume/{resumeId}/run")
    public Result<String> runMatchForResume(@PathVariable Long resumeId) {
        CompletableFuture.runAsync(() -> {
            try {
                matchService.runMatchForResume(resumeId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return Result.success("匹配任务已提交，请稍后刷新页面查看结果");
    }

    @GetMapping("/resume/{resumeId}")
    public Result<List<Map<String, Object>>> getByResumeId(@PathVariable Long resumeId) {
        List<MatchResult> list = matchService.getByResumeId(resumeId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (MatchResult mr : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", mr.getId());
            item.put("jobId", mr.getJobId());
            item.put("createTime", mr.getCreateTime());
            if (mr.getReport() != null && !mr.getReport().isBlank()) {
                try {
                    JsonNode root = objectMapper.readTree(mr.getReport());
                    item.put("jobName", root.has("jobName") ? root.get("jobName").asText() : "岗位");
                    item.put("matchScore", root.has("matchScore") ? root.get("matchScore").asInt() : 0);
                    item.put("advantages", root.has("advantages") ? objectMapper.convertValue(root.get("advantages"), List.class) : List.of());
                    item.put("disadvantages", root.has("disadvantages") ? objectMapper.convertValue(root.get("disadvantages"), List.class) : List.of());
                    item.put("suggestions", root.has("suggestions") ? objectMapper.convertValue(root.get("suggestions"), List.class) : List.of());
                } catch (Exception e) {
                    item.put("jobName", "岗位");
                    item.put("matchScore", 0);
                    item.put("advantages", List.of());
                    item.put("disadvantages", List.of());
                    item.put("suggestions", List.of());
                }
            } else {
                item.put("jobName", "岗位");
                item.put("matchScore", 0);
                item.put("advantages", List.of());
                item.put("disadvantages", List.of());
                item.put("suggestions", List.of());
            }
            out.add(item);
        }
        return Result.success(out);
    }
}