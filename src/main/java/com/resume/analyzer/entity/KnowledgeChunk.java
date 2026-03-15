package com.resume.analyzer.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 知识库文档分块：用于 RAG 检索。当前按文本检索（关键词/相似度），可扩展存储向量。
 */
@Data
@Entity
@Table(name = "knowledge_chunk", indexes = {
    @Index(name = "idx_chunk_user_doc", columnList = "userId,knowledgeDocId")
})
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long knowledgeDocId;

    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    /** 块在同一文档内的顺序 */
    private Integer sortOrder;

    public static KnowledgeChunk of(Long docId, Long userId, String content, int sortOrder) {
        KnowledgeChunk c = new KnowledgeChunk();
        c.setKnowledgeDocId(docId);
        c.setUserId(userId);
        c.setContent(content);
        c.setSortOrder(sortOrder);
        return c;
    }
}
