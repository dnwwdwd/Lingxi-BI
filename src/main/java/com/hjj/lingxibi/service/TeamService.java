package com.hjj.lingxibi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hjj.lingxibi.common.DeleteRequest;
import com.hjj.lingxibi.model.dto.team.TeamAddRequest;
import com.hjj.lingxibi.model.dto.team.TeamQueryRequest;
import com.hjj.lingxibi.model.entity.Team;
import com.hjj.lingxibi.model.vo.TeamVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author hejiajun
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-12-09 10:22:36
*/
public interface TeamService extends IService<Team> {

    boolean addTeam(TeamAddRequest teamAddRequest, HttpServletRequest request);

    boolean deleteTeam(DeleteRequest deleteRequest, HttpServletRequest request);

    Page<TeamVO> listTeam(TeamQueryRequest teamQueryRequest, HttpServletRequest request);
}
