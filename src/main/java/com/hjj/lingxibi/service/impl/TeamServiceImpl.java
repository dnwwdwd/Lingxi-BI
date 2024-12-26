package com.hjj.lingxibi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hjj.lingxibi.common.DeleteRequest;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.constant.CommonConstant;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.mapper.TeamMapper;
import com.hjj.lingxibi.model.dto.team.TeamAddRequest;
import com.hjj.lingxibi.model.dto.team.TeamQueryRequest;
import com.hjj.lingxibi.model.entity.Team;
import com.hjj.lingxibi.model.entity.TeamUser;
import com.hjj.lingxibi.model.entity.User;
import com.hjj.lingxibi.model.vo.TeamVO;
import com.hjj.lingxibi.service.TeamService;
import com.hjj.lingxibi.service.TeamUserService;
import com.hjj.lingxibi.service.UserService;
import com.hjj.lingxibi.utils.SqlUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author hejiajun
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2024-12-09 10:22:36
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private UserService userService;

    @Resource
    private TeamUserService teamUserService;

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
        boolean b1 = this.save(team);
        TeamUser teamUser = new TeamUser();
        teamUser.setTeamId(team.getId());
        teamUser.setUserId(userId);
        boolean b2 = teamUserService.save(teamUser);
        return b1 && b2;
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
        queryWrapper.like(StringUtils.isNotEmpty(searchParam), "name", searchParam);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        Page<Team> teamPage = this.page(new Page<>(current, pageSize), queryWrapper);
        List<Team> teamPageRecords = teamPage.getRecords();
        List<TeamVO> teamVOs = this.getTeamVOList(teamPageRecords, request);
        Page<TeamVO> teamVOPage = new Page<>(current, pageSize);
        teamVOPage.setRecords(teamVOs);
        teamVOPage.setTotal(teamPage.getTotal());
        return teamVOPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinTeam(Team team, HttpServletRequest request) {
        Long teamId = team.getId();
        team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        if (isInTeam(team, request)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已在该队伍中");
        }
        QueryWrapper<TeamUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        long count = teamUserService.count(queryWrapper);
        if (count >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数已满");
        }
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        TeamUser teamUser = new TeamUser();
        teamUser.setTeamId(teamId);
        teamUser.setUserId(userId);
        boolean b = teamUserService.save(teamUser);
        return b;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean exitTeam(Team team, HttpServletRequest request) {
        Long teamId = team.getId();
        team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        if (!isInTeam(team, request)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不在队伍中");
        }
        QueryWrapper<TeamUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        long count = teamUserService.count(queryWrapper);
        if (count <= 1) {
            queryWrapper.eq("userId", userId);
            boolean b1 = teamUserService.remove(queryWrapper);
            boolean b2 = this.removeById(teamId);
            return b1 && b2;
        }
        queryWrapper.orderBy(true, true, "createTime").apply("limit 2");
        TeamUser teamUser = teamUserService.list(queryWrapper).get(1);
        Long newCaptainId = teamUser.getUserId();
        team.setUserId(newCaptainId);
        boolean b1 = this.updateById(team);
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId).eq("userId", userId);
        boolean b2 = teamUserService.remove(queryWrapper);
        return b1 && b2;
    }

    @Override
    public Page<TeamVO> listMyJoinedTeam(TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        String searchParam = teamQueryRequest.getSearchParam();
        long current = teamQueryRequest.getCurrent();
        long pageSize = teamQueryRequest.getPageSize();
        String sortField = teamQueryRequest.getSortField();
        String sortOrder = teamQueryRequest.getSortOrder();

        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        QueryWrapper<TeamUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        Page<TeamUser> teamUserPage = teamUserService.page(new Page<>(current, pageSize));
        List<Long> teamIds = teamUserPage.getRecords().stream().map(TeamUser::getTeamId)
                .collect(Collectors.toList());
        List<Team> teams = this.listByIds(teamIds);
        List<TeamVO> teamVOs = this.getTeamVOList(teams, request);
        Page<TeamVO> teamVOPage = new Page<>(current, pageSize);
        teamVOPage.setRecords(teamVOs);
        teamVOPage.setTotal(teamUserPage.getTotal());
        return teamVOPage;
    }

    private boolean isInTeam(Team team, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        QueryWrapper<TeamUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", team.getId());
        queryWrapper.eq("userId", userId);
        long count = teamUserService.count(queryWrapper);
        return count > 0;
    }

    private List<TeamVO> getTeamVOList(List<Team> teams, HttpServletRequest request) {
        return teams.stream().map(team -> {
            TeamVO teamVO = new TeamVO();
            BeanUtils.copyProperties(team, teamVO);
            teamVO.setUserVO(userService.getUserVOById(team.getUserId()));
            teamVO.setInTeam(this.isInTeam(team, request));
            return teamVO;
        }).collect(Collectors.toList());
    }

}




