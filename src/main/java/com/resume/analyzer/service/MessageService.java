package com.resume.analyzer.service;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.dto.SendMessageDTO;
import com.resume.analyzer.entity.Message;
import com.resume.analyzer.entity.User;
import com.resume.analyzer.repository.MessageRepository;
import com.resume.analyzer.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    /** 用户：获取我收到的消息列表 */
    public Result<List<Message>> listReceivedByUser(Long userId) {
        if (userId == null) {
            return Result.error("请先登录");
        }
        List<Message> list = messageRepository.findByReceiverIdOrderByCreateTimeDesc(userId);
        return Result.success(list);
    }

    /** 用户：接受面试邀请 */
    public Result<Message> acceptInvitation(Long messageId, Long userId) {
        if (userId == null) {
            return Result.error("请先登录");
        }
        Message msg = messageRepository.findById(messageId).orElse(null);
        if (msg == null) {
            return Result.error("消息不存在");
        }
        if (!msg.getReceiverId().equals(userId)) {
            return Result.error("无权操作该消息");
        }
        if ("accepted".equalsIgnoreCase(msg.getStatus())) {
            return Result.success(msg);
        }
        msg.setStatus("accepted");
        messageRepository.save(msg);
        return Result.success(msg);
    }

    /** 用户：拒绝面试邀请（可选） */
    public Result<Message> declineInvitation(Long messageId, Long userId) {
        if (userId == null) {
            return Result.error("请先登录");
        }
        Message msg = messageRepository.findById(messageId).orElse(null);
        if (msg == null) {
            return Result.error("消息不存在");
        }
        if (!msg.getReceiverId().equals(userId)) {
            return Result.error("无权操作该消息");
        }
        msg.setStatus("declined");
        messageRepository.save(msg);
        return Result.success(msg);
    }

    /** 管理员：向用户发送面试通知 */
    public Result<Message> sendFromAdmin(Long adminId, SendMessageDTO dto) {
        if (adminId == null) {
            return Result.error("请先登录");
        }
        User admin = userRepository.findById(adminId).orElse(null);
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            return Result.error("仅管理员可发送通知");
        }
        if (dto.getReceiverId() == null) {
            return Result.error("请选择接收用户");
        }
        User receiver = userRepository.findById(dto.getReceiverId()).orElse(null);
        if (receiver == null) {
            return Result.error("接收用户不存在");
        }
        if (receiver.getId().equals(adminId)) {
            return Result.error("不能给自己发消息");
        }
        String title = dto.getTitle() != null ? dto.getTitle().trim() : "";
        String content = dto.getContent() != null ? dto.getContent().trim() : "";
        if (title.isEmpty()) {
            return Result.error("请输入通知标题");
        }

        Message msg = new Message();
        msg.setSenderId(adminId);
        msg.setReceiverId(dto.getReceiverId());
        msg.setTitle(title);
        msg.setContent(content);
        msg.setType("interview");
        msg.setStatus("pending");
        msg.setCreateTime(LocalDateTime.now());
        messageRepository.save(msg);
        return Result.success(msg);
    }

    /** 管理员：获取我发出的消息列表 */
    public Result<List<Message>> listSentByAdmin(Long adminId) {
        if (adminId == null) {
            return Result.error("请先登录");
        }
        List<Message> list = messageRepository.findBySenderIdOrderByCreateTimeDesc(adminId);
        return Result.success(list);
    }
}
