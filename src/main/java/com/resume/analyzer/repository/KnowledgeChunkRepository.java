package com.resume.analyzer.repository;

import com.resume.analyzer.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findByUserIdOrderByKnowledgeDocIdAscSortOrderAsc(Long userId);

    long countByUserId(Long userId);

    @Query("SELECT c.id FROM KnowledgeChunk c WHERE c.knowledgeDocId = :docId")
    List<Long> findIdsByKnowledgeDocId(@Param("docId") Long docId);

    void deleteByKnowledgeDocId(Long knowledgeDocId);

    long countByKnowledgeDocId(Long knowledgeDocId);
}
