package com.hjj.lingxibi.model.dto.team;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TeamAddRequest implements Serializable {

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍图片
     */
    private String imgUrl;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    private static final long serialVersionUID = 1L;
}