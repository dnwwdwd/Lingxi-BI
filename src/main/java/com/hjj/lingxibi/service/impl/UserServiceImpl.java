package com.hjj.lingxibi.service.impl;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.constant.CommonConstant;
import com.hjj.lingxibi.constant.RedisConstant;
import com.hjj.lingxibi.constant.UserConstant;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.mapper.UserMapper;
import com.hjj.lingxibi.model.dto.user.UserQueryRequest;
import com.hjj.lingxibi.model.entity.User;
import com.hjj.lingxibi.model.enums.UserRoleEnum;
import com.hjj.lingxibi.model.vo.LoginUserVO;
import com.hjj.lingxibi.model.vo.UserVO;
import com.hjj.lingxibi.service.UserService;
import com.hjj.lingxibi.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hjj.lingxibi.constant.UserConstant.USER_LOGIN_STATE;
import static com.hjj.lingxibi.constant.UserConstant.USER_SIGN_IN;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "yupi";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        String isSignIn = stringRedisTemplate.opsForValue().get(RedisConstant.USER_SIGN_IN_REDIS_ID + user.getId());
        loginUserVO.setIsSignIn(StringUtils.isNotEmpty(isSignIn) && isSignIn.equals(USER_SIGN_IN));
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public List<Long> queryUsersId() {
        return userMapper.queryUsersId();
    }


    @Override
    public boolean userHasScore(User user) {
        if (user == null || user.getScore() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Integer score = user.getScore();
        // 积分为空或者小于5，代表用户无积分，返回false
        return score != null && score >= 5;
    }


    @Override
    public void deductUserScore(Long userId) {
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper.eq("id", userId);
        userUpdateWrapper.setSql("score = score - 5");
        boolean updateScoreResult = this.update(userUpdateWrapper);
        if (!updateScoreResult) {
            log.error("用户: {} 积分扣除失败", userId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        log.info("用户: {} 积分扣除成功", userId);
    }

    @Override
    public UserVO getUserVOById(Long id) {
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return this.getUserVO(user);
    }

    @Override
    public synchronized void deductUserGeneratIngCount(Long userId) {
        User user = this.getById(userId);
        if (user.getGeneratingCount() == null || user.getGeneratingCount() <= 0) {
            return;
        }
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper.eq("id", userId);
        userUpdateWrapper.setSql("generatingCount = generatingCount - 1");
        boolean updateScoreResult = this.update(userUpdateWrapper);
        if (!updateScoreResult) {
            log.error("用户: {} 生成图表数量扣除失败", userId);
        }
    }

    @Override
    public synchronized void increaseUserGeneratIngCount(Long userId) {
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper.eq("id", userId);
        userUpdateWrapper.setSql("generatingCount = generatingCount + 1");
        boolean updateScoreResult = this.update(userUpdateWrapper);
        if (!updateScoreResult) {
            log.error("用户: {} 生成图表数量增加失败", userId);
        }
    }

    @Override
    public Boolean canGenerateChart(User user) {
        return user.getGeneratingCount() <= 3;
    }

    @Override
    public Page<User> pageUser(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        Long userId = loginUser.getId();
        String searchParams = userQueryRequest.getSearchParams();
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(StringUtils.isNotEmpty(searchParams), q1 -> q1.like("userName", searchParams)
                .or(StringUtils.isNotEmpty(searchParams), q2 -> q2.like("userRole", searchParams)));
        queryWrapper.ne("id", userId);
        Page<User> userPage = this.page(new Page<>(current, pageSize), queryWrapper);
        return userPage;
    }

    @Override
    public Boolean updateUser(User user, HttpServletRequest request) {
        Long userId = user.getId();
        User loginUser = getLoginUser(request);
        User oldUser = this.getById(userId);
        if (UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "管理员用户不可修改");
        }
        if (!this.isAdmin(request) && !loginUser.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改");
        }
        if (!oldUser.getUserAccount().equals(user.getUserAccount())) {
            String userAccount = user.getUserAccount();
            if (this.count(new QueryWrapper<User>().eq("userAccount", userAccount)) > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户名已存在");
            }
        }
        if (StringUtils.isNotEmpty(user.getUserPassword()) && !oldUser.getUserPassword().equals(user.getUserPassword())) {
            user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + user.getUserPassword()).getBytes()));
        }
        return this.updateById(user);
    }

    @Override
    public boolean addUser(User user) {
        String userAccount = user.getUserAccount();
        String userPassword = user.getUserPassword();
        String userName = user.getUserName();
        String userAvatar = user.getUserAvatar();
        String userRole = user.getUserRole();
        Integer score = user.getScore();
        if (StringUtils.isAnyBlank(userAccount, userPassword, userName, userAvatar, userRole)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (score == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "积分不能为空");
        }
        if (this.count(new QueryWrapper<User>().eq("userAccount", userAccount)) > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户名已存在");
        }
        user.setGeneratingCount(0);
        // 2. 加密
        userPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        user.setUserPassword(userPassword);
        boolean b = this.save(user);
        return b;
    }

    @Override
    public boolean deleteUser(long userId, HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        if (loginUser.getId() == userId) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能删除自己");
        }
        User user = this.getById(userId);
        if (UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能删除管理员");
        }
        boolean b = this.removeById(userId);
        return b;

    }

    @Override
    public synchronized boolean signIn(HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long userId = loginUser.getId();
        // 先判断是否已签到过
        String isSignIn = stringRedisTemplate.opsForValue().get(RedisConstant.USER_SIGN_IN_REDIS_ID + userId);
        if (!StringUtils.isEmpty(isSignIn) && isSignIn.equals(UserConstant.USER_SIGN_IN)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您今天已签到，请明天再来");
        }
        // 计算当前时间至次日零点的秒数
        LocalDateTime currentDatetime = LocalDateTime.now();

        LocalDateTime nextDayMidnight = currentDatetime.plusDays(1).withHour(0).
                withMinute(0).withSecond(0).withNano(0);
        Duration duration = Duration.between(currentDatetime, nextDayMidnight);
        long seconds = Math.abs(duration.getSeconds());
        // 将签到信息存入 Redis
        try {
            stringRedisTemplate.opsForValue().set(RedisConstant.USER_SIGN_IN_REDIS_ID + userId,
                    UserConstant.USER_SIGN_IN, seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.info("用户 {} 签到失败", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "签到失败");
        }
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper.eq("id", userId);
        userUpdateWrapper.setSql("score = score + 20");
        boolean update = this.update(userUpdateWrapper);
        return update;
    }

}
