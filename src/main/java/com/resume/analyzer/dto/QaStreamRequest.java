package com.resume.analyzer.dto;

import lombok.Data;

@Data
public class QaStreamRequest {
    private Long userId;
    private String question;
}
