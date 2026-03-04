package com.resume.analyzer.controller;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.dto.LoginDTO;
import com.resume.analyzer.dto.RegisterDTO;
import com.resume.analyzer.dto.UpdateProfileDTO;
import com.resume.analyzer.entity.User;
import com.resume.analyzer.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 注册
    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterDTO dto) {
        return userService.register(dto);
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PutMapping("/{id}")
    public Result<User> updateProfile(@PathVariable Long id, @RequestBody UpdateProfileDTO dto) {
        return userService.updateProfile(id, dto);
    }

    @PostMapping("/{id}/avatar")
    public Result<User> uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file, @RequestParam("userId") Long userId) {
        if (!id.equals(userId)) return Result.error("无权操作");
        return userService.uploadAvatar(id, file);
    }
}