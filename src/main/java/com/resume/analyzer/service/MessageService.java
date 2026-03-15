package com.resume.analyzer.service;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.dto.SendInquiryDTO;
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
    private final CacheService cacheService;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository, CacheService cacheService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.cacheService = cacheService;
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
        cacheService.incrementUnreadCount(dto.getReceiverId());
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

    /** 用户：发起咨询（发给管理员） */
    public Result<Message> userSendInquiry(Long userId, SendInquiryDTO dto) {
        if (userId == null) return Result.error("请先登录");
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return Result.error("用户不存在");
        List<User> admins = userRepository.findByRole("admin");
        if (admins == null || admins.isEmpty()) return Result.error("暂无可咨询的管理员");
        User admin = admins.get(0);
        if (admin.getId().equals(userId)) return Result.error("不能给自己发咨询");
        String title = dto.getTitle() != null ? dto.getTitle().trim() : "岗位咨询";
        if (title.isEmpty()) title = "岗位咨询";
        String content = dto.getContent() != null ? dto.getContent().trim() : "";
        if (dto.getJobId() != null) {
            content = "[岗位ID: " + dto.getJobId() + "]\n" + content;
        }
        Message msg = new Message();
        msg.setSenderId(userId);
        msg.setReceiverId(admin.getId());
        msg.setTitle(title);
        msg.setContent(content);
        msg.setType("inquiry");
        msg.setStatus("pending");
        msg.setCreateTime(LocalDateTime.now());
        messageRepository.save(msg);
        return Result.success(msg);
    }

    /** 用户：我发出的咨询列表 */
    public Result<List<Message>> listMyInquiries(Long userId) {
        if (userId == null) return Result.error("请先登录");
        List<Message> list = messageRepository.findBySenderIdAndTypeOrderByCreateTimeDesc(userId, "inquiry");
        return Result.success(list);
    }

    /** 管理员：收到的用户咨询列表 */
    public Result<List<Message>> listInquiriesForAdmin(Long adminId) {
        if (adminId == null) return Result.error("请先登录");
        List<Message> list = messageRepository.findByReceiverIdAndTypeOrderByCreateTimeDesc(adminId, "inquiry");
        return Result.success(list);
    }

    /** 管理员：回复用户咨询 */
    public Result<Message> adminReplyToInquiry(Long adminId, Long inquiryMessageId, String content) {
        if (adminId == null) return Result.error("请先登录");
        Message inquiry = messageRepository.findById(inquiryMessageId).orElse(null);
        if (inquiry == null) return Result.error("咨询记录不存在");
        if (!"inquiry".equalsIgnoreCase(inquiry.getType())) return Result.error("该消息不是咨询");
        if (!inquiry.getReceiverId().equals(adminId)) return Result.error("无权回复该咨询");
        String replyContent = content != null ? content.trim() : "";
        Message reply = new Message();
        reply.setSenderId(adminId);
        reply.setReceiverId(inquiry.getSenderId());
        reply.setTitle("回复： " + (inquiry.getTitle() != null ? inquiry.getTitle() : "您的咨询"));
        reply.setContent(replyContent);
        reply.setType("inquiry_reply");
        reply.setStatus("accepted");
        reply.setCreateTime(LocalDateTime.now());
        messageRepository.save(reply);
        inquiry.setStatus("replied");
        messageRepository.save(inquiry);
        cacheService.incrementUnreadCount(inquiry.getSenderId());
        return Result.success(reply);
    }

    /** 未读消息数（Redis 缓存 key: user:message:unread:{userId}） */
    public Result<Long> getUnreadCount(Long userId) {
        if (userId == null) return Result.error("请先登录");
        Long cached = cacheService.getUnreadCount(userId);
        if (cached != null) return Result.success(cached);
        long count = messageRepository.countByReceiverId(userId);
        cacheService.setUnreadCount(userId, count);
        return Result.success(count);
    }

    /** 用户查看消息后清零未读（进入消息页时调用） */
    public Result<Void> markMessagesRead(Long userId) {
        if (userId == null) return Result.error("请先登录");
        cacheService.setUnreadCount(userId, 0L);
        return Result.success(null);
    }

    /** 管理员：未回复的咨询数量（用于小红点） */
    public Result<Long> getAdminInquiryUnreadCount(Long adminId) {
        if (adminId == null) return Result.success(0L);
        long count = messageRepository.countByReceiverIdAndTypeAndStatus(adminId, "inquiry", "pending");
        return Result.success(count);
    }
}
