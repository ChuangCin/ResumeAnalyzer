package com.resume.analyzer.repository;

import com.resume.analyzer.entity.Resume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserIdOrderByUploadTimeDesc(Long userId);

    Page<Resume> findByUserIdOrderByUploadTimeDesc(Long userId, Pageable pageable);
}