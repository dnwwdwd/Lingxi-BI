package com.hjj.lingxibi.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ExcelData implements Serializable {
    /**
     * 对应表格的日期列
     */
    private String date;

    /**
     * 对应表格的用户数列
     */
    private Integer userNum;

}
