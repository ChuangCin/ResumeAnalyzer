package com.resume.analyzer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RegisterDTO {
    /** 手机号（账号） */
    private String phone;
    private String email;
    private String password;
    /** 昵称/显示名，选填 */
    private String username;
    /** 机构授权码，填写正确则注册为管理员 */
    @JsonProperty("adminCode")
    private String adminCode;
}