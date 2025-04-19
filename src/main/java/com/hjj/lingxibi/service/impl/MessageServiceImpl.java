package com.hjj.lingxibi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.model.entity.ChartHistory;
import com.hjj.lingxibi.model.entity.Message;
import com.hjj.lingxibi.model.entity.User;
import com.hjj.lingxibi.model.vo.MessageVO;
import com.hjj.lingxibi.service.ChartHistoryService;
import com.hjj.lingxibi.service.ChartService;
import com.hjj.lingxibi.service.MessageService;
import com.hjj.lingxibi.mapper.MessageMapper;
import com.hjj.lingxibi.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author hejiajun
 * @description 针对表【message(消息表)】的数据库操作Service实现
 * @createDate 2025-04-17 19:36:36
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
        implements MessageService {

    @Resource
    @Lazy
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private ChartHistoryService chartHistoryService;

    @Override
    public boolean sendMessage(Long oldChartId, User loginUser) {
        // 查询出还没更新的图表
        Chart chart = chartService.getById(oldChartId);
        ChartHistory chartHistory = new ChartHistory();
        BeanUtils.copyProperties(chart, chartHistory);
        chartHistory.setRelatedChartId(oldChartId);
        chartHistory.setId(null);
        boolean b1 = chartHistoryService.save(chartHistory);
        Message message = new Message();
        message.setFromId(loginUser.getId());
        message.setToId(chart.getUserId());
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String time = now.format(formatter);
        if (loginUser.getId().equals(chart.getUserId())) {
            message.setContent("您在" + time + "更新了自己的图表" + chart.getName());
        } else {
            message.setContent(loginUser.getUserName() + "在" + time + "更改了您的图表" + chart.getName());
        }
        message.setChartHistoryId(chartHistory.getId());
        boolean b2 = this.save(message);
        return b1 && b2;
    }

    @Override
    public List<MessageVO> listMyMessage(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        List<Message> messages = this.lambdaQuery().eq(Message::getToId, userId).orderByDesc(Message::getCreateTime).list();
        List<MessageVO> messageVOS = messages.stream().map(message -> {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);
            ChartHistory chartHistory = chartHistoryService.getById(message.getChartHistoryId());
            messageVO.setChartHistory(chartHistory);
            User user = userService.getById(message.getFromId());
            messageVO.setFromUser(userService.getUserVO(user));
            return messageVO;
        }).collect(Collectors.toList());
        return messageVOS;
    }

    @Override
    public boolean readAllMessage(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        List<Message> messages = this.lambdaQuery().eq(Message::getToId, userId).eq(Message::getIsRead, 0).list();
        messages.stream().forEach(message -> {
            message.setIsRead(1);
        });
        return this.updateBatchById(messages);
    }


}




