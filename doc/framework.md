# 一、摘要
用Springboot3.5.9 + Vue3做一个个人博客网站。
采用GraphQL作为Api框架
采用单体架构写，简单一点。

# 二、后端技术栈（Spring Boot 3.5.9）
## 1.基础框架

|技术|说明|
|---|---|
|**Spring Boot 3.5.9**|核心框架（Jakarta EE）|
|Spring Web MVC|REST API|
|Spring Validation|参数校验|
|Spring AOP|日志 / 权限|


## 2. 安全 & 登录体系（重点）
**JWT + Spring Security**

|技术|用途|
|---|---|
|Spring Security 6|权限框架|
|JWT（jjwt / nimbus）|无状态登录|
|BCrypt|密码加密|
|RBAC|用户-角色-权限|

**权限模型示例：**
```
用户 → 角色（admin > user > visitor > 未登入）
角色 → 权限（admin即超级管理；user是文章作者，可以创建文章，编辑/删除自己的文章，visitor是访客用户，可以评论；未登入的用户只能浏览文章）
```

## 3️.数据层

| 技术               | 用途            |
| ---------------- | ------------- |
| **MyBatis-Plus** | ORM           |
| PostgreSQL       | 主数据库          |

## 4️. 缓存 & 性能

| 技术           | 用途        |
| ------------ | --------- |
| Redis        | 缓存、验证码、限流 |
| Spring Cache | 缓存抽象      |

**典型缓存场景**
- 首页文章列表
- 热门文章
- 阅读量 / 点赞数
- 登录验证码

## 5️.文件&图片&视频

| 技术            | 用途   |
| ------------- | ---- |
| RustFS        | 对象存储 |
| Thumbnailator | 图片压缩 |

## 6️.日志 & 监控

| 技术                   | 用途   |
| -------------------- | ---- |
| Sl4j                 | 日志   |
| Spring Boot Actuator | 健康检查 |


# 三、前端技术栈（Vue 3）

## 1️.核心框架

|技术|用途|
|---|---|
|**Vue 3 + Vite**|核心|
|TypeScript|强类型|
|Vue Router|路由|
|Pinia|状态管理|

## 2️. UI 框架（推荐）
 **个人博客最推荐：**

| 框架               | 场景       |
| ---------------- | -------- |
| **Element Plus** | 后台管理     |
| Naive UI         | 更现代（可选）  |
| Tailwind CSS     | 博客前台（可选） |

> **前台博客：Tailwind + 自定义**  
> **后台管理：Element Plus**

## 3️.网络 & 工具

|技术|用途|
|---|---|
|Axios|HTTP|
|Markdown-it / Vditor|Markdown 渲染|
|highlight.js|代码高亮|
|Day.js|时间处理|
# 四、中间件 & 基础设施

| 中间件            | 作用           |
| -------------- | ------------ |
| **Nginx**      | 反向代理 / HTTPS |
| Redis          | 缓存           |
| RustFS         | 文件           |
| Docker         | 一键部署         |
| Docker Compose | 本地环境         |

# 五、核心功能模块设计

## 博客前台
- 首页文章列表
- 分类 / 标签
- 文章详情（Markdown）
- 阅读量统计
- 评论系统
- 搜索
##  后台管理
- 登录 / 权限
- 文章管理
	- 创建文章，支持markdown编辑器，文章都是用Markdown格式写的
	- 编辑文章
	- 删除文章
- 分类 / 标签
- 评论管理
- 用户管理
- 文件管理
- 系统配置
## 数据模型示例
```
User
Role
Permission
Blog
Category
Tag
BlogTag
Comment
File
Config
```
# 七、进阶可选（加分项）
**后续可以加的**
- Elasticsearch（文章搜索）
- WebSocket（评论实时刷新）
- GraphQL（你之前问过，很适合博客）
- 定时任务（文章定时发布）
- API 限流（Redis + Lua）
