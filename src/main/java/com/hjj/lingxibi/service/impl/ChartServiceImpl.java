package com.hjj.lingxibi.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.hjj.lingxibi.bizmq.BIMessageProducer;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.constant.CommonConstant;
import com.hjj.lingxibi.constant.RedisConstant;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.exception.ThrowUtils;
import com.hjj.lingxibi.manager.RedisLimiterManager;
import com.hjj.lingxibi.mapper.ChartMapper;
import com.hjj.lingxibi.model.dto.chart.ChartQueryRequest;
import com.hjj.lingxibi.model.dto.chart.ChartRegenRequest;
import com.hjj.lingxibi.model.dto.chart.GenChartByAIRequest;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.model.entity.User;
import com.hjj.lingxibi.model.vo.BIResponse;
import com.hjj.lingxibi.service.ChartService;
import com.hjj.lingxibi.service.UserService;
import com.hjj.lingxibi.utils.ExcelUtils;
import com.hjj.lingxibi.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
* @author 17653
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-01-25 19:35:15
*/
@Service
@Slf4j
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

    @Resource
    private UserService userService;

    @Resource
    private ChartMapper chartMapper;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private BIMessageProducer biMessageProducer;

    @Resource
    private Retryer<Boolean> retryer;
    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        // 根据查询条件查询
        queryWrapper.eq( id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public Long queryUserIdByChartId(Long id) {
        if(id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = chartMapper.queryUserIdByChartId(id);
        if(userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return userId;
    }

    @Override
    public BIResponse genChartByAIAsyncMq(MultipartFile multipartFile, GenChartByAIRequest genChartByAIRequest,
                                          HttpServletRequest request) {
        String name = genChartByAIRequest.getName();
        String goal = genChartByAIRequest.getGoal();
        String chartType = genChartByAIRequest.getChartType();
        // 校验参数
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100,
                ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1M");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "非法文件后缀");
        // 获取登录用户信息
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        // 限流判断
        redisLimiterManager.doRateLimit(RedisConstant.REDIS_LIMITER_ID + userId);

        // 无需写prompt，直接调用现有模型
/*        final String prompt="你是一个数据分析刊师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                "分析需求：\n" +
                "{数据分析的需求和目标}\n" +
                "原始数据:\n" +
                "{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照以下格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "\n" +
                "【【【【【【\n" +
                "{前端Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化}\n" +
                "【【【【【【\n" +
                "{ 明确的数据分析结论、越详细越好，不要生成多余的注释 }";*/

        long modelId = CommonConstant.BI_MODEL_ID;
        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);


        // 插入数据到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(userId);

        BIResponse biResponse = null;

        // 尝试初次保存至数据库
        boolean saveResult = this.save(chart);

        // 图表保存成功直接发消息给MQ并返回图表id
        if (saveResult) {
            log.info("图表初次保存至数据库成功");
            Long newChartId = chart.getId();
            biMessageProducer.sendMessage(String.valueOf(newChartId));
            // 创建 BIResponse 对象并返回
            biResponse = new BIResponse();
            biResponse.setChartId(newChartId);
            return biResponse;

            // 如果图表保存失败，则尝试重试 -> 重新保存图表
        } else {
            try {
                // 使用 Guava Retryer 进行重试
                Boolean callResult = retryer.call(() -> {
                    boolean retrySaveResult = this.save(chart);
                    if (!retrySaveResult) {
                        log.warn("图表保存至数据库仍然失败，进行重试...");
                    }
                    return !retrySaveResult; // 继续重试的条件
                });
                // 重试保存至数据库成功后发送消息
                Long newChartId = chart.getId();
                if (callResult) {
                    // 图表信息重新保存至数据库向MQ投递消息
                    trySendMessageByMq(newChartId);
                }
                // 创建 BIResponse 对象并返回
                biResponse = new BIResponse();
                biResponse.setChartId(newChartId);
                return biResponse;
            } catch (RetryException e) {
                // 如果重试了出现异常就要将图表状态更新为failed，并打印日志
                log.error("重试保存至数据库失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "重试保存至数据库失败");
            } catch (ExecutionException e) {
                log.error("重试保存至数据库失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "重试保存至数据库失败");
            }
        }
    }

    @Override
    public BIResponse regenChartByAsyncMq(ChartRegenRequest chartRegenRequest, HttpServletRequest request) {
        // 参数校验
        Long chartId = chartRegenRequest.getId();
        String name = chartRegenRequest.getName();
        String goal = chartRegenRequest.getGoal();
        String chartData = chartRegenRequest.getChartData();
        String chartType = chartRegenRequest.getChartType();
        ThrowUtils.throwIf(chartId == null || chartId <= 0, ErrorCode.PARAMS_ERROR, "图表不存在");
        ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "图表名称为空");
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        ThrowUtils.throwIf(StringUtils.isBlank(chartData), ErrorCode.PARAMS_ERROR, "原始数据为空");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType), ErrorCode.PARAMS_ERROR, "图表类型为空");
        // 查看重新生成的图标是否存在
        ChartQueryRequest chartQueryRequest = new ChartQueryRequest();
        chartQueryRequest.setId(chartId);
        Long chartCount = chartMapper.selectCount(this.getQueryWrapper(chartQueryRequest));
        if (chartCount <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图表不存在");
        }
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        // 限流
        redisLimiterManager.doRateLimit(RedisConstant.REDIS_LIMITER_ID + userId);
        // 更改图表状态为wait
        Chart waitingChart = new Chart();
        BeanUtils.copyProperties(chartRegenRequest, waitingChart);
        waitingChart.setStatus("wait");
        boolean updateResult = this.updateById(waitingChart);
        // 将修改后的图表信息保存至数据库
        if (updateResult) {
            log.info("修改后的图表信息初次保存至数据库成功");
            // 初次保存成功，则向MQ投递消息
            trySendMessageByMq(chartId);
            BIResponse biResponse = new BIResponse();
            biResponse.setChartId(chartId);
            return biResponse;
        } else {    // 保存失败则继续重试尝试保存
            try {
                Boolean callResult = retryer.call(() -> {
                    boolean retryResult = this.updateById(waitingChart);
                    if (!retryResult) {
                        log.warn("修改后的图表信息保存至数据库仍然失败，进行重试...");
                    }
                    return !retryResult;
                });
                if (callResult) {
                    trySendMessageByMq(chartId);
                }
                BIResponse biResponse = new BIResponse();
                biResponse.setChartId(chartId);
                return biResponse;
            } catch (RetryException e) {
                // 如果重试了出现异常就要将图表状态更新为failed，并打印日志
                log.error("修改后的图表信息重试保存至数据库失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改后的图表信息重试保存至数据库失败");
            } catch (ExecutionException e) {
                log.error("修改后的图表信息重试保存至数据库失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改后的图表信息重试保存至数据库失败");
            }
        }
    }

    public void trySendMessageByMq(long chartId) {
        try {
            biMessageProducer.sendMessage(String.valueOf(chartId));
        } catch (Exception e) {
            log.error("图表成功保存至数据库，但是消息投递失败");
            Chart failedChart = new Chart();
            failedChart.setId(chartId);
            failedChart.setStatus("failed");
            boolean b = this.updateById(failedChart);
            if (!b) {
                throw new RuntimeException("修改图表状态信息为失败失败了");
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用 AI 接口失败");
        }
    }
}
