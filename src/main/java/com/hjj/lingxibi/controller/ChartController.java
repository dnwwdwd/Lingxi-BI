package com.hjj.lingxibi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hjj.lingxibi.annotation.AuthCheck;
import com.hjj.lingxibi.common.BaseResponse;
import com.hjj.lingxibi.common.DeleteRequest;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.common.ResultUtils;
import com.hjj.lingxibi.constant.UserConstant;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.exception.ThrowUtils;
import com.hjj.lingxibi.model.dto.chart.ChartAddRequest;
import com.hjj.lingxibi.model.dto.chart.ChartQueryRequest;
import com.hjj.lingxibi.model.dto.chart.ChartRegenRequest;
import com.hjj.lingxibi.model.dto.chart.GenChartByAIRequest;
import com.hjj.lingxibi.model.dto.team_chart.ChartAddToTeamRequest;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.model.entity.User;
import com.hjj.lingxibi.model.vo.BIResponse;
import com.hjj.lingxibi.service.ChartService;
import com.hjj.lingxibi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 图表接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newPostId = chart.getId();
        return ResultUtils.success(newPostId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = chartService.deleteChart(deleteRequest, request);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chart
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChartByAdmin(@RequestBody Chart chart) {
        if (chart == null || chart.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = chartService.updateChartByAdmin(chart);
        return ResultUtils.success(b);
    }

    /**
     * 根据 AI 同步生成图表（同步）
     *
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BIResponse> genChartByAI(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAIRequest genChartByAIRequest, HttpServletRequest request) {
        if (multipartFile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件为空");
        }
        if (genChartByAIRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分析诉求为空");
        }
        if (userService.getLoginUser(request) == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        BIResponse biResponse = chartService.genChartByAI(multipartFile, genChartByAIRequest, request);
        return ResultUtils.success(biResponse);
    }

    /*    *//**
     * 根据AI异步生成图表，线程池实现
     *
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     *//*
    @PostMapping("/gen/async")
    public BaseResponse<BIResponse> genChartByAIAsync(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAIRequest genChartByAIRequest, HttpServletRequest request) {
        // 先校验用户积分是否足够
        boolean hasScore = userService.userHasScore(request);
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
        // 获取登录用户信息
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        // 限流判断
        redisLimiterManager.doRateLimit("genChartByAI_" + userId);


        // 无需写prompt，直接调用现有模型
*//*        final String prompt="你是一个数据分析刊师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                "分析需求：\n" +
                "{数据分析的需求和目标}\n" +
                "原始数据:\n" +
                "{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照以下格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "\n" +
                "【【【【【【\n" +
                "{前端Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化}\n" +
                "【【【【【【\n" +
                "{ 明确的数据分析结论、越详细越好，不要生成多余的注释 }";*//*

        long modelId = 1659171950288818178L;

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
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        // 插入数据到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(userId);
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // todo 建议处理任务队列满了后抛异常的情况
        CompletableFuture.runAsync(() -> {
            // 先修改图表任务状态为“执行中”。等执行成功后，修改为“已完成”、保存执行结果；执行失败后，状态修改为“失败”，记录任务失败信息。
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean b = chartService.updateById(updateChart);
            if (!b) {
                handlerChartUpdateError(chart.getId(), "更新图表执行状态失败");
                return;
            }
            // 调用AI
            String result = aiManager.doChat(modelId, userInput.toString());
            String[] splits = result.split("【【【【【");
            if (splits.length < 3) {
                handlerChartUpdateError(chart.getId(), "AI生成错误");
                return;
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus("success");
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                handlerChartUpdateError(chart.getId(), "更新图表成功状态失败");
            }

        }, threadPoolExecutor);
        BIResponse biResponse = new BIResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }*/


    /*
     * 根据AI异步生成图表，RabbitMQ实现（无重试机制）
     *
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     *//*
    @PostMapping("/gen/async")
    public BaseResponse<BIResponse> genChartByAIAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAIRequest genChartByAIRequest, HttpServletRequest request) {
        // 先校验用户积分是否足够
        boolean hasScore = userService.userHasScore(request);
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
        // 获取登录用户信息
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        // 限流判断
        redisLimiterManager.doRateLimit("genChartByAI_" + userId);


        // 无需写prompt，直接调用现有模型
*//*        final String prompt="你是一个数据分析刊师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                "分析需求：\n" +
                "{数据分析的需求和目标}\n" +
                "原始数据:\n" +
                "{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照以下格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "\n" +
                "【【【【【【\n" +
                "{前端Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化}\n" +
                "【【【【【【\n" +
                "{ 明确的数据分析结论、越详细越好，不要生成多余的注释 }";*//*

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
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // todo 建议处理任务队列满了后抛异常的情况
        Long newChartId = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(newChartId));

        BIResponse biResponse = new BIResponse();
        biResponse.setChartId(newChartId);
        return ResultUtils.success(biResponse);
    }*/


    /**
     * 根据AI异步生成图表，RabbitMQ实现（有重试机制）
     *
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BIResponse> genChartByAIAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                        GenChartByAIRequest genChartByAIRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "请传入文件");
        ThrowUtils.throwIf(genChartByAIRequest == null, ErrorCode.PARAMS_ERROR, "请输入分析诉求或图表标题");
        BIResponse biResponse = chartService.genChartByAIAsyncMq(multipartFile, genChartByAIRequest, request);
        return ResultUtils.success(biResponse);
    }


/*    private void handlerChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }*/

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/page")
    public BaseResponse<Page<Chart>> pageChart(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<Chart> chartPage = chartService.pageChart(chartQueryRequest);
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();

        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 无缓存查询数据库
        chartQueryRequest.setUserId(userId);
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        ThrowUtils.throwIf(chartPage == null, ErrorCode.SYSTEM_ERROR);
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 修改图表重新生成
     */
    @PostMapping("/regen")
    public BaseResponse<BIResponse> reGenChartByAsyncMq(@RequestBody ChartRegenRequest chartRegenRequest, HttpServletRequest request) {
        if (chartRegenRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图表不存在");
        }
        BIResponse biResponse = chartService.regenChartByAsyncMq(chartRegenRequest, request);
        return ResultUtils.success(biResponse);
    }

    /**
     * 修改图表重新生成
     */
    @PostMapping("/regen/by/admin")
    public BaseResponse<Boolean> reGenChartByAsyncMqAdmin(@RequestBody ChartRegenRequest chartRegenRequest, HttpServletRequest request) {
        if (chartRegenRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图表不存在");
        }
        Boolean b = chartService.regenChartByAsyncMqAdmin(chartRegenRequest, request);
        return ResultUtils.success(b);
    }

    /**
     * 按关键词搜索我的图表（MySQL 实现）
     */
    @PostMapping("/my/search/page")
    public BaseResponse<Page<Chart>> searchMyChart(@RequestBody ChartQueryRequest chartQueryRequest,
                                                   HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Page<Chart> page = chartService.searchMyCharts(chartQueryRequest);
        return ResultUtils.success(page);
    }

    @PostMapping("/add/team")
    public BaseResponse<Boolean> addChartToTeam(@RequestBody ChartAddToTeamRequest chartAddToTeamRequest, HttpServletRequest request) {
        if (chartAddToTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = chartService.addChartToTeam(chartAddToTeamRequest, request);
        return ResultUtils.success(b);
    }

}
