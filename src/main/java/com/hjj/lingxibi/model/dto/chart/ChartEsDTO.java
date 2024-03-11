package com.hjj.lingxibi.model.dto.chart;

import com.hjj.lingxibi.model.entity.Chart;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Document(indexName = "chart")
@Data
public class ChartEsDTO {
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * id
     */
    @Id
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
     * 图表类型
     */
    private String chartType;

    /**
     * 图表原始数据
     */
    private String chartData;

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 分析结果
     */
    private String genResult;

    /**
     * 图表状态
     */
    private String status;

    /**
     * 创建的userId
     */
    private Long userId;

    /**
     * 创建时间
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN) // 指定日期格式
    private Date createTime;

    /**
     * 更新时间
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转包装类
     *
     * @param chart
     * @return
     */
    public static ChartEsDTO objToDto(Chart chart) {
        if (chart == null) {
            return null;
        }
        ChartEsDTO chartEsDTO = new ChartEsDTO();
        BeanUtils.copyProperties(chart, chartEsDTO);
        return chartEsDTO;
    }

    /**
     * 包装类转对象
     *
     * @param chartEsDTO
     * @return
     */
    public static Chart dtoToObj(ChartEsDTO chartEsDTO) {
        if (chartEsDTO == null) {
            return null;
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEsDTO, chart);
        return chart;
    }

}
