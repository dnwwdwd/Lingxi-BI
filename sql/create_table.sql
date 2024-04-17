-- 创建库
create database if not exists lingxibi;

-- 切换库
use lingxibi;

-- 用户表
create table lingxibi.user
(
    id           bigint auto_increment comment 'id'
        primary key,
    userAccount  varchar(256)                                                               not null comment '账号',
    userPassword varchar(512)                                                               not null comment '密码',
    userName     varchar(256)                                                               null comment '用户昵称',
    userAvatar   varchar(1024) default 'https://p.qqan.com/up/2021-1/16101620383324674.jpg' null comment '用户头像',
    userRole     varchar(256)  default 'user'                                               not null comment '用户角色：user/admin/ban',
    createTime   datetime      default CURRENT_TIMESTAMP                                    not null comment '创建时间',
    updateTime   datetime      default CURRENT_TIMESTAMP                                    not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint       default 0                                                    not null comment '是否删除'
)
    comment '用户' collate = utf8mb4_unicode_ci;

create index idx_userAccount
    on lingxibi.user (userAccount);


-- 图表信息表
create table chart
(
    id          bigint auto_increment comment 'id'
        primary key,
    name        varchar(128)                           null comment '图标名称',
    goal        text                                   null comment '分析目标',
    chartData   text                                   null comment '图表数据',
    chartType   varchar(128)                           null comment '图表类型',
    genChart    text                                   null comment 'AI生成的图表信息',
    genResult   text                                   null comment 'AI生成的分析结果',
    status      varchar(128) default 'wait'            not null comment '图表的状态（wait,succeed,failed,running）',
    execMessage text                                   null comment '执行信息',
    userId      bigint                                 null comment '创建的用户Id',
    createTime  datetime     default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime  datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint      default 0                 null comment '是否删除（0-不删除 1-删除）'
)
    comment '图表信息表';



