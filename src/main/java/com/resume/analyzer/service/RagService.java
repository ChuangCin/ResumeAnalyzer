package com.resume.analyzer.service;

import com.resume.analyzer.entity.KnowledgeChunk;
import com.resume.analyzer.repository.KnowledgeChunkRepository;
import com.resume.analyzer.repository.KnowledgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RAG：优先向量检索（Redis Stack），未配置时回退关键词检索；DeepSeek 流式生成。
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final int TOP_K = 10;
    private static final Pattern WORD_BOUND = Pattern.compile("\\s+|[\\p{P}\\p{S}]");

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final KnowledgeService knowledgeService;
    private final EmbeddingService embeddingService;
    private final KnowledgeVectorStore vectorStore;
    private final DeepSeekStreamService deepSeekStreamService;

    public RagService(KnowledgeChunkRepository knowledgeChunkRepository,
                      KnowledgeRepository knowledgeRepository,
                      KnowledgeService knowledgeService,
                      EmbeddingService embeddingService,
                      KnowledgeVectorStore vectorStore,
                      DeepSeekStreamService deepSeekStreamService) {
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.knowledgeService = knowledgeService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.deepSeekStreamService = deepSeekStreamService;
    }

    private static final int SOURCE_PREVIEW_LEN = 200;

    /**
     * 流式回答：先检索相关分块，若有 onSources 则先回调「参考来源」预览及当前用户知识库条数，再流式生成。
     * onSources 接收 Map：snippets(List<String>), chunkCount(long)，便于前端区分「知识库为空」与「未检索到相关片段」。
     */
    public void streamAnswer(Long userId, String question, Consumer<String> onChunk,
                            Consumer<Map<String, Object>> onSources) throws IOException {
        if (userId == null || question == null || question.isBlank()) {
            onChunk.accept("请先登录并输入问题。");
            return;
        }
        long chunkCount = knowledgeChunkRepository.countByUserId(userId);
        long uploadedDocCount = knowledgeRepository.countByUserIdAndStatus(userId, "uploaded");
        boolean indexingTriggered = false;
        // 方案 B：如果该用户还没有任何 chunk，但存在待索引文档，则自动触发一次索引构建
        if (chunkCount == 0 && uploadedDocCount > 0) {
            indexingTriggered = knowledgeService.triggerIndexingIfNeeded(userId);
        }
        List<String> contextContents = retrieveContext(userId, question.trim(), TOP_K);
        if (onSources != null) {
            List<String> previews = contextContents.isEmpty() ? List.of()
                : contextContents.stream()
                    .map(s -> s == null ? "" : (s.length() <= SOURCE_PREVIEW_LEN ? s : s.substring(0, SOURCE_PREVIEW_LEN) + "…"))
                    .toList();
            onSources.accept(Map.of(
                "snippets", previews,
                "chunkCount", chunkCount,
                "uploadedDocCount", uploadedDocCount,
                "indexingTriggered", indexingTriggered
            ));
        }
        if (contextContents.isEmpty() && chunkCount > 0) {
            log.info("RAG 用户 {} 知识库共 {} 条，但本次检索未命中相关片段", userId, chunkCount);
        } else if (contextContents.isEmpty()) {
            if (uploadedDocCount > 0) {
                log.info("RAG 用户 {} 尚未生成 chunk（uploadedDocCount={}），已触发索引构建={}", userId, uploadedDocCount, indexingTriggered);
            } else {
                log.debug("RAG 用户 {} 知识库无内容（chunkCount=0, uploadedDocCount=0）", userId);
            }
        }
        String prompt = buildRagPrompt(question.trim(), contextContents);
        deepSeekStreamService.streamChat(prompt, onChunk);
    }

    /** 检索上下文：优先向量检索，否则关键词检索 */
    private List<String> retrieveContext(Long userId, String question, int topK) {
        if (embeddingService.isEnabled() && vectorStore.isAvailable()) {
            float[] queryVec = embeddingService.embed(question);
            if (queryVec != null) {
                List<String> fromVector = vectorStore.search(userId, queryVec, topK);
                if (!fromVector.isEmpty()) {
                    log.debug("RAG 使用向量检索，命中 {} 条", fromVector.size());
                    return fromVector;
                }
            }
        }
        log.debug("RAG 使用关键词检索（未配置 embedding 或向量库不可用/无结果）");
        List<KnowledgeChunk> chunks = retrieveTopChunksByKeyword(userId, question, topK);
        return chunks.stream()
            .map(KnowledgeChunk::getContent)
            .filter(c -> c != null && !c.isBlank())
            .toList();
    }

    /** 关键词检索：按问题词在块中命中数排序，取 topK */
    private List<KnowledgeChunk> retrieveTopChunksByKeyword(Long userId, String question, int topK) {
        List<KnowledgeChunk> all = knowledgeChunkRepository.findByUserIdOrderByKnowledgeDocIdAscSortOrderAsc(userId);
        if (all.isEmpty()) return all;
        Set<String> queryTerms = toTerms(question);
        if (queryTerms.isEmpty()) return all.stream().limit(topK).toList();

        List<ScoredChunk> scored = new ArrayList<>();
        for (KnowledgeChunk c : all) {
            String content = c.getContent();
            if (content == null || content.isBlank()) continue;
            Set<String> chunkTerms = toTerms(content);
            long score = queryTerms.stream().filter(chunkTerms::contains).count();
            scored.add(new ScoredChunk(c, score));
        }
        return scored.stream()
            .sorted(Comparator.comparingLong(ScoredChunk::score).reversed())
            .limit(topK)
            .map(ScoredChunk::chunk)
            .toList();
    }

    private static Set<String> toTerms(String text) {
        if (text == null || text.isBlank()) return Set.of();
        return Arrays.stream(WORD_BOUND.split(text))
            .map(String::trim)
            .filter(s -> s.length() > 0)
            .map(s -> s.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
    }

    private String buildRagPrompt(String question, List<String> contextContents) {
        if (contextContents == null || contextContents.isEmpty()) {
            return "用户当前知识库中暂无上传的参考资料。请根据你的知识简要、友好地回答以下问题；若无法回答请说明。\n\n问题：" + question;
        }
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < contextContents.size(); i++) {
            String content = contextContents.get(i);
            if (content != null && !content.isBlank())
                context.append("【").append(i + 1).append("】 ").append(content.trim()).append("\n\n");
        }
        return "请严格根据以下「参考资料」回答用户问题。只基于资料内容回答；若资料中无相关内容，可说明并简要补充常识。不要编造资料中不存在的信息。\n\n参考资料：\n\n"
            + context
            + "\n用户问题：" + question;
    }

    private record ScoredChunk(KnowledgeChunk chunk, long score) {}
}
