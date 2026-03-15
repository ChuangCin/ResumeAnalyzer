package com.resume.analyzer.controller;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.dto.AnalysisListItemDTO;
import com.resume.analyzer.dto.SendMessageDTO;
import com.resume.analyzer.dto.UpdateProfileDTO;
import com.resume.analyzer.entity.Job;
import com.resume.analyzer.entity.Message;
import com.resume.analyzer.entity.User;
import com.resume.analyzer.service.AdminService;
import com.resume.analyzer.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final MessageService messageService;

    public AdminController(AdminService adminService, MessageService messageService) {
        this.adminService = adminService;
        this.messageService = messageService;
    }

    // ---------- 用户管理 ----------
    @GetMapping("/users")
    public Result<List<User>> listUsers() {
        return adminService.listUsers();
    }

    /** 分页查询用户（MyBatis-Plus） */
    @GetMapping("/users/page")
    public Result<Map<String, Object>> pageUsers(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return adminService.pageUsers(page, size);
    }

    @PutMapping("/users/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody UpdateProfileDTO dto) {
        return adminService.updateUser(id, dto);
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        return adminService.deleteUser(id);
    }

    // ---------- 岗位管理 ----------
    @GetMapping("/jobs")
    public Result<List<Job>> listJobs() {
        return adminService.listJobs();
    }

    /** 分页查询岗位（MyBatis-Plus） */
    @GetMapping("/jobs/page")
    public Result<Map<String, Object>> pageJobs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return adminService.pageJobs(page, size);
    }

    @PostMapping("/jobs")
    public Result<Job> createJob(@RequestBody Job job) {
        return adminService.createJob(job);
    }

    @PutMapping("/jobs/{id}")
    public Result<Job> updateJob(@PathVariable Long id, @RequestBody Job job) {
        return adminService.updateJob(id, job);
    }

    @DeleteMapping("/jobs/{id}")
    public Result<Void> deleteJob(@PathVariable Long id) {
        return adminService.deleteJob(id);
    }

    // ---------- 简历分析结果 ----------
    @GetMapping("/analysis")
    public Result<List<AnalysisListItemDTO>> listAnalysis() {
        return adminService.listAnalysis();
    }

    /** 分页查询简历分析结果 */
    @GetMapping("/analysis/page")
    public Result<Map<String, Object>> pageAnalysis(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return adminService.pageAnalysis(page, size);
    }

    @GetMapping("/analysis/{id}")
    public Result<java.util.Map<String, Object>> getAnalysisDetail(@PathVariable Long id) {
        return adminService.getAnalysisDetailForAdmin(id);
    }

    // ---------- 系统统计 ----------
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return adminService.getStats();
    }

    // ---------- 系统维护 ----------
    @PostMapping("/maintenance")
    public Result<String> maintenance() {
        return adminService.maintenance();
    }

    // ---------- 消息/面试通知 ----------
    /** 管理员发送面试通知 */
    @PostMapping("/messages")
    public Result<Message> sendMessage(@RequestParam Long senderId, @RequestBody SendMessageDTO dto) {
        return messageService.sendFromAdmin(senderId, dto);
    }

    /** 管理员查看已发送的通知列表 */
    @GetMapping("/messages")
    public Result<List<Message>> listSentMessages(@RequestParam Long senderId) {
        return messageService.listSentByAdmin(senderId);
    }

    /** 管理员：收到的用户咨询列表 */
    @GetMapping("/messages/inquiries")
    public Result<List<Message>> listInquiries(@RequestParam Long adminId) {
        return messageService.listInquiriesForAdmin(adminId);
    }

    /** 管理员：未回复咨询数量（小红点） */
    @GetMapping("/messages/inquiries/unread-count")
    public Result<Long> getInquiryUnreadCount(@RequestParam Long adminId) {
        return messageService.getAdminInquiryUnreadCount(adminId);
    }

    /** 管理员：回复用户咨询 */
    @PostMapping("/messages/inquiry/{id}/reply")
    public Result<Message> replyToInquiry(@PathVariable Long id, @RequestParam Long adminId, @RequestBody Map<String, String> body) {
        String content = body != null && body.containsKey("content") ? body.get("content") : "";
        return messageService.adminReplyToInquiry(adminId, id, content);
    }
}
