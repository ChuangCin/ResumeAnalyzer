package com.resume.analyzer.controller;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.entity.Job;
import com.resume.analyzer.repository.JobRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 公开岗位接口：供前端展示岗位名称（如消息中 [岗位ID: x] 替换为岗位名）。
 */
@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobRepository jobRepository;

    public JobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /** 岗位简要列表（id、jobName），用于消息等展示 */
    @GetMapping("/summary")
    public Result<List<JobSummary>> summary() {
        List<JobSummary> list = jobRepository.findAll().stream()
                .map(j -> new JobSummary(j.getId(), j.getJobName()))
                .collect(Collectors.toList());
        return Result.success(list);
    }

    public static class JobSummary {
        private Long id;
        private String jobName;

        public JobSummary(Long id, String jobName) {
            this.id = id;
            this.jobName = jobName;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }
    }
}
