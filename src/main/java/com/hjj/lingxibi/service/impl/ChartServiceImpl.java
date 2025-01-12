package com.hjj.lingxibi.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.hjj.lingxibi.bizmq.BIMessageProducer;
import com.hjj.lingxibi.bizmq.MQMessage;
import com.hjj.lingxibi.common.DeleteRequest;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.constant.CommonConstant;
import com.hjj.lingxibi.constant.RedisConstant;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.exception.ThrowUtils;
import com.hjj.lingxibi.manager.RedisLimiterManager;
import com.hjj.lingxibi.manager.SSEManager;
import com.hjj.lingxibi.manager.ZhiPuAIManager;
import com.hjj.lingxibi.mapper.ChartMapper;
import com.hjj.lingxibi.model.dto.chart.ChartQueryRequest;
import com.hjj.lingxibi.model.dto.chart.ChartRegenRequest;
import com.hjj.lingxibi.model.dto.chart.GenChartByAIRequest;
import com.hjj.lingxibi.model.dto.team_chart.ChartAddToTeamRequest;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.model.entity.Team;
import com.hjj.lingxibi.model.entity.TeamChart;
import com.hjj.lingxibi.model.entity.User;
import com.hjj.lingxibi.model.vo.BIResponse;
import com.hjj.lingxibi.service.ChartService;
import com.hjj.lingxibi.service.TeamChartService;
import com.hjj.lingxibi.service.TeamService;
import com.hjj.lingxibi.service.UserService;
import com.hjj.lingxibi.utils.AIUtil;
import com.hjj.lingxibi.utils.ChartUtil;
import com.hjj.lingxibi.utils.ExcelUtils;
import com.hjj.lingxibi.utils.SqlUtils;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author hejiajun
 * @description 针对表【chart(图表信息表)】的数据库操作Service实现
 * @createDate 2024-01-25 19:35:15
 */
