package com.resume.analyzer.service;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.dto.AnalysisListItemDTO;
import com.resume.analyzer.dto.UpdateProfileDTO;
import com.resume.analyzer.entity.AnalysisResult;
import com.resume.analyzer.entity.Job;
import com.resume.analyzer.entity.Resume;
import com.resume.analyzer.entity.User;
import com.resume.analyzer.repository.AnalysisRepository;
import com.resume.analyzer.repository.JobRepository;
import com.resume.analyzer.repository.ResumeRepository;
import com.resume.analyzer.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final AnalysisRepository analysisRepository;
    private final ResumeRepository resumeRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminService(UserRepository userRepository, JobRepository jobRepository,
                        AnalysisRepository analysisRepository, ResumeRepository resumeRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.analysisRepository = analysisRepository;
        this.resumeRepository = resumeRepository;
    }

    public Result<List<User>> listUsers() {
        List<User> list = userRepository.findAll().stream()
                .peek(u -> u.setPassword(null))
                .collect(Collectors.toList());
        return Result.success(list);
    }

    public Result<User> updateUser(Long id, UpdateProfileDTO dto) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return Result.error("用户不存在");
        if (dto.getUsername() != null)
            user.setUsername(dto.getUsername().trim().isBlank() ? null : dto.getUsername().trim());
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            String phone = dto.getPhone().trim();
            User byPhone = userRepository.findByPhone(phone);
            if (byPhone != null && !byPhone.getId().equals(id))
                return Result.error("该手机号已被其他用户使用");
            user.setPhone(phone);
        }
        if (dto.getEmail() != null) {
            String email = dto.getEmail().trim();
            if (!email.isEmpty()) {
                User byEmail = userRepository.findByEmail(email);
                if (byEmail != null && !byEmail.getId().equals(id))
                    return Result.error("该邮箱已被其他用户使用");
                user.setEmail(email);
            }
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank())
            user.setPassword(encoder.encode(dto.getPassword()));
        userRepository.save(user);
        user.setPassword(null);
        return Result.success(user);
    }

    public Result<Void> deleteUser(Long id) {
        if (!userRepository.existsById(id)) return Result.error("用户不存在");
        userRepository.deleteById(id);
        return Result.success(null);
    }

    public Result<List<Job>> listJobs() {
        return Result.success(jobRepository.findAll());
    }

    public Result<Job> createJob(Job job) {
        if (job.getCreateTime() == null)
            job.setCreateTime(java.time.LocalDateTime.now());
        job.setId(null);
        return Result.success(jobRepository.save(job));
    }

    public Result<Job> updateJob(Long id, Job job) {
        Job existing = jobRepository.findById(id).orElse(null);
        if (existing == null) return Result.error("岗位不存在");
        if (job.getJobName() != null) existing.setJobName(job.getJobName());
        if (job.getJobCategory() != null) existing.setJobCategory(job.getJobCategory());
        if (job.getCompany() != null) existing.setCompany(job.getCompany());
        if (job.getCity() != null) existing.setCity(job.getCity());
        if (job.getSalaryMin() != null) existing.setSalaryMin(job.getSalaryMin());
        if (job.getSalaryMax() != null) existing.setSalaryMax(job.getSalaryMax());
        if (job.getEducation() != null) existing.setEducation(job.getEducation());
        if (job.getExperience() != null) existing.setExperience(job.getExperience());
        if (job.getSkills() != null) existing.setSkills(job.getSkills());
        if (job.getDescription() != null) existing.setDescription(job.getDescription());
        if (job.getRequirement() != null) existing.setRequirement(job.getRequirement());
        return Result.success(jobRepository.save(existing));
    }

    public Result<Void> deleteJob(Long id) {
        if (!jobRepository.existsById(id)) return Result.error("岗位不存在");
        jobRepository.deleteById(id);
        return Result.success(null);
    }

    public Result<List<AnalysisListItemDTO>> listAnalysis() {
        List<AnalysisResult> all = analysisRepository.findAll();
        List<AnalysisListItemDTO> list = new ArrayList<>();
        for (AnalysisResult ar : all) {
            AnalysisListItemDTO dto = new AnalysisListItemDTO();
            dto.setId(ar.getId());
            dto.setResumeId(ar.getResumeId());
            dto.setScore(ar.getScore());
            dto.setSummary(ar.getSummary());
            dto.setCreateTime(ar.getCreateTime());
            if (ar.getResumeId() != null) {
                Resume r = resumeRepository.findById(ar.getResumeId()).orElse(null);
                if (r != null && r.getUserId() != null) {
                    dto.setUserId(r.getUserId());
                    User u = userRepository.findById(r.getUserId()).orElse(null);
                    if (u != null) {
                        dto.setUsername(u.getUsername() != null && !u.getUsername().isBlank() ? u.getUsername() : (u.getPhone() != null ? u.getPhone() : u.getEmail()));
                    }
                }
            }
            list.add(dto);
        }
        return Result.success(list);
    }

    public Result<Map<String, Object>> getAnalysisDetailForAdmin(Long analysisId) {
        AnalysisResult ar = analysisRepository.findById(analysisId).orElse(null);
        if (ar == null) return Result.error("分析记录不存在");
        Map<String, Object> map = new HashMap<>();
        map.put("id", ar.getId());
        map.put("resumeId", ar.getResumeId());
        map.put("summary", ar.getSummary());
        map.put("highlights", ar.getHighlights());
        map.put("score", ar.getScore());
        map.put("createTime", ar.getCreateTime());
        map.put("skillScore", ar.getSkillScore());
        map.put("experienceScore", ar.getExperienceScore());
        map.put("structureScore", ar.getStructureScore());
        map.put("expressionScore", ar.getExpressionScore());
        map.put("completenessScore", ar.getCompletenessScore());
        try {
            if (ar.getSuggestions() != null && !ar.getSuggestions().isBlank()) {
                map.put("suggestions", new ObjectMapper().readTree(ar.getSuggestions()));
            } else {
                map.put("suggestions", null);
            }
        } catch (Exception e) {
            map.put("suggestions", null);
        }
        if (ar.getResumeId() != null) {
            Resume r = resumeRepository.findById(ar.getResumeId()).orElse(null);
            if (r != null && r.getUserId() != null) {
                map.put("userId", r.getUserId());
                User u = userRepository.findById(r.getUserId()).orElse(null);
                if (u != null) {
                    map.put("username", u.getUsername() != null && !u.getUsername().isBlank() ? u.getUsername() : (u.getPhone() != null ? u.getPhone() : u.getEmail()));
                }
            }
        }
        return Result.success(map);
    }

    public Result<Map<String, Object>> getStats() {
        Map<String, Object> map = new HashMap<>();
        map.put("userCount", userRepository.count());
        map.put("jobCount", jobRepository.count());
        map.put("resumeCount", resumeRepository.count());
        map.put("analysisCount", analysisRepository.count());
        return Result.success(map);
    }

    public Result<String> maintenance() {
        return Result.success("系统维护功能已执行（当前为占位实现）");
    }
}
