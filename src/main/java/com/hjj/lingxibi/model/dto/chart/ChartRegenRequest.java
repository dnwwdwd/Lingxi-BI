package com.hjj.lingxibi.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChartRegenRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 图标名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 队伍id
     */
    private Long teamId;

    private static final long serialVersionUID = 1L;
}
