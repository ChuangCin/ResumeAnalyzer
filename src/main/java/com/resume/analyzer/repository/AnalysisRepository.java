package com.resume.analyzer.repository;

import com.resume.analyzer.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<AnalysisResult, Long> {

    // 查询某个简历的所有分析记录
    List<AnalysisResult> findByResumeId(Long resumeId);

    // 查询最新的一条分析记录（PDF用）
    AnalysisResult findTopByResumeIdOrderByCreateTimeDesc(Long resumeId);
}