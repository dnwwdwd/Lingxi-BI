package com.hjj.lingxibi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hjj.lingxibi.annotation.AuthCheck;
import com.hjj.lingxibi.common.BaseResponse;
import com.hjj.lingxibi.common.DeleteRequest;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.common.ResultUtils;
import com.hjj.lingxibi.constant.UserConstant;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.model.dto.chart.ChartQueryRequest;
import com.hjj.lingxibi.model.dto.chart.ChartRegenRequest;
import com.hjj.lingxibi.model.dto.team.TeamAddRequest;
import com.hjj.lingxibi.model.dto.team.TeamQueryRequest;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.model.entity.Team;
import com.hjj.lingxibi.model.vo.BIResponse;
import com.hjj.lingxibi.model.vo.TeamVO;
import com.hjj.lingxibi.service.ChartService;
import com.hjj.lingxibi.service.TeamService;
import com.hjj.lingxibi.service.UserService;
import com.hjj.lingxibi.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RequestMapping("/team")
@RestController
public class TeamController {

    @Resource
    private TeamService teamService;

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @PostMapping("/add")
    public BaseResponse<Boolean> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = teamService.addTeam(teamAddRequest, request);
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
    public BaseResponse<Page<TeamVO>> pageMyJoinedTeam(@RequestBody TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<TeamVO> teamVOS = teamService.pageMyJoinedTeam(teamQueryRequest, request);
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

    @GetMapping("/list/my/joined")
    public BaseResponse<List<Team>> listAllMyJoinedTeam(HttpServletRequest request) {
        return ResultUtils.success(teamService.listAllMyJoinedTeam(request));
    }

    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/page")
    public BaseResponse<Page<Team>> pageTeam(@RequestBody TeamQueryRequest teamQueryRequest) {
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<Team> teamPage = teamService.pageTeam(teamQueryRequest);
        return ResultUtils.success(teamPage);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody Team team, HttpServletRequest request) {
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = teamService.updateTeam(team, request);
        return ResultUtils.success(b);
    }

    @AuthCheck(mustRole = "admin")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = teamService.deleteTeam(deleteRequest);
        return ResultUtils.success(b);
    }

    @PostMapping("/chart/regen")
    public BaseResponse<BIResponse> regenChart(@RequestBody ChartRegenRequest chartRegenRequest,
                                               HttpServletRequest request) {
        if (chartRegenRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        BIResponse biResponse = chartService.regenChartByAsyncMqFromTeam(chartRegenRequest, request);
        return ResultUtils.success(biResponse);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(Long id) {
        if (id == null || id < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        return ResultUtils.success(team);
    }

}