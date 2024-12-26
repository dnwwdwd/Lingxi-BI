package com.hjj.lingxibi.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TeamVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍图片
     */
    private String imgUrl;

    /**
     * 队长id
     */
    private Long userId;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 创建时间
     */
    private Date createTime;

    private UserVO userVO;

    private boolean inTeam;
}
