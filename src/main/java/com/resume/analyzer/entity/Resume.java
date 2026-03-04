package com.resume.analyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "resume")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String fileName;

    private String filePath;

    private LocalDateTime uploadTime;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private String skills;
    private String education;
    private String experience;


}