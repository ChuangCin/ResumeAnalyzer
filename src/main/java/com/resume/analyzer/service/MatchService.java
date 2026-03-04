package com.resume.analyzer.service;

import com.resume.analyzer.entity.Job;
import com.resume.analyzer.entity.MatchResult;
import com.resume.analyzer.entity.Resume;
import com.resume.analyzer.repository.JobRepository;
import com.resume.analyzer.repository.MatchResultRepository;
import com.resume.analyzer.repository.ResumeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchService {

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final MatchResultRepository matchRepository;
    private final AIService aiService;

    public MatchService(
            ResumeRepository resumeRepository,
            JobRepository jobRepository,
            MatchResultRepository matchRepository,
            AIService aiService
    ) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.matchRepository = matchRepository;
        this.aiService = aiService;
    }

    public MatchResult match(Long resumeId, Long jobId) {

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("简历不存在"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("岗位不存在"));

        String report = aiService.matchJob(
                resume.getContent(),
                job.getDescription()
        );

        MatchResult match = new MatchResult();
        match.setResumeId(resumeId);
        match.setJobId(jobId);
        match.setReport(report);
        match.setCreateTime(LocalDateTime.now());

        return matchRepository.save(match);
    }

    public List<MatchResult> getByResumeId(Long resumeId) {
        return matchRepository.findByResumeIdOrderByCreateTimeDesc(resumeId);
    }

    /**
     * 对指定简历与所有岗位执行匹配，并保存结果。
     */
    public List<MatchResult> runMatchForResume(Long resumeId) {
        List<Job> jobs = jobRepository.findAll();
        List<MatchResult> results = new ArrayList<>();
        for (Job job : jobs) {
            try {
                MatchResult mr = match(resumeId, job.getId());
                results.add(mr);
            } catch (Exception e) {
                e.printStackTrace();
                // 单个岗位失败不影响其余
            }
        }
        return results;
    }
}