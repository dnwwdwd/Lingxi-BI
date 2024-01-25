package com.hjj.lingxibi.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 图表信息表
 * @TableName chart
 */
@TableName(value ="chart")
@Data
public class Chart implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分析目标
     */
    @TableField(value = "goal")
    private String goal;

    /**
     * 图表数据
     */
    @TableField(value = "chartData")
    private String chartData;

    /**
     * 图表类型
     */
    @TableField(value = "chartType")
    private String chartType;

    /**
     * AI生成的图表信息
     */
    @TableField(value = "genChart")
    private String genChart;

    /**
     * AI生成的分析结果
     */
    @TableField(value = "genResult")
    private String genResult;

    /**
     * 创建时间
     */
    @TableField(value = "createTime")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updateTime")
    private Date updateTime;

    /**
     * 是否删除（0-不删除 1-删除）
     */
    @TableLogic
    @TableField(value = "isDelete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}