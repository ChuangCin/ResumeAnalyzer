package com.resume.analyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "analysis_result")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long resumeId;

    @Column(length = 2000)
    private String summary; // 核心评价

    @Column(length = 2000)
    private String highlights; // 优势亮点

    private Integer score;

    private Integer skillScore;
    private Integer experienceScore;
    private Integer structureScore;
    private Integer expressionScore;
    private Integer completenessScore;

    @Column(length = 5000)
    private String suggestions; // 改进建议

    private LocalDateTime createTime;
}