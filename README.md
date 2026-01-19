# SimpleBlog

Spring Boot 3.5.9 + Vue 3 + GraphQL 的个人博客项目。

## 后端
- 技术：Spring Boot 3.5.9 / GraphQL / MyBatis-Plus / PostgreSQL / Redis
- 入口：`/graphql`
- GraphiQL：`/graphiql`

### 运行
1. 初始化数据库：执行 `doc/schema.sql`
2. 修改配置：`src/main/resources/application.yml`
3. 启动后端：

```bash
./gradlew bootRun
```

## 前端
前端代码位于 `frontend/`，使用 Vite + Vue 3 + TypeScript + Apollo Client。

### 运行
```bash
cd frontend
npm install
npm run dev
```

## 设计文档
- 架构规划：`doc/architecture.md`
- 数据库：`doc/schema.sql`
