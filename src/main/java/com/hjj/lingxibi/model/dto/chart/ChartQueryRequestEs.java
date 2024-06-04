package com.hjj.lingxibi.model.dto.chart;

import com.hjj.lingxibi.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 图表 ES 搜索请求
 */
@Data
public class ChartQueryRequestEs extends PageRequest implements Serializable {

    // 搜索关键词
    private String name;

    private static final long serialVersionUID = 1L;
}
