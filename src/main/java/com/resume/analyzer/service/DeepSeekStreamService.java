package com.resume.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * DeepSeek 流式对话：SSE 解析并逐 token 回调。
 */
@Service
public class DeepSeekStreamService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String MODEL = "deepseek-chat";

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 流式请求：将 prompt 发给 DeepSeek，每收到一段 content 就调用 onChunk。
     */
    public void streamChat(String prompt, Consumer<String> onChunk) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            onChunk.accept("未配置 DeepSeek API Key，请在 application.properties 中设置 deepseek.api-key。");
            return;
        }
        String body = objectMapper.writeValueAsString(Map.of(
            "model", MODEL,
            "stream", true,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        ));
        Request request = new Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(body, JSON))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String err = response.code() + " " + (response.body() != null ? response.body().string() : "");
                onChunk.accept("请求失败: " + err);
                return;
            }
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    if (data.isEmpty()) continue;
                    try {
                        JsonNode root = objectMapper.readTree(data);
                        JsonNode choices = root.path("choices");
                        if (choices.isEmpty()) continue;
                        JsonNode delta = choices.get(0).path("delta");
                        JsonNode content = delta.path("content");
                        if (!content.isMissingNode() && content.isTextual()) {
                            String text = content.asText();
                            if (text != null && !text.isEmpty())
                                onChunk.accept(text);
                        }
                    } catch (Exception ignored) { }
                }
            }
        }
    }
}
