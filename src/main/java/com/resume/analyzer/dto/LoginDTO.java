package com.resume.analyzer.dto;

import lombok.Data;

@Data
public class LoginDTO {
    /** 邮箱或手机号（账号） */
    private String account;
    private String password;
    /** 登录身份：user 普通用户入口，admin 管理员入口；与账号实际角色必须一致 */
    private String role;
}