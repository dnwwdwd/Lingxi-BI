package com.hjj.lingxibi.bizmq;

import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.constant.BIMQConstant;
import com.hjj.lingxibi.constant.CommonConstant;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.manager.AIManager;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.service.ChartService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class BIMessageConsumer {
    @Resource
    private ChartService chartService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private AIManager aiManager;

    // 制定消费者监听哪个队列和消息确认机制
    @SneakyThrows
    @RabbitListener(queues = {BIMQConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage is {}", message);
        if(StringUtils.isBlank(message)) {
            // 如果失败，消息拒绝
            channel.basicNack(deliveryTag, false, false);
            log.info("消息为空拒绝接收");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }

        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            channel.basicNack(deliveryTag, false, false);
            log.info("图标为空拒绝接收");
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }

        // 先修改图表任务状态为“执行中”。等执行成功后，修改为“已完成”、保存执行结果；执行失败后，状态修改为“失败”，记录任务失败信息。
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            channel.basicNack(deliveryTag, false, false);
            handlerChartUpdateError(chart.getId(), "更新图表执行状态失败");
            return;
        }
        // 调用AI
        String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, buildUserInput(chart));
        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            channel.basicNack(deliveryTag, false, false);
            handlerChartUpdateError(chart.getId(), "AI生成错误");
            return;
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus("succeed");
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            channel.basicNack(deliveryTag, false, false);
            handlerChartUpdateError(chart.getId(), "更新图表成功状态失败");
        }
        // 如果任务执行成功，手动执行ack
        channel.basicAck(deliveryTag, false);
    }


    private void handlerChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }

    /**
     * 构建用户输入
     * @param chart
     * @return
     */
    private String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        // 压缩后的数据

        userInput.append(csvData).append("\n");
        return userInput.toString();
    }
}
