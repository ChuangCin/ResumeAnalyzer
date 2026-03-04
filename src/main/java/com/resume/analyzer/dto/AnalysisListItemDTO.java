package com.resume.analyzer.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnalysisListItemDTO {
    private Long id;
    private Long resumeId;
    private Long userId;
    private String username;
    private Integer score;
    private String summary;
    private LocalDateTime createTime;
}
