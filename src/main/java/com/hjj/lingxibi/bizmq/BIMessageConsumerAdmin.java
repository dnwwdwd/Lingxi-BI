package com.hjj.lingxibi.bizmq;

import cn.hutool.json.JSONUtil;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.manager.SSEManager;
import com.hjj.lingxibi.model.dto.ai.ChatGPTResponse;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.service.ChartService;
import com.hjj.lingxibi.service.UserService;
import com.hjj.lingxibi.utils.AIUtil;
import com.hjj.lingxibi.utils.ChartUtil;
import com.hjj.lingxibi.utils.MQUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
public class BIMessageConsumerAdmin {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private SSEManager sseManager;

    // 制定消费者监听哪个队列和消息确认机制
    @RabbitListener(queues = {"bi_common_queue_admin"}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage is {}", message);
        if (StringUtils.isBlank(message)) {
            // 如果失败，消息拒绝
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException e) {
                log.info("消息拒绝失败：", e);
                throw new RuntimeException(e);
            }
            log.info("消息为空拒绝接收");
        }
        // 将消息转换为对象
        MQMessage mqMessage = JSONUtil.toBean(message, MQMessage.class);
        Long chartId = mqMessage.getChartId();

        if (chartId == null || chartId < 1) {
            // 如果失败，消息拒绝
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException e) {
                log.info("消息拒绝失败：", e);
                throw new RuntimeException(e);
            }
            log.info("消息为空拒绝接收");
        }
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException e) {
                log.info("消息拒绝失败：", e);
                throw new RuntimeException(e);
            }
            log.info("图表为空拒绝接收");
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }

        // 先修改图表任务状态为“执行中”。等执行成功后，修改为“已完成”、保存执行结果；执行失败后，状态修改为“失败”，记录任务失败信息。
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException e) {
                log.info("消息拒绝失败：", e);
                throw new RuntimeException(e);
            }
            chartService.handleChartUpdateError(chart.getId(), "更新图表执行状态失败");
            return;
        }
        String userInput = ChartUtil.buildUserInput(chart);
        log.info("用户: {} 调用 ChatGPT 的输入: {}", chart.getUserId(), userInput);
        // 调用 ChatGPT
        String response = "";
        try {
            response = AIUtil.invokeChatGPT(userInput, chartId);
        } catch (Exception e) {
            chartService.handleChartUpdateError(chartId, "调用ChatGPT失败");
            MQUtil.rejectMsgAndRequeue(channel, deliveryTag, chartId);
            return;
        }
        ChatGPTResponse chatGPTResponse = AIUtil.extractAIResponseFoChatGPT(response);
        String content = chatGPTResponse.getContent();
        String genChart = AIUtil.extractJsCode(content);
        genChart = ChartUtil.optimizeGenChart(genChart);
        String genResult = AIUtil.extractAnalysis(content).trim();
        log.info("图表JS代码为：" + genChart);
        log.info("生成结论为：" + genResult);
        // 检查生成的 Echarts 代码是否合法
        boolean isValid = ChartUtil.isChartValid(genChart);
        log.info("图表Id为" + chartId + "生成的Echarts代码是否合法：" + isValid);
        // 生成的 Echarts 代码不合法
        if (!isValid) {
            chartService.handleChartUpdateError(chartId, "生成的 Echarts 代码不合法");
            return;
        }
        // 生成的 Echarts 代码合法则将生成的Echarts代码进行增强，拓展下载图表功能
        genChart = ChartUtil.strengthenGenChart(genChart);
        // 更新图表状态为成功
        chartService.handleChartUpdateSuccess(chartId, genChart, genResult);
        Long userId = chart.getUserId();

        // 将生成的图表推送到SSE
        sseManager.sendChartUpdate(userId, chart);

        // 如果任务执行成功，手动执行ack
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.info("消息应答失败：", e);
            throw new RuntimeException(e);
        }
    }

}