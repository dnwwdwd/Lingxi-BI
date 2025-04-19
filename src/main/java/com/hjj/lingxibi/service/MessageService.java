package com.hjj.lingxibi.service;

import com.hjj.lingxibi.model.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hjj.lingxibi.model.entity.User;
import com.hjj.lingxibi.model.vo.MessageVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author hejiajun
* @description 针对表【message(消息表)】的数据库操作Service
* @createDate 2025-04-17 19:36:36
*/
public interface MessageService extends IService<Message> {
    boolean sendMessage(Long oldChartId, User loginUser);

    List<MessageVO> listMyMessage(HttpServletRequest request);

    boolean readAllMessage(HttpServletRequest request);
}
