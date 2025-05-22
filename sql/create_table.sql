-- 创建库
create database if not exists lingxibi;

-- 切换库
use lingxibi;

create table lingxibi.chart
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
    type        tinyint                                null comment '类型（1 - 教育行业 2 - 汽车行业）',
    allowModify tinyint      default 0                 null comment '是否允许他人修改',
    createTime  datetime     default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime  datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint      default 0                 null comment '是否删除（0-不删除 1-删除）'
)
    comment '图表信息表';

create index chart_userId_index
    on lingxibi.chart (userId)
    comment '图表-用户id-索引';

create table lingxibi.chart_history
(
    id             bigint auto_increment comment 'id'
        primary key,
    name           varchar(128)                           null comment '图标名称',
    goal           text                                   null comment '分析目标',
    chartData      text                                   null comment '图表数据',
    chartType      varchar(128)                           null comment '图表类型',
    genChart       text                                   null comment 'AI生成的图表信息',
    genResult      text                                   null comment 'AI生成的分析结果',
    status         varchar(128) default 'wait'            not null comment '图表的状态（wait,succeed,failed,running）',
    execMessage    text                                   null comment '执行信息',
    userId         bigint                                 null comment '创建的用户Id',
    relatedChartId bigint                                 null comment '关联的图表id',
    allowModify    tinyint      default 1                 null comment '是否允许他人修改（0 - 不允许 1 - 允许）',
    type           tinyint                                null comment '类型（1 - 教育行业 2 - 汽车行业）',
    createTime     datetime     default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime     datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint      default 0                 null comment '是否删除（0-不删除 1-删除）'
)
    comment '图表信息表';

create index chart_history_relatedChartId_index
    on lingxibi.chart_history (relatedChartId)
    comment '图表历史-关联图表id-索引';

create index chart_userId_index
    on lingxibi.chart_history (userId)
    comment '图表-用户id-索引';

create table lingxibi.message
(
    id             bigint auto_increment comment 'id'
        primary key,
    content        varchar(1024)                      null comment '消息内容',
    chartHistoryId bigint                             null comment '关联的图表历史id',
    fromId         bigint                             null comment '发送消息的用户id',
    toId           bigint                             null comment '接收消息的用户id',
    createTime     datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime     datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isRead         tinyint  default 0                 null comment '是否已（0 - 未读 1 - 已读）'
)
    comment '消息表';

create index message_fromId_index
    on lingxibi.message (fromId)
    comment '消息-发送消息的用户id-索引';

create index message_toId_index
    on lingxibi.message (toId)
    comment '消息-接收消息的用户id-索引';

create table lingxibi.team
(
    id          bigint auto_increment comment 'id'
        primary key,
    name        varchar(256)                       null comment '队伍名称',
    imgUrl      varchar(512)                       null comment '队伍图片',
    userId      bigint                             null comment '队长id',
    description varchar(128)                       null comment '队伍描述',
    maxNum      int                                null comment '最大人数',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
)
    comment '队伍';

create table lingxibi.team_chart
(
    id         bigint auto_increment comment 'id'
        primary key,
    teamId     bigint                             null comment '队伍id',
    chartId    bigint                             null comment '图表id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '队伍图表关系表';

create table lingxibi.team_user
(
    id         bigint auto_increment comment 'id'
        primary key,
    teamId     bigint                             null comment '队伍id',
    userId     bigint                             null comment '用户id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '队伍用户关系表';

create table lingxibi.user
(
    id              bigint auto_increment comment 'id'
        primary key,
    userAccount     varchar(256)                                                                                                           not null comment '账号',
    userPassword    varchar(512)                                                                                                           not null comment '密码',
    userName        varchar(256)  default '游客'                                                                                           not null comment '用户昵称',
    userAvatar      varchar(1024) default 'https://th.bing.com/th/id/R.54a295a86f04aaf12f1285d4e00fd6be?rik=QAdEADu3LNh9Hg&pid=ImgRaw&r=0' null comment '用户头像',
    userRole        varchar(256)  default 'user'                                                                                           not null comment '用户角色：user/admin/ban',
    score           int           default 0                                                                                                not null comment '积分',
    createTime      datetime      default CURRENT_TIMESTAMP                                                                                not null comment '创建时间',
    updateTime      datetime      default CURRENT_TIMESTAMP                                                                                not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint       default 0                                                                                                not null comment '是否删除'
)
    comment '用户' collate = utf8mb4_unicode_ci;

create index idx_userAccount
    on lingxibi.user (userAccount);

