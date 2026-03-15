package com.resume.analyzer.repository;

import com.resume.analyzer.entity.KnowledgeDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeRepository extends JpaRepository<KnowledgeDoc, Long> {

    List<KnowledgeDoc> findByUserIdOrderByUploadTimeDesc(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    List<KnowledgeDoc> findByUserIdAndStatusOrderByUploadTimeAsc(Long userId, String status);
}
