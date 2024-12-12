package com.hjj.lingxibi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hjj.lingxibi.model.entity.TeamUser;
import com.hjj.lingxibi.service.TeamUserService;
import com.hjj.lingxibi.mapper.TeamUserMapper;
import org.springframework.stereotype.Service;

/**
* @author hejiajun
* @description 针对表【team_user(队伍用户关系表)】的数据库操作Service实现
* @createDate 2024-12-11 12:33:32
*/
@Service
public class TeamUserServiceImpl extends ServiceImpl<TeamUserMapper, TeamUser>
    implements TeamUserService{

}




