package com.resume.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 兼容的 Embedding API：用于将文本转为向量。
 * 支持 OpenAI、国内兼容接口（如 DashScope、智谱等）。
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Value("${embedding.api-url:}")
    private String apiUrl;

    @Value("${embedding.api-key:}")
    private String apiKey;

    @Value("${embedding.model:text-embedding-3-small}")
    private String model;

    @Value("${embedding.dim:1536}")
    private int dim;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 是否已配置（api-url 与 api-key 非空） */
    public boolean isEnabled() {
        return apiUrl != null && !apiUrl.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    /**
     * 单条文本向量化。
     *
     * @return 向量数组，未配置或失败时返回 null
     */
    public float[] embed(String text) {
        if (!isEnabled() || text == null || text.isBlank()) return null;
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "input", text
            ));
            Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body, JSON))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("Embedding API 请求失败: {} {}", response.code(), response.body() != null ? response.body().string() : "");
                    return null;
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                JsonNode data = root.path("data");
                if (!data.isArray() || data.isEmpty()) return null;
                JsonNode emb = data.get(0).path("embedding");
                if (!emb.isArray()) return null;
                float[] vec = new float[emb.size()];
                for (int i = 0; i < emb.size(); i++) vec[i] = (float) emb.get(i).asDouble();
                return vec;
            }
        } catch (Exception e) {
            log.warn("Embedding 调用异常: {}", e.getMessage());
            return null;
        }
    }

    /** 向量维度（用于 Redis 索引） */
    public int getDim() {
        return dim > 0 ? dim : 1536;
    }
}
