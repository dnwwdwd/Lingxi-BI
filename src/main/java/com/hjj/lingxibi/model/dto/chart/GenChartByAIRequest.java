package com.hjj.lingxibi.model.dto.chart;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel(description = "描述信息")
@Data
public class GenChartByAIRequest implements Serializable{
    @ApiModelProperty(value = "名称", required = true)
    private String name;

    @ApiModelProperty(value = "目标", required = true)
    private String goal;

    @ApiModelProperty(value = "图表类型", required = true)
    private String chartType;
}
