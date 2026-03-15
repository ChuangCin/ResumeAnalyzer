package com.resume.analyzer.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档：支持 PDF、DOCX、Markdown 等，上传后异步分块与向量化。
 */
@Data
@Entity
@Table(name = "knowledge_doc")
public class KnowledgeDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String fileName;

    /** 文件格式：pdf, docx, md, txt 等 */
    private String format;

    /** 存储路径（MinIO 对象键或本地路径） */
    private String filePath;

    /** processing=处理中, completed=已完成, failed=失败 */
    private String status;

    /** 分块数量（向量化完成后写入） */
    private Integer chunkCount;

    private LocalDateTime uploadTime;

    @PrePersist
    public void prePersist() {
        if (uploadTime == null) uploadTime = LocalDateTime.now();
    }
}
