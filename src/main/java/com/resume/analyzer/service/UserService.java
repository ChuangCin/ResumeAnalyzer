package com.resume.analyzer.service;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.dto.LoginDTO;
import com.resume.analyzer.dto.RegisterDTO;
import com.resume.analyzer.dto.UpdateProfileDTO;
import com.resume.analyzer.entity.User;
import com.resume.analyzer.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private static final String ADMIN_REGISTER_CODE = "ADMIN2026";

    // 注册：使用手机号、邮箱、密码；账号为手机号或邮箱
    public Result<User> register(RegisterDTO dto) {
        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            return Result.error("请输入手机号（账号）");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            return Result.error("请输入邮箱");
        }
        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            return Result.error("请输入密码");
        }
        String phone = dto.getPhone().trim();
        String email = dto.getEmail().trim();
        if (userRepository.findByPhone(phone) != null) {
            return Result.error("该手机号已注册");
        }
        if (userRepository.findByEmail(email) != null) {
            return Result.error("该邮箱已注册");
        }

        User user = new User();
        user.setPhone(phone);
        user.setEmail(email);
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setUsername(dto.getUsername() != null && !dto.getUsername().isBlank() ? dto.getUsername().trim() : phone);
        String code = dto.getAdminCode() != null ? dto.getAdminCode().trim() : null;
        if (ADMIN_REGISTER_CODE.equalsIgnoreCase(code)) {
            user.setRole("admin");
        } else {
            user.setRole("user");
        }

        userRepository.save(user);
        user.setPassword(null);
        return Result.success(user);
    }

    // 登录：账号为邮箱或手机号 + 密码
    public Result<User> login(LoginDTO dto) {
        if (dto.getAccount() == null || dto.getAccount().trim().isEmpty()) {
            return Result.error("请输入邮箱或手机号");
        }
        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            return Result.error("请输入密码");
        }
        String account = dto.getAccount().trim();
        User user = userRepository.findByPhone(account);
        if (user == null) {
            user = userRepository.findByEmail(account);
        }
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!encoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.error("密码错误");
        }
        String actualRole = user.getRole() != null && user.getRole().equalsIgnoreCase("admin") ? "admin" : "user";
        String requestRole = dto.getRole() != null ? dto.getRole().trim().toLowerCase() : "user";
        if ("admin".equals(requestRole)) {
            if (!"admin".equals(actualRole)) {
                return Result.error("该账号为普通用户，请使用普通用户入口登录");
            }
        } else {
            if ("admin".equals(actualRole)) {
                return Result.error("该账号为管理员，请使用管理员入口登录");
            }
        }
        user.setPassword(null);
        return Result.success(user);
    }

    public Result<User> getById(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    public Result<User> updateProfile(Long id, UpdateProfileDTO dto) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername().trim().isBlank() ? null : dto.getUsername().trim());
        }
        if (dto.getPhone() != null) {
            String phone = dto.getPhone().trim();
            if (!phone.isEmpty()) {
                User byPhone = userRepository.findByPhone(phone);
                if (byPhone != null && !byPhone.getId().equals(id)) {
                    return Result.error("该手机号已被其他用户使用");
                }
                user.setPhone(phone);
            }
        }
        if (dto.getEmail() != null) {
            String email = dto.getEmail().trim();
            if (!email.isEmpty()) {
                User byEmail = userRepository.findByEmail(email);
                if (byEmail != null && !byEmail.getId().equals(id)) {
                    return Result.error("该邮箱已被其他用户使用");
                }
                user.setEmail(email);
            }
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(encoder.encode(dto.getPassword()));
        }
        userRepository.save(user);
        user.setPassword(null);
        return Result.success(user);
    }

    /** 上传头像：仅允许本人上传，支持 jpg/png/gif/webp */
    public Result<User> uploadAvatar(Long userId, MultipartFile file) {
        if (userId == null) return Result.error("请先登录");
        if (file == null || file.isEmpty()) return Result.error("请选择头像文件");
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return Result.error("用户不存在");
        String original = file.getOriginalFilename();
        if (original == null || original.isEmpty()) return Result.error("无效文件名");
        String ext = "";
        int i = original.lastIndexOf('.');
        if (i > 0) ext = original.substring(i).toLowerCase();
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)")) return Result.error("仅支持 JPG、PNG、GIF、WEBP 格式");
        String dir = System.getProperty("user.dir") + "/uploads/avatars/";
        File dirFile = new File(dir);
        if (!dirFile.exists()) dirFile.mkdirs();
        String filename = userId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ext;
        String path = dir + filename;
        try {
            file.transferTo(new File(path));
        } catch (IOException e) {
            return Result.error("保存文件失败");
        }
        user.setAvatar(filename);
        userRepository.save(user);
        user.setPassword(null);
        return Result.success(user);
    }
}