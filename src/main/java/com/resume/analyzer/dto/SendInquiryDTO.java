package com.resume.analyzer.dto;

import lombok.Data;

@Data
public class SendInquiryDTO {
    /** 咨询标题（如：关于 XX 岗位的咨询） */
    private String title;
    /** 咨询内容 */
    private String content;
    /** 关联岗位 ID（选填，便于管理员识别） */
    private Long jobId;
}
