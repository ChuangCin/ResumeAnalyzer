package com.resume.analyzer.dto;

import lombok.Data;

@Data
public class SendMessageDTO {
    /** 接收者用户 ID */
    private Long receiverId;
    /** 标题 */
    private String title;
    /** 内容 */
    private String content;
}
