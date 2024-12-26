package com.hjj.lingxibi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hjj.lingxibi.common.BaseResponse;
import com.hjj.lingxibi.common.DeleteRequest;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.common.ResultUtils;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.model.dto.chart.ChartQueryRequest;
import com.hjj.lingxibi.model.dto.team.TeamAddRequest;
import com.hjj.lingxibi.model.dto.team.TeamQueryRequest;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.model.entity.Team;
import com.hjj.lingxibi.model.vo.TeamVO;
import com.hjj.lingxibi.service.ChartService;
import com.hjj.lingxibi.service.TeamService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RequestMapping("/team")
@RestController
public class TeamController {

    @Resource
    private TeamService teamService;

    @Resource
    private ChartService chartService;

    @PostMapping("/add")
    public BaseResponse<Boolean> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = teamService.addTeam(teamAddRequest, request);
        return ResultUtils.success(b);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = teamService.deleteTeam(deleteRequest, request);
        return ResultUtils.success(b);
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<TeamVO>> listTeamByPage(@RequestBody TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<TeamVO> teamVOS = teamService.listTeam(teamQueryRequest, request);
        return ResultUtils.success(teamVOS);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody Team team, HttpServletRequest request) {
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = teamService.joinTeam(team, request);
        return ResultUtils.success(b);
    }

    @PostMapping("/exit")
    public BaseResponse<Boolean> exitTeam(@RequestBody Team team, HttpServletRequest request) {
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = teamService.exitTeam(team, request);
        return ResultUtils.success(b);
    }

    @PostMapping("/page/my/joined")
    public BaseResponse<Page<TeamVO>> listMyJoinedTeam(@RequestBody TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<TeamVO> teamVOS = teamService.listMyJoinedTeam(teamQueryRequest, request);
        return ResultUtils.success(teamVOS);
    }

    @PostMapping("/chart/page")
    public BaseResponse<Page<Chart>> listTeamChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        Long teamId = chartQueryRequest.getTeamId();
        if (teamId == null || teamId < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<Chart> chartPage = chartService.pageTeamChart(chartQueryRequest);
        return ResultUtils.success(chartPage);
    }

}