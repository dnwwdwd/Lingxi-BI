package com.hjj.lingxibi.mapper;

import com.hjj.lingxibi.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author hejiajun
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2024-01-25 19:35:15
* @Entity com.hjj.lingxibi.model.entity.User
*/
public interface UserMapper extends BaseMapper<User> {
    List<Long> queryUsersId();

}