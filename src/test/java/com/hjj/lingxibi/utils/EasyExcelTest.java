package com.hjj.lingxibi.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.util.ListUtils;
import com.hjj.lingxibi.model.ExcelData;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EasyExcel 测试
 *
 */
@SpringBootTest
public class EasyExcelTest {

    @Test
    public void doImport() throws FileNotFoundException {
        List<Map<Integer, String>> list = null;
        File file = ResourceUtils.getFile("classpath:网站数据.xlsx");
        try {
            list = EasyExcel.read(file)
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (Exception e) {
            throw new RuntimeException("读取 Excel 文件失败");
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i=0;i<list.size();i++) {
            // 转为 LinkedHashMap 主要是为了保证读取的数据和表格顺序一致
            LinkedHashMap<Integer, String> linkedHashMap = (LinkedHashMap) list.get(i);
            List<String> dataList = linkedHashMap.values().stream()
                    .filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(dataList, ",")).append("\n");
        }
        System.out.println(stringBuilder.toString());
    }

    /**
     * 知道表头，并形成映射关系
     */
    @Test
    public void doImportsForMapping() throws FileNotFoundException{
        // 读取 resource 目录下的 Excel 文件（网站数据.xlsx）
        File file = ResourceUtils.getFile("classpath:网站数据.xlsx");
        // 创建一个 list 存储每行的数据，即 ExcelData 对象
        List<ExcelData> list = new ArrayList<>();
        // 直接使用 EasyExcel 的 read 方法，同时定义表头的类型，以便将列中数据映射为 ExcelData 对象
        EasyExcel.read(file, ExcelData.class, new PageReadListener<ExcelData>(dataList -> {
            // 并且每行数据，并将其 add 至 list 中
            for (ExcelData excelData : dataList) {
                if (excelData != null) {
                    list.add(excelData);
                }
            }
        })).excelType(ExcelTypeEnum.XLSX).sheet().doRead(); // 指定 Excel 的文件后缀，开始分析读取
        for (ExcelData excelData : list) {
            System.out.println(excelData.getDate() + "," + excelData.getUserNum());
        }
    }

    /**
     * 知道表头，并形成映射关系
     * @throws FileNotFoundException
     */
    @Test
    public void doImportsForMappingByInnerClass() throws FileNotFoundException{
        File file = ResourceUtils.getFile("classpath:网站数据.xlsx");
        EasyExcel.read(file, ExcelData.class, new ReadListener<ExcelData>() {

            // 单次缓存的数据量
            public static final int BATCH_COUNT = 2;

            // 临时存储的列表
            private List<ExcelData> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
            @Override
            public void invoke(ExcelData excelData, AnalysisContext analysisContext) {
                cachedDataList.add(excelData);
                getData(excelData);
                if (cachedDataList.size() >= BATCH_COUNT) {
                    cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                System.out.println("存储数据库成功");
            }

            private void getData(ExcelData excelData) {
                System.out.println(excelData.getDate() + "," + excelData.getUserNum());
            }
        }).excelType(ExcelTypeEnum.XLSX).sheet().doRead();

    }
}