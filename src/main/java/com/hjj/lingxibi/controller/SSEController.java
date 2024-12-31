package com.hjj.lingxibi.controller;

import com.hjj.lingxibi.manager.SSEManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;

@RestController
@RequestMapping("/sse")
public class SSEController {

    @Resource
    private SSEManager sseManager;

    @GetMapping("/user/connect")
    public SseEmitter userSSEConnect(@RequestParam("userId") Long userId) {
        return sseManager.createChartSSEConnection(userId);
    }

    @GetMapping("/team/connect")
    public SseEmitter teamSSEConnect(@RequestParam("teamId") Long teamId) {
       return sseManager.createTeamChartSSEConnection(teamId);
    }

}
