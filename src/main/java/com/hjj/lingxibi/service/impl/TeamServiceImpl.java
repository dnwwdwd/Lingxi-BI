package com.hjj.lingxibi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hjj.lingxibi.common.DeleteRequest;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.constant.CommonConstant;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.model.dto.team.TeamQueryRequest;
import com.hjj.lingxibi.model.entity.Team;
import com.hjj.lingxibi.model.entity.User;
import com.hjj.lingxibi.model.dto.team.TeamAddRequest;
import com.hjj.lingxibi.model.vo.TeamVO;
import com.hjj.lingxibi.service.TeamService;
import com.hjj.lingxibi.mapper.TeamMapper;
import com.hjj.lingxibi.service.UserService;
import com.hjj.lingxibi.utils.SqlUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author hejiajun
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2024-12-09 10:22:36
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserService userService;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public boolean addTeam(TeamAddRequest teamAddRequest, HttpServletRequest request) {
        String name = teamAddRequest.getName();
        List<String> imgs = teamAddRequest.getImgs();
        String description = teamAddRequest.getDescription();
        Integer maxNum = teamAddRequest.getMaxNum();
        if (StringUtils.isEmpty(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名称不能为空");
        }
        if (CollectionUtils.isEmpty(imgs)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空");
        }
        if (StringUtils.isEmpty(description) || description.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不能为空或长度大于100");
        }
        if (maxNum == null || maxNum < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最大人数不得为空或者小于1");
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        team.setUserId(userId);
        return this.save(team);
    }

    @Override
    public boolean deleteTeam(DeleteRequest deleteRequest, HttpServletRequest request) {
        Long teamId = deleteRequest.getId();
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员无权限删除");
        }
        boolean b = this.removeById(teamId);
        return b;
    }

    @Override
    public Page<TeamVO> listTeam(TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        String searchParam = teamQueryRequest.getSearchParam();
        long current = teamQueryRequest.getCurrent();
        long pageSize = teamQueryRequest.getPageSize();
        String sortField = teamQueryRequest.getSortField();
        String sortOrder = teamQueryRequest.getSortOrder();

        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(searchParam),"name", searchParam);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        Page<Team> teamPage = this.page(new Page<>(current, pageSize), queryWrapper);
        List<TeamVO> teamVOList = teamPage.getRecords().stream().map(team -> {
            TeamVO teamVO = new TeamVO();
            BeanUtils.copyProperties(team, teamVO);
            teamVO.setUserVO(userService.getUserVOById(team.getUserId()));
            return teamVO;
        }).collect(Collectors.toList());
        Page<TeamVO> teamVOPage = new Page<>(current, pageSize);
        teamVOPage.setRecords(teamVOList);
        teamVOPage.setTotal(teamPage.getTotal());
        return teamVOPage;
    }


}




