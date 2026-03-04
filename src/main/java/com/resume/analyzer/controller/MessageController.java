package com.resume.analyzer.controller;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.entity.Message;
import com.resume.analyzer.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /** 用户：我收到的消息列表 */
    @GetMapping("/list")
    public Result<List<Message>> listReceived(@RequestParam Long userId) {
        return messageService.listReceivedByUser(userId);
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
