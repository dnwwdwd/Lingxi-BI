package com.hjj.lingxibi.bizmq;

import cn.hutool.json.JSONUtil;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.manager.SSEManager;
import com.hjj.lingxibi.manager.ZhiPuAIManager;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.model.entity.User;
import com.hjj.lingxibi.service.ChartService;
import com.hjj.lingxibi.service.UserService;
import com.hjj.lingxibi.utils.AIUtil;
import com.hjj.lingxibi.utils.ChartUtil;
import com.hjj.lingxibi.utils.MQUtil;
import com.rabbitmq.client.Channel;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

import static com.hjj.lingxibi.utils.ChartUtil.buildUserInput;
import static com.hjj.lingxibi.utils.ChartUtil.strengthenGenChart;

@Component
@Slf4j
public class BIMessageConsumerByZhiPuAI {
    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private SSEManager sseManager;

    @Resource
    private ZhiPuAIManager zhiPuAIManager;

    // 制定消费者监听哪个队列和消息确认机制
    @RabbitListener(queues = {"bi_common_queue"}, ackMode = "MANUAL")
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
            log.info("此消息正在被转发到死信队列中");
        }

        // 将消息转换为对象
        MQMessage mqMessage = JSONUtil.toBean(message, MQMessage.class);
        Long chartId = mqMessage.getChartId();
        Long teamId = mqMessage.getTeamId();
        Long invokeUserId = mqMessage.getInvokeUserId();

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
        if (chart.getStatus().equals("succeed")) {
            try {
                channel.basicAck(deliveryTag, false);
            } catch (IOException e) {
                log.info("消息应答失败：", e);
                throw new RuntimeException(e);
            }
            return;
        }
        Long userId = chart.getUserId();
        User user = userService.getById(chart.getUserId());
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
        // 如果图表状态改为“running”失败，则将图表状态想改为“失败”，再重新投递消息到队列中
        if (!b) {
            try {
                channel.basicReject(deliveryTag, true);
            } catch (IOException e) {
                log.error("消息拒绝失败，图表Id：" + chartId + "，异常信息" + e);
                throw new RuntimeException(e);
            }
            chartService.handleChartUpdateError(chart.getId(), "更新图表执行状态失败");
            deductUserGeneratIngCount(userId, invokeUserId);
            return;
        }
        String userInput = buildUserInput(chart);
        // 调用智谱 AI
        log.info("用户: {} 调用智谱 AI 的输入: {}", chart.getUserId(), userInput);
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), userInput);
        String result = null;
        try {
            result = zhiPuAIManager.doChat(chatMessage, chartId);
        } catch (Exception e) {
            chartService.handleChartUpdateError(chart.getId(), "调用智谱AI失败");
            deductUserGeneratIngCount(userId, invokeUserId);
            log.info("图表Id: {} 调用智谱 AI 失败了", chartId);
            MQUtil.rejectMsgAndRequeue(channel, deliveryTag, chartId);
            return;
        }
        String genResult = AIUtil.extractAnalysis(result).trim();
        String genChart = AIUtil.extractJsCode(result).trim();
        genChart = ChartUtil.optimizeGenChart(genChart);
        // 检查生成的 Echarts 代码是否合法
        boolean isValid = ChartUtil.isChartValid(genChart);
        log.info("图表Id为" + chartId + "生成的Echarts代码是否合法：" + isValid);
        // 生成的 Echarts 代码不合法
        if (!isValid) {
            chartService.handleChartUpdateError(chartId, "生成的 Echarts 代码不合法");
            deductUserGeneratIngCount(userId, invokeUserId);
            return;
        }
        // 生成的 Echarts 代码合法则将生成的Echarts代码进行增强，拓展下载图表功能
        genChart = strengthenGenChart(genChart);
        // 扣除用户正在生成的图表数量
        deductUserGeneratIngCount(userId, invokeUserId);
        // 更新图表任务状态为成功
        chartService.handleChartUpdateSuccess(chartId, genChart, genResult);
        // 扣除用户积分（调用一次 AI 服务，扣除5个积分）
        userService.deductUserScore(userId);
        // 将生成的图表推送到SSE
        if (teamId != null) {
            sseManager.sendTeamChartUpdate(teamId, chartService.getById(chartId));
        } else {
            sseManager.sendChartUpdate(userId, chartService.getById(chartId));
        }

        // 如果任务执行成功，手动执行ack
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("图表Id为" + chartId + "消息应答失败：" + e);
            throw new RuntimeException(e);
        }
    }

    private void deductUserGeneratIngCount(Long userId, Long invokeUserId) {
        if (invokeUserId == null) {
            userService.deductUserGeneratIngCount(userId);
        } else {
            userService.deductUserGeneratIngCount(invokeUserId);
        }
    }

}