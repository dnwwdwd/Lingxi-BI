package com.hjj.lingxibi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hjj.lingxibi.common.BaseResponse;
import com.hjj.lingxibi.common.DeleteRequest;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.common.ResultUtils;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.model.dto.team.TeamAddRequest;
import com.hjj.lingxibi.model.dto.team.TeamQueryRequest;
import com.hjj.lingxibi.model.vo.TeamVO;
import com.hjj.lingxibi.service.TeamService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RequestMapping("/team")
@RestController
public class TeamController {

    @Resource
    private TeamService teamService;

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
    public BaseResponse<Page<TeamVO>> listTeam(@RequestBody TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<TeamVO> teamVOS = teamService.listTeam(teamQueryRequest, request);
        return ResultUtils.success(teamVOS);
    }

}
