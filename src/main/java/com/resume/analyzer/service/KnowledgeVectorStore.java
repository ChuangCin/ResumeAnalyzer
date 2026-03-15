package com.resume.analyzer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.schemafields.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Redis Stack 向量存储：基于 RediSearch 的 HNSW 向量索引。
 * 需运行 Redis Stack（含 RediSearch 模块）。
 */
@Service
public class KnowledgeVectorStore {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeVectorStore.class);
    private static final String INDEX_NAME = "idx:knowledge_chunk";
    private static final String KEY_PREFIX = "chunk:";

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    private JedisPooled jedis;
    private int vectorDim = 1536;

    @PostConstruct
    public void init() {
        try {
            String uri = (password != null && !password.isBlank())
                ? "redis://:" + password + "@" + host + ":" + port + "/" + database
                : "redis://" + host + ":" + port + "/" + database;
            jedis = new JedisPooled(URI.create(uri));
            jedis.ping();
        } catch (Exception e) {
            log.warn("Redis Stack 连接失败，向量检索将不可用: {}", e.getMessage());
            jedis = null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (jedis != null) {
            jedis.close();
        }
    }

    /** 设置向量维度（需与 Embedding 模型一致） */
    public void setVectorDim(int dim) {
        this.vectorDim = dim > 0 ? dim : 1536;
    }

    /** 是否可用 */
    public boolean isAvailable() {
        return jedis != null;
    }

    /** 确保索引存在 */
    public void ensureIndex() {
        if (jedis == null) return;
        try {
            try {
                jedis.ftInfo(INDEX_NAME);
                return;
            } catch (JedisDataException e) {
                if (!e.getMessage().contains("no such index")) throw e;
            }
            SchemaField[] schema = {
                TextField.of("content"),
                TagField.of("user_id"),
                VectorField.builder()
                    .fieldName("embedding")
                    .algorithm(redis.clients.jedis.search.schemafields.VectorField.VectorAlgorithm.HNSW)
                    .attributes(Map.of(
                        "TYPE", "FLOAT32",
                        "DIM", String.valueOf(vectorDim),
                        "DISTANCE_METRIC", "COSINE"
                    ))
                    .build()
            };
            jedis.ftCreate(INDEX_NAME,
                FTCreateParams.createParams().addPrefix(KEY_PREFIX).on(IndexDataType.HASH),
                schema
            );
            log.info("Redis 向量索引 {} 创建成功，维度 {}", INDEX_NAME, vectorDim);
        } catch (Exception e) {
            log.warn("创建向量索引失败: {}", e.getMessage());
        }
    }

    /** 保存分块向量 */
    public void saveVector(long chunkId, long userId, String content, float[] embedding) {
        if (jedis == null || embedding == null || embedding.length != vectorDim) return;
        String key = KEY_PREFIX + chunkId;
        byte[] vecBytes = floatsToBytes(embedding);
        jedis.hset(key, Map.of(
            "content", content != null ? content : "",
            "user_id", String.valueOf(userId)
        ));
        jedis.hset(key.getBytes(), "embedding".getBytes(), vecBytes);
    }

    /** 向量检索：按用户过滤，返回 topK 条 content */
    public List<String> search(long userId, float[] queryEmbedding, int topK) {
        List<String> result = new ArrayList<>();
        if (jedis == null || queryEmbedding == null || queryEmbedding.length != vectorDim) return result;
        try {
            byte[] vecBytes = floatsToBytes(queryEmbedding);
            Query q = new Query("(@user_id:{" + userId + "})=>[KNN " + topK + " @embedding $BLOB AS score]")
                .returnFields("content", "score")
                .addParam("BLOB", vecBytes)
                .setSortBy("score", true)
                .dialect(2);
            SearchResult sr = jedis.ftSearch(INDEX_NAME, q);
            for (Document doc : sr.getDocuments()) {
                String content = doc.getString("content");
                if (content != null && !content.isBlank())
                    result.add(content);
            }
        } catch (Exception e) {
            log.warn("向量检索失败: {}", e.getMessage());
        }
        return result;
    }

    /** 按 chunkId 列表删除 */
    public void deleteByChunkIds(List<Long> chunkIds) {
        if (jedis == null || chunkIds == null || chunkIds.isEmpty()) return;
        for (Long id : chunkIds) {
            jedis.del(KEY_PREFIX + id);
        }
    }

    private static byte[] floatsToBytes(float[] floats) {
        byte[] bytes = new byte[Float.BYTES * floats.length];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(floats);
        return bytes;
    }
}
