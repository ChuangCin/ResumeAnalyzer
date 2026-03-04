package com.resume.analyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "match_result")
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long resumeId;

    private Long jobId;

    @Column(columnDefinition = "TEXT")
    private String report;

    private LocalDateTime createTime;
}