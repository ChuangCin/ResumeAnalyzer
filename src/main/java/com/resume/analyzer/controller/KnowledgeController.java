package com.resume.analyzer.controller;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.entity.KnowledgeChunk;
import com.resume.analyzer.entity.KnowledgeDoc;
import com.resume.analyzer.repository.KnowledgeChunkRepository;
import com.resume.analyzer.service.KnowledgeService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeChunkRepository knowledgeChunkRepository;

    public KnowledgeController(KnowledgeService knowledgeService, KnowledgeChunkRepository knowledgeChunkRepository) {
        this.knowledgeService = knowledgeService;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
    }

    @GetMapping("/list")
    public Result<List<KnowledgeDoc>> list(@RequestParam("userId") Long userId) {
        return knowledgeService.listByUserId(userId);
    }

    @PostMapping("/upload")
    public Result<KnowledgeDoc> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId
    ) {
        try {
            return knowledgeService.upload(file, userId);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return Result.error("上传失败: " + msg);
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        return knowledgeService.delete(id, userId);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam("userId") Long userId) {
        return knowledgeService.stats(userId);
    }

    /** 方案 B：手动触发一次索引构建（把 uploaded 文档置为 processing 并启动异步分块/向量化） */
    @PostMapping("/trigger-index")
    public Result<Map<String, Object>> triggerIndex(@RequestParam("userId") Long userId) {
        boolean triggered = knowledgeService.triggerIndexingIfNeeded(userId);
        return Result.success(Map.of("triggered", triggered));
    }

    /** 强制重建索引：用于“已完成但无分块/检索不到”的排障 */
    @PostMapping("/force-reindex")
    public Result<Map<String, Object>> forceReindex(@RequestParam("docId") Long docId, @RequestParam("userId") Long userId) {
        boolean ok = knowledgeService.forceReindex(docId, userId);
        return Result.success(Map.of("ok", ok));
    }

    /** 诊断：查看当前用户 chunk 数量与抽样内容（确认是否真的入库） */
    @GetMapping("/debug/chunks")
    public Result<Map<String, Object>> debugChunks(@RequestParam("userId") Long userId) {
        long count = knowledgeChunkRepository.countByUserId(userId);
        List<KnowledgeChunk> list = knowledgeChunkRepository.findByUserIdOrderByKnowledgeDocIdAscSortOrderAsc(userId);
        List<String> samples = list.stream()
            .limit(3)
            .map(c -> c.getContent() != null && c.getContent().length() > 120 ? c.getContent().substring(0, 120) + "…" : (c.getContent() != null ? c.getContent() : ""))
            .toList();
        return Result.success(Map.of("chunkCount", count, "samples", samples));
    }
}
