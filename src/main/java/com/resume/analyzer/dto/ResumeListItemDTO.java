package com.resume.analyzer.dto;

import java.time.LocalDateTime;

public class ResumeListItemDTO {
    private Long id;
    private String fileName;
    private LocalDateTime uploadTime;
    private Integer score;
    private String interviewStatus;

    public ResumeListItemDTO() {}

    public ResumeListItemDTO(Long id, String fileName, LocalDateTime uploadTime, Integer score, String interviewStatus) {
        this.id = id;
        this.fileName = fileName;
        this.uploadTime = uploadTime;
        this.score = score;
        this.interviewStatus = interviewStatus;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getInterviewStatus() { return interviewStatus; }
    public void setInterviewStatus(String interviewStatus) { this.interviewStatus = interviewStatus; }
}
