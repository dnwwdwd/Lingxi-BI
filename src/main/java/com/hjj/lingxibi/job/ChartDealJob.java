package com.hjj.lingxibi.job;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjj.lingxibi.bizmq.BIMessageProducer;
import com.hjj.lingxibi.bizmq.MQMessage;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.service.ChartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChartDealJob {

    @Resource
    private ChartService chartService;

    @Resource
    private BIMessageProducer biMessageProducer;


    @Scheduled(cron = "0 */2 * * * ?")
    public void dealWaitingChart() {
        log.info("定时任务执行");
        List<Chart> charts = chartService.list(new QueryWrapper<Chart>().eq("status", "waiting"));
        if (CollectionUtils.isEmpty(charts)) {
            return;
        }
        List<Long> chartIds = charts.stream().map(Chart::getId).collect(Collectors.toList());
        for (long chartId : chartIds) {
            MQMessage mqMessage = MQMessage.builder().chartId(chartId).build();
            String mqMessageJson = JSONUtil.toJsonStr(mqMessage);
            try {
                biMessageProducer.sendMessage(mqMessageJson);
            } catch (Exception e) {
                log.error("图表成功保存至数据库，但是消息投递失败");
                Chart failedChart = new Chart();
                failedChart.setId(chartId);
                failedChart.setStatus("failed");
                boolean b = chartService.updateById(failedChart);
                if (!b) {
                    throw new RuntimeException("修改图表状态信息为失败失败了");
                }
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "MQ 消息发送失败");
            }
        }

    }

}
