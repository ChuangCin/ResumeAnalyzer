package com.resume.analyzer.controller;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.dto.SendInquiryDTO;
import com.resume.analyzer.entity.Message;
import com.resume.analyzer.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /** 用户：我收到的消息列表（面试通知 + 咨询回复） */
    @GetMapping("/list")
    public Result<List<Message>> listReceived(@RequestParam Long userId) {
        return messageService.listReceivedByUser(userId);
    }

    /** 用户：未读消息数（Redis 缓存 user:message:unread:{userId}） */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestParam Long userId) {
        return messageService.getUnreadCount(userId);
    }

    /** 用户：标记已读（进入消息页时调用，清零未读缓存） */
    @PostMapping("/read")
    public Result<Void> markRead(@RequestParam Long userId) {
        return messageService.markMessagesRead(userId);
    }

    /** 用户：我发出的咨询列表 */
    @GetMapping("/my-inquiries")
    public Result<List<Message>> listMyInquiries(@RequestParam Long userId) {
        return messageService.listMyInquiries(userId);
    }

    /** 用户：发起咨询 */
    @PostMapping("/inquiry")
    public Result<Message> sendInquiry(@RequestParam Long userId, @RequestBody SendInquiryDTO dto) {
        return messageService.userSendInquiry(userId, dto);
    }

    /** 用户：接受面试邀请 */
    @PutMapping("/{id}/accept")
    public Result<Message> accept(@PathVariable Long id, @RequestParam Long userId) {
        return messageService.acceptInvitation(id, userId);
    }

    /** 用户：拒绝面试邀请 */
    @PutMapping("/{id}/decline")
    public Result<Message> decline(@PathVariable Long id, @RequestParam Long userId) {
        return messageService.declineInvitation(id, userId);
    }
}