@Service
@Slf4j
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Resource
    private UserService userService;

    @Resource
    private ChartMapper chartMapper;

    @Resource
    private TeamChartService teamChartService;

    @Resource
    private TeamService teamService;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private BIMessageProducer biMessageProducer;

    @Resource
    private Retryer<Boolean> retryer;

    @Resource
    private ZhiPuAIManager zhiPuAIManager;

    @Resource
    private SSEManager sseManager;
    @Autowired
    private UserServiceImpl userServiceImpl;

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
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
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
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = chartMapper.queryUserIdByChartId(id);
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return userId;
    }

    @Override
    public BIResponse genChartByAIAsyncMq(MultipartFile multipartFile, GenChartByAIRequest genChartByAIRequest,
                                          HttpServletRequest request) {
        // 获取登录用户信息
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        boolean canGenChart = userService.canGenerateChart(loginUser);
        // 判断能否生成图表
        if (!canGenChart) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您同时生成图表过多，请稍后再生成");
        }
        userService.increaseUserGeneratIngCount(userId);
        // 先校验用户积分是否足够
        boolean hasScore = userService.userHasScore(loginUser);
        if (!hasScore) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户积分不足");
        }
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
        // 限流判断
        redisLimiterManager.doRateLimit(RedisConstant.REDIS_LIMITER_ID + userId);

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
            trySendMessageByMq(newChartId);
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
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        // 判断能否生成图表
        boolean canGenChart = userService.canGenerateChart(loginUser);
        if (!canGenChart) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您同时生成图表过多，请稍后再生成");
        }
        userService.increaseUserGeneratIngCount(userId);
        // 先校验用户积分是否足够
        boolean hasScore = userService.userHasScore(loginUser);
        if (!hasScore) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户积分不足");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
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

    @Override
    public synchronized BIResponse genChartByAI(MultipartFile multipartFile, GenChartByAIRequest genChartByAIRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        boolean canGenChart = userService.canGenerateChart(loginUser);
        // 判断能否生成图表
        if (!canGenChart) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您同时生成图表过多，请稍后再生成");
        }
        userService.increaseUserGeneratIngCount(userId);
        // 先校验用户积分是否足够
        boolean hasScore = userService.userHasScore(loginUser);
        if (!hasScore) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户积分不足");
        }
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

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ",请注意图表类型为" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");
        log.info("用户输入诉求：{}", userInput);
        String response = null;
        try {
            response = zhiPuAIManager.doChat(new ChatMessage(ChatMessageRole.USER.value(), userInput.toString()));
        } catch (Exception e) {
            saveAndReturnFailedChart(name, goal, chartType, csvData, "",
                    "", "智谱AI调用失败", userId);
            throw new BusinessException(ErrorCode.THIRD_SERVICE_ERROR, e.getMessage());
        }
        String genResult = AIUtil.extractAnalysis(response).trim();
        String genChart = AIUtil.extractJsCode(response);
        genChart = ChartUtil.optimizeGenChart(genChart);
        log.info("生成的数据结论：" + genResult);
        log.info("生成的JS代码：" + genChart);
        boolean isValid = ChartUtil.isChartValid(genChart);
        log.info("生成的Echarts代码是否合法：{}", isValid);
        if (!isValid) {
            this.saveAndReturnFailedChart(name, goal, chartType, csvData, genChart,
                    genResult, "生成的JS代码不合法", userId);
            throw new BusinessException(ErrorCode.THIRD_SERVICE_ERROR, "图表生成失败");
        }
        genChart = ChartUtil.strengthenGenChart(genChart);
        // 正在生成图表数量 - 1
        userService.deductUserGeneratIngCount(loginUser.getId());
        // 插入数据到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(userId);
        chart.setStatus("succeed");
        boolean saveResult = this.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        // 更新用户积分和正在生成的图表数量
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper.setSql("score = score - 5, generating_count = generatingCount - 1");
        userUpdateWrapper.eq("id", userId);
        boolean update = userService.update(userUpdateWrapper);
        if (!update) {
            log.error("用户 {} 积分扣除失败", userId);
        }
        log.info("图表 Id 为：{} 的对象信息: {}", chart.getId(), chart.toString());
        BIResponse biResponse = new BIResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        return biResponse;
    }

    @Override
    public Page<Chart> searchMyCharts(ChartQueryRequest chartQueryRequest) {
        String name = chartQueryRequest.getName();
        long current = chartQueryRequest.getCurrent();
        long pageSize = chartQueryRequest.getPageSize();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        queryWrapper.and(qw -> qw.like(StringUtils.isNotBlank(name), "name", name).or().like(StringUtils.isNotBlank(name), "chartType", name));
        Page<Chart> page = this.page(new Page<>(current, pageSize), queryWrapper);
        return page;
    }

    @Override
    public void handleChartUpdateSuccess(Long chartId, String genChart, String genResult) {
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setStatus("succeed");
        boolean updateResult = this.updateById(chart);
        if (!updateResult) {
            log.error("图表Id: {} 更新状态为成功失败了", chartId);
        }
    }

    @Override
    public void handleChartUpdateError(Long chartId, String execMessage) {
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setStatus("failed");
        chart.setExecMessage(execMessage);
        boolean updateResult = this.updateById(chart);
        if (!updateResult) {
            log.error("更新图表状态为失败失败了" + chartId + "," + execMessage);
        }
    }

    private void trySendMessageByMq(long chartId) {
        MQMessage mqMessage = MQMessage.builder().chartId(chartId).build();
        String mqMessageJson = JSONUtil.toJsonStr(mqMessage);
        try {
            biMessageProducer.sendMessage(mqMessageJson);
        } catch (Exception e) {
            log.error("图表成功保存至数据库，但是消息投递失败");
            Chart failedChart = new Chart();
            failedChart.setId(chartId);
            failedChart.setStatus("failed");
            boolean b = this.updateById(failedChart);
            if (!b) {
                throw new RuntimeException("修改图表状态信息为失败失败了");
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "MQ 消息发送失败");
        }
    }

    private void trySendMessageByMq(long chartId, long teamId) {
        MQMessage mqMessage = MQMessage.builder().chartId(chartId).teamId(teamId).build();
        String mqMessageJson = JSONUtil.toJsonStr(mqMessage);
        try {
            biMessageProducer.sendMessage(mqMessageJson);
        } catch (Exception e) {
            log.error("图表成功保存至数据库，但是消息投递失败");
            Chart failedChart = new Chart();
            failedChart.setId(chartId);
            failedChart.setStatus("failed");
            boolean b = this.updateById(failedChart);
            if (!b) {
                throw new RuntimeException("修改图表状态信息为失败失败了");
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "MQ 消息发送失败");
        }
    }

    private void trySendMessageByMq(long chartId, long teamId, long invokeUserId) {
        MQMessage mqMessage = MQMessage.builder().chartId(chartId).teamId(teamId).invokeUserId(invokeUserId).build();
        String mqMessageJson = JSONUtil.toJsonStr(mqMessage);
        try {
            biMessageProducer.sendMessage(mqMessageJson);
        } catch (Exception e) {
            log.error("图表成功保存至数据库，但是消息投递失败");
            Chart failedChart = new Chart();
            failedChart.setId(chartId);
            failedChart.setStatus("failed");
            boolean b = this.updateById(failedChart);
            if (!b) {
                throw new RuntimeException("修改图表状态信息为失败失败了");
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "MQ 消息发送失败");
        }
    }

    public void trySendMessageToAdminConsumer(long chartId) {
        MQMessage mqMessage = MQMessage.builder().chartId(chartId).build();
        String mqMessageJson = JSONUtil.toJsonStr(mqMessage);
        try {
            biMessageProducer.sendMessageToCommonQueue(mqMessageJson);
        } catch (Exception e) {
            log.error("图表成功保存至数据库，但是消息投递失败（admin）");
            Chart failedChart = new Chart();
            failedChart.setId(chartId);
            failedChart.setStatus("failed");
            boolean b = this.updateById(failedChart);
            if (!b) {
                throw new RuntimeException("修改图表状态信息为失败失败了");
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "MQ 消息发送失败（admin）");
        }
    }

    /**
     * 创建并返回一个状态为失败的图表，并将相关信息落表
     *
     * @param name
     * @param goal
     * @param chartType
     * @param chartData
     * @param genResult
     * @param execMessage
     * @param userId
     */
    @Override
    public void saveAndReturnFailedChart(String name, String goal, String chartType,
                                         String chartData, String genChart, String genResult, String execMessage, Long userId) {
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartType(chartType);
        chart.setChartData(chartData);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setExecMessage(execMessage);
        chart.setUserId(userId);
        chart.setStatus("failed");
        this.save(chart);
    }

    @Override
    public boolean addChartToTeam(ChartAddToTeamRequest chartAddToTeamRequest, HttpServletRequest request) {
        Long chartId = chartAddToTeamRequest.getChartId();
        Long teamId = chartAddToTeamRequest.getTeamId();
        Chart chart = this.getById(chartId);
        if (chart == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图表不存在");
        }
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        TeamChart teamChart = TeamChart.builder().teamId(teamId).chartId(chartId).build();
        return teamChartService.save(teamChart);
    }

    @Override
    public Page<Chart> pageTeamChart(ChartQueryRequest chartQueryRequest) {
        Long teamId = chartQueryRequest.getTeamId();
        long current = chartQueryRequest.getCurrent();
        long pageSize = chartQueryRequest.getPageSize();
        String name = chartQueryRequest.getName();
        // 建立 SSE 连接
        Page<TeamChart> teamChartPage = teamChartService.page(new Page<>(current, pageSize),
                new QueryWrapper<TeamChart>().eq("teamId", teamId));
        if (CollectionUtils.isEmpty(teamChartPage.getRecords())) {
            return new Page<>();
        }
        List<Long> chartIds = teamChartPage.getRecords().stream()
                .map(TeamChart::getChartId).collect(Collectors.toList());
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        queryWrapper.in(CollectionUtils.isNotEmpty(chartIds), "id", chartIds);
        queryWrapper.like(StringUtils.isNotEmpty(name), "name", name);
        Page<Chart> chartPage = this.page(new Page<>(current, pageSize), queryWrapper);
        chartPage.setTotal(chartIds.size());
        return chartPage;
    }

    @Override
    public Boolean updateChartByAdmin(Chart chart) {
        Long chartId = chart.getId();
        Chart oldChart = this.getById(chartId);
        if (!oldChart.getChartData().equals(chart.getChartData())) {
            trySendMessageToAdminConsumer(chartId);
        }
        return this.updateById(chart);
    }

    @Override
    public Page<Chart> pageChart(ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        String searchParams = chartQueryRequest.getSearchParams();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        QueryWrapper<Chart> queryWrapper = this.getQueryWrapper(chartQueryRequest);
        queryWrapper.like(StringUtils.isNotEmpty(searchParams), "name", searchParams).or(StringUtils.isNotEmpty(searchParams), wrapper -> wrapper.like("status", searchParams));
        queryWrapper.orderBy(true, true, "updateTime");
        Page<Chart> chartPage = this.page(new Page<>(current, size), queryWrapper);
        return chartPage;
    }

    @Override
    public Boolean regenChartByAsyncMqAdmin(ChartRegenRequest chartRegenRequest, HttpServletRequest request) {
        Long charId = chartRegenRequest.getId();
        Chart chart = new Chart();
        chart.setId(charId);
        chart.setStatus("wait");
        boolean b = this.updateById(chart);
        if (!b) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表状态更新失败");
        }
        trySendMessageToAdminConsumer(charId);
        return true;
    }


    @Override
    public boolean deleteChart(DeleteRequest deleteRequest, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = this.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = this.removeById(id);
        return b;
    }

    @Override
    public BIResponse regenChartByAsyncMqFromTeam(ChartRegenRequest chartRegenRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 判断能否生成图表
        boolean canGenChart = userService.canGenerateChart(loginUser);
        if (!canGenChart) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您同时生成图表过多，请稍后再生成");
        }
        userService.increaseUserGeneratIngCount(userId);
        // 先校验用户积分是否足够
        boolean hasScore = userService.userHasScore(loginUser);
        if (!hasScore) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户积分不足");
        }
        // 参数校验
        Long chartId = chartRegenRequest.getId();
        String name = chartRegenRequest.getName();
        String goal = chartRegenRequest.getGoal();
        String chartData = chartRegenRequest.getChartData();
        String chartType = chartRegenRequest.getChartType();
        Long teamId = chartRegenRequest.getTeamId();
        ThrowUtils.throwIf(chartId == null || chartId <= 0, ErrorCode.PARAMS_ERROR, "图表不存在");
        ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "图表名称为空");
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        ThrowUtils.throwIf(StringUtils.isBlank(chartData), ErrorCode.PARAMS_ERROR, "原始数据为空");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType), ErrorCode.PARAMS_ERROR, "图表类型为空");
        ThrowUtils.throwIf(teamId == null, ErrorCode.PARAMS_ERROR, "队伍Id为空");
        // 查看重新生成的图标是否存在
        ChartQueryRequest chartQueryRequest = new ChartQueryRequest();
        chartQueryRequest.setId(chartId);
        Long chartCount = chartMapper.selectCount(this.getQueryWrapper(chartQueryRequest));
        if (chartCount <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图表不存在");
        }
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
            trySendMessageByMq(chartId, teamId, userId);
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
                    trySendMessageByMq(chartId, teamId);
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

}
