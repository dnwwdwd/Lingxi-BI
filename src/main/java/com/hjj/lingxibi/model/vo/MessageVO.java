package com.hjj.lingxibi.model.vo;

import com.hjj.lingxibi.model.entity.ChartHistory;
import lombok.Data;

import java.io.Serializable;

@Data
public class MessageVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 关联的图表历史id
     */
    private Long chartHistoryId;

    /**
     * 发送消息的用户id
     */
    private Long fromId;

    /**
     * 接收消息的用户id
     */
    private Long toId;

    /**
     * 是否已（0 - 未读 1 - 已读）
     */
    private Integer isRead;

    private UserVO fromUser;

    private ChartHistory chartHistory;
}
