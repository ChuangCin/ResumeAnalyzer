package com.resume.analyzer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
public class User {
    @JsonIgnore
    private String password;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username; // 昵称/显示名，非登录账号

    private String phone; // 手机号，作为账号之一

    private String email; // 邮箱，作为账号之一

    private String role = "user"; // user | admin

    /** 头像文件名，存于 uploads/avatars/，访问路径 /avatars/xxx */
    private String avatar;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // getter and setter
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public String getEmail() {
        return email;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role == null || role.isBlank() ? "user" : role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}