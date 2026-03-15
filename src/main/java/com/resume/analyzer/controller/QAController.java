package com.resume.analyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.analyzer.dto.QaStreamRequest;
import com.resume.analyzer.service.EmbeddingService;
import com.resume.analyzer.service.KnowledgeVectorStore;
import com.resume.analyzer.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于知识库的 RAG 流式问答，SSE 格式（打字机式输出）。
 */
@RestController
@RequestMapping("/qa")
public class QAController {

    private final RagService ragService;
    private final EmbeddingService embeddingService;
    private final KnowledgeVectorStore vectorStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QAController(RagService ragService, EmbeddingService embeddingService,
                        KnowledgeVectorStore vectorStore) {
        this.ragService = ragService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    /** 查询当前是否启用 embedding 与向量库（GET/POST 均支持，避免代理只放行 POST） */
    @RequestMapping(value = "/embedding-status", method = { RequestMethod.GET, RequestMethod.POST })
    public Map<String, Object> embeddingStatus() {
        return Map.of(
            "embeddingEnabled", embeddingService.isEnabled(),
            "vectorStoreAvailable", vectorStore.isAvailable(),
            "usingVectorSearch", embeddingService.isEnabled() && vectorStore.isAvailable()
        );
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody stream(@RequestBody QaStreamRequest request) {
        Long userId = parseUserId(request != null ? request.getUserId() : null);
        String question = request != null && request.getQuestion() != null ? request.getQuestion().trim() : "";
        if (userId == null || question.isEmpty()) {
            return out -> {
                try {
                    writeChunk(out, "请先登录并输入问题。");
                    out.flush();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
        return out -> {
            try {
                ragService.streamAnswer(userId, question,
                    chunk -> {
                        try {
                            String line = "data: " + objectMapper.writeValueAsString(Map.of("content", chunk)) + "\n\n";
                            out.write(line.getBytes(StandardCharsets.UTF_8));
                            out.flush();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    payload -> {
                        try {
                            Map<String, Object> event = new HashMap<>(payload);
                            event.put("type", "sources");
                            String line = "data: " + objectMapper.writeValueAsString(event) + "\n\n";
                            out.write(line.getBytes(StandardCharsets.UTF_8));
                            out.flush();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
            } catch (Exception e) {
                String err = e.getMessage() != null ? e.getMessage() : "回答生成失败";
                try {
                    writeChunk(out, "抱歉，发生错误：" + err);
                    out.flush();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    private void writeChunk(OutputStream out, String text) throws Exception {
        String line = "data: " + objectMapper.writeValueAsString(Map.of("content", text)) + "\n\n";
        out.write(line.getBytes(StandardCharsets.UTF_8));
    }

    /** 防御性解析：前端可能传字符串 "7"，统一转为 Long */
    private static Long parseUserId(Object userId) {
        if (userId == null) return null;
        if (userId instanceof Number n) return n.longValue();
        if (userId instanceof String s) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
