package com.resume.analyzer.dto;

import lombok.Data;

@Data
public class UpdateProfileDTO {
    private String username; // 昵称
    private String phone;    // 手机号（账号）
    private String email;
    private String password; // 可选，不传则不修改
}
