package com.hjj.lingxibi.controller;

import com.hjj.lingxibi.common.BaseResponse;
import com.hjj.lingxibi.common.ResultUtils;
import com.hjj.lingxibi.model.vo.MessageVO;
import com.hjj.lingxibi.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Resource
    private MessageService messageService;

    @GetMapping("/list/my")
    public BaseResponse<List<MessageVO>> listMyMessage(HttpServletRequest request) {
    List<MessageVO> messageVOS = messageService.listMyMessage(request);
    return ResultUtils.success(messageVOS);
    }

    @PostMapping("/read/all")
    public BaseResponse<Boolean> readAllMessage(HttpServletRequest request) {
        boolean b = messageService.readAllMessage(request);
        return ResultUtils.success(b);
    }

}
