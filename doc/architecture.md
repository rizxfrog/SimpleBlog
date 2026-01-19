# 系统架构规划

## 架构总览
- 架构风格：单体应用（Spring Boot）+ 前后端分离（Vue 3）
- API 规范：GraphQL（单一入口）
- 鉴权方式：JWT + Spring Security（RBAC）
- 数据库：PostgreSQL
- 缓存：Redis（缓存与后续限流/验证码）
- 文件存储：RustFS（预留接口）

## 模块划分
- `auth`：登录、JWT 签发、权限校验
- `blog`：文章、分类、标签、评论、统计
- `system`：配置、文件、基础设置
- `frontend`：博客前台 + 后台管理

## 关键流程
1. 登录
   - 前端调用 `login` mutation
   - 后端校验用户名/密码，签发 JWT
   - 前端保存 token 并在请求头带上 `Authorization: Bearer <token>`

2. 文章读取
   - 前端调用 `blogs` / `blog` queries
   - 后端返回文章详情及关联信息（作者、分类、标签）

3. 文章管理
   - 后台通过 `createBlog` / `updateBlog` / `deleteBlog` mutations
   - RBAC 控制：admin/user 允许写入

## 目录结构
- `src/main/java`：后端代码
- `src/main/resources/graphql`：GraphQL Schema
- `doc/schema.sql`：数据库建表脚本
- `frontend/`：前端 Vue 3 项目
