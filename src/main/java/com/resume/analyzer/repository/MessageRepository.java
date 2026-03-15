package com.resume.analyzer.repository;

import com.resume.analyzer.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByReceiverIdOrderByCreateTimeDesc(Long receiverId);

    List<Message> findBySenderIdOrderByCreateTimeDesc(Long senderId);

    List<Message> findByReceiverIdAndTypeOrderByCreateTimeDesc(Long receiverId, String type);

    List<Message> findBySenderIdAndTypeOrderByCreateTimeDesc(Long senderId, String type);

    long countByReceiverId(Long receiverId);

    long countByReceiverIdAndTypeAndStatus(Long receiverId, String type, String status);
}
