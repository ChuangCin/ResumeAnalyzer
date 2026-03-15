package com.resume.analyzer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "resume")
@TableName("resume")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String fileName;

    /** 本地路径或 MinIO 对象键（objectKey），用于删除时定位文件 */
    private String filePath;

    /** MinIO 文件访问地址（上传到 MinIO 时写入） */
    @Column(name = "file_url")
    private String fileUrl;

    private LocalDateTime uploadTime;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private String skills;
    private String education;
    private String experience;


}