package com.resume.analyzer.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 发送者 ID（管理员） */
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    /** 接收者 ID（用户） */
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    /** 标题 */
    private String title;

    /** 内容 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 类型：interview 面试通知 */
    private String type = "interview";

    /** 状态：pending 待处理，accepted 已接受，declined 已拒绝 */
    private String status = "pending";

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
