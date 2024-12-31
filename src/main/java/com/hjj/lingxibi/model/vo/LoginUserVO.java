package com.hjj.lingxibi.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.Setter;

/**
 * 已登录用户视图（脱敏）
 *
 **/
@Data
public class LoginUserVO implements Serializable {

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     *
     */
    private Integer score;

    /**
     * 是否签到
     */
    private boolean isSignIn;

    private Integer generatingCount;

    public void setIsSignIn(boolean isSignIn) {
        this.isSignIn = isSignIn;
    }

    private static final long serialVersionUID = 1L;
}