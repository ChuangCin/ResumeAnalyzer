package com.resume.analyzer.service;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.entity.KnowledgeChunk;
import com.resume.analyzer.entity.KnowledgeDoc;
import com.resume.analyzer.repository.KnowledgeChunkRepository;
import com.resume.analyzer.repository.KnowledgeRepository;
import com.resume.analyzer.utils.ChunkUtil;
import com.resume.analyzer.utils.DocumentExtractUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 知识库文档：上传、分块落库；若配置 embedding 则写入 Redis 向量库，供 RAG 语义检索。
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeRepository knowledgeRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final MinioService minioService;
    private final EmbeddingService embeddingService;
    private final KnowledgeVectorStore vectorStore;
    private final KnowledgeService self;

    public KnowledgeService(KnowledgeRepository knowledgeRepository,
                            KnowledgeChunkRepository knowledgeChunkRepository,
                            MinioService minioService,
                            EmbeddingService embeddingService,
                            KnowledgeVectorStore vectorStore,
                            @Lazy KnowledgeService self) {
        this.knowledgeRepository = knowledgeRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.minioService = minioService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.self = self;
    }

    public Result<List<KnowledgeDoc>> listByUserId(Long userId) {
        if (userId == null) return Result.error("请先登录");
        List<KnowledgeDoc> list = knowledgeRepository.findByUserIdOrderByUploadTimeDesc(userId);
        return Result.success(list);
    }

    public Result<KnowledgeDoc> upload(MultipartFile file, Long userId) throws IOException {
        if (userId == null) return Result.error("请先登录");
        if (file == null || file.isEmpty()) return Result.error("请选择文件");
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) return Result.error("无效文件名");

        String ext = "";
        int i = original.lastIndexOf('.');
        if (i >= 0) ext = original.substring(i + 1).toLowerCase();
        if (!ext.matches("pdf|docx?|md|markdown|txt")) {
            return Result.error("仅支持 PDF、DOCX、Markdown、TXT 格式");
        }

        String objectKey = "knowledge/" + userId + "/" + UUID.randomUUID() + "_" + original;
        try {
            minioService.uploadStream(file.getInputStream(), file.getSize(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream", objectKey);
        } catch (Exception e) {
            return Result.error("文件存储失败: " + (e.getMessage() != null ? e.getMessage() : "请检查 MinIO 配置"));
        }

        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setUserId(userId);
        doc.setFileName(original);
        doc.setFormat(ext);
        doc.setFilePath(objectKey);
        // 上传后立即异步构建索引
        doc.setStatus("processing");
        doc.setChunkCount(0);
        doc.setUploadTime(java.time.LocalDateTime.now());
        KnowledgeDoc saved = knowledgeRepository.save(doc);
        final Long savedId = saved.getId();
        final Long docUserId = saved.getUserId();
        final String filePath = saved.getFilePath();
        final String format = saved.getFormat();
        CompletableFuture
            .runAsync(() -> self.processDocChunks(savedId, docUserId, filePath, format))
            .orTimeout(10, TimeUnit.MINUTES)
            .exceptionally(ex -> {
                self.markDocFailed(savedId, "索引超时或异常: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
                return null;
            });
        return Result.success(saved);
    }

    /**
     * 按需触发索引：为该用户找到最早的 uploaded 文档，标记 processing 并启动异步分块/向量化。
     * @return true 表示本次成功触发；false 表示无需触发或触发失败
     */
    public boolean triggerIndexingIfNeeded(Long userId) {
        if (userId == null) return false;
        // 已经有在建索引的文档时不重复触发
        boolean hasProcessing = knowledgeRepository.countByUserIdAndStatus(userId, "processing") > 0;
        if (hasProcessing) return false;
        List<KnowledgeDoc> pending = knowledgeRepository.findByUserIdAndStatusOrderByUploadTimeAsc(userId, "uploaded");
        if (pending == null || pending.isEmpty()) return false;
        KnowledgeDoc doc = pending.get(0);
        return triggerIndexingForDoc(doc.getId(), userId);
    }

    /** 触发单个文档构建索引（将状态置为 processing 并异步处理） */
    @Transactional(rollbackFor = Exception.class)
    public boolean triggerIndexingForDoc(Long docId, Long userId) {
        if (docId == null || userId == null) return false;
        KnowledgeDoc doc = knowledgeRepository.findById(docId).orElse(null);
        if (doc == null) return false;
        if (!userId.equals(doc.getUserId())) return false;
        if (!"uploaded".equalsIgnoreCase(doc.getStatus())) return false;
        doc.setStatus("processing");
        knowledgeRepository.save(doc);

        final String filePath = doc.getFilePath();
        final String format = doc.getFormat();
        CompletableFuture
            .runAsync(() -> self.processDocChunks(docId, userId, filePath, format))
            .orTimeout(10, TimeUnit.MINUTES)
            .exceptionally(ex -> {
                self.markDocFailed(docId, "索引超时或异常: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
                return null;
            });
        return true;
    }

    /**
     * 异步：从 MinIO 拉取文档 → 解析正文 → 分块落库 → 向量化写入 Redis（若已配置 embedding）→ 更新状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void processDocChunks(Long docId, Long userId, String filePath, String format) {
        try {
            final boolean embeddingEnabled = embeddingService.isEnabled();
            final boolean vectorStoreAvailable = vectorStore.isAvailable();
            final int configuredDim = embeddingService.getDim();
            log.info("知识库索引阶段 docId={}, userId={}, embeddingEnabled={}, vectorStoreAvailable={}, configuredDim={}, filePath={}",
                docId, userId, embeddingEnabled, vectorStoreAvailable, configuredDim, filePath);
            if (embeddingEnabled && vectorStoreAvailable) {
                vectorStore.setVectorDim(configuredDim);
                vectorStore.ensureIndex();
                log.info("知识库索引阶段 docId={}, ensureIndex done", docId);
            }
            try (InputStream is = minioService.getObjectStream(filePath)) {
                log.info("知识库索引阶段 docId={}, MinIO stream opened", docId);
                String text = DocumentExtractUtil.extractText(format, is);
                if (text == null) text = "";
                log.info("知识库索引构建开始 docId={}, userId={}, format={}, textLen={}", docId, userId, format, text.length());
                List<String> chunks = ChunkUtil.split(text);
                log.info("知识库索引阶段 docId={}, split done, chunkCount={}", docId, chunks != null ? chunks.size() : 0);
                int order = 0;
                int savedChunks = 0;
                long embedCalls = 0;
                long embedOk = 0;
                long embedFail = 0;
                long embedTotalMs = 0;
                long vectorSaved = 0;
                Integer firstVectorSize = null;
                for (String content : chunks) {
                    if (content == null || content.isBlank()) continue;
                    KnowledgeChunk chunk = KnowledgeChunk.of(docId, userId, content, order++);
                    chunk = knowledgeChunkRepository.save(chunk);
                    savedChunks++;
                    if (embeddingEnabled && vectorStoreAvailable) {
                        embedCalls++;
                        long t0 = System.nanoTime();
                        float[] vec = embeddingService.embed(content);
                        long costMs = (System.nanoTime() - t0) / 1_000_000;
                        embedTotalMs += costMs;
                        if (vec != null) {
                            embedOk++;
                            if (firstVectorSize == null) firstVectorSize = vec.length;
                            vectorStore.saveVector(chunk.getId(), userId, content, vec);
                            vectorSaved++;
                        } else {
                            embedFail++;
                        }
                    }
                }
                if (embeddingEnabled && vectorStoreAvailable) {
                    long avgMs = embedCalls > 0 ? (embedTotalMs / embedCalls) : 0;
                    log.info("知识库 embedding 汇总 docId={}, userId={}, embeddingEnabled={}, vectorStoreAvailable={}, configuredDim={}, embedCalls={}, ok={}, fail={}, totalMs={}, avgMs={}, firstVectorSize={}, vectorSaved={}",
                        docId, userId, embeddingEnabled, vectorStoreAvailable, configuredDim,
                        embedCalls, embedOk, embedFail, embedTotalMs, avgMs, firstVectorSize, vectorSaved);
                } else {
                    log.info("知识库 embedding 汇总 docId={}, userId={}, embeddingEnabled={}, vectorStoreAvailable={}, configuredDim={}, embedCalls=0",
                        docId, userId, embeddingEnabled, vectorStoreAvailable, configuredDim);
                }
                log.info("知识库索引构建完成 docId={}, userId={}, savedChunks={}", docId, userId, savedChunks);
                if (savedChunks <= 0) {
                    markDocFailed(docId, "解析结果为空或未生成分块");
                } else {
                    markDocCompleted(docId);
                }
            }
        } catch (Exception e) {
            log.warn("知识库索引构建失败 docId={}, userId={}, err={}", docId, userId, e.getMessage());
            markDocFailed(docId, e.getMessage());
        }
    }

    /** 在独立事务中更新文档状态为已完成（chunkCount 取实际数量） */
    @Transactional(rollbackFor = Exception.class)
    public void markDocCompleted(Long docId) {
        knowledgeRepository.findById(docId).ifPresent(doc -> {
            doc.setStatus("completed");
            doc.setChunkCount((int) knowledgeChunkRepository.countByKnowledgeDocId(docId));
            knowledgeRepository.save(doc);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void markDocFailed(Long docId, String reason) {
        knowledgeRepository.findById(docId).ifPresent(doc -> {
            doc.setStatus("failed");
            doc.setChunkCount(0);
            knowledgeRepository.save(doc);
        });
    }

    /** 强制重建索引：删除旧 chunk 与向量，置为 processing 并异步重新分块/向量化 */
    public boolean forceReindex(Long docId, Long userId) {
        if (docId == null || userId == null) return false;
        KnowledgeDoc doc = knowledgeRepository.findById(docId).orElse(null);
        if (doc == null) return false;
        if (!userId.equals(doc.getUserId())) return false;
        List<Long> chunkIds = knowledgeChunkRepository.findIdsByKnowledgeDocId(docId);
        vectorStore.deleteByChunkIds(chunkIds);
        knowledgeChunkRepository.deleteByKnowledgeDocId(docId);
        doc.setStatus("processing");
        doc.setChunkCount(0);
        knowledgeRepository.save(doc);
        final String filePath = doc.getFilePath();
        final String format = doc.getFormat();
        CompletableFuture.runAsync(() -> self.processDocChunks(docId, userId, filePath, format));
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long docId, Long userId) {
        if (userId == null) return Result.error("请先登录");
        try {
            KnowledgeDoc doc = knowledgeRepository.findById(docId).orElse(null);
            if (doc == null) return Result.error("文档不存在");
            if (!doc.getUserId().equals(userId)) return Result.error("无权删除");
            List<Long> chunkIds = knowledgeChunkRepository.findIdsByKnowledgeDocId(docId);
            vectorStore.deleteByChunkIds(chunkIds);
            knowledgeChunkRepository.deleteByKnowledgeDocId(docId);
            try {
                if (doc.getFilePath() != null && !doc.getFilePath().isEmpty()) {
                    minioService.deleteFile(doc.getFilePath());
                }
            } catch (Exception e) {
                log.warn("删除知识库文档时删除 MinIO 文件失败 docId={}, path={}, err={}",
                    docId, doc.getFilePath(), e.getMessage());
            }
            knowledgeRepository.delete(doc);
            return Result.success(null);
        } catch (Exception e) {
            log.warn("删除知识库文档失败 docId={}, userId={}, err={}", docId, userId, e.getMessage());
            return Result.error("删除失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    public Result<Map<String, Object>> stats(Long userId) {
        if (userId == null) return Result.error("请先登录");
        long docCount = knowledgeRepository.countByUserId(userId);
        List<KnowledgeDoc> list = knowledgeRepository.findByUserIdOrderByUploadTimeDesc(userId);
        int chunkCount = list.stream().mapToInt(d -> d.getChunkCount() != null ? d.getChunkCount() : 0).sum();
        Map<String, Object> map = new HashMap<>();
        map.put("docCount", (int) docCount);
        map.put("chunkCount", chunkCount);
        return Result.success(map);
    }
}
