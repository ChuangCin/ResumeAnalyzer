package com.resume.analyzer.repository;

import com.resume.analyzer.entity.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    List<MatchResult> findByResumeIdOrderByCreateTimeDesc(Long resumeId);
}