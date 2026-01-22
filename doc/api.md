# SimpleBlog GraphQL API 文档

本文档面向前端与接口调用，描述 GraphQL 入口、鉴权方式、数据模型、Query/Mutation 示例与错误处理规范。

## 1. 基本信息
- GraphQL 入口：`http://localhost:8888/graphql`
- GraphiQL：`http://localhost:8888/graphiql`
- 认证方式：JWT（Header: `Authorization: Bearer <token>`）
- 编码：UTF-8

## 2. 鉴权与权限
- 登录成功后，返回 `token`。
- 后续请求需带 `Authorization: Bearer <token>`。
- 权限模型（RBAC）：
  - `admin`：全部权限
  - `user`：文章作者权限（创建/编辑）
  - `visitor`：登录用户可评论
  - 未登录：仅可查询公开数据

### 2.1 需要登录的操作
- `createComment`

### 2.2 需要角色权限的操作
- `createBlog` / `updateBlog`：`admin` 或 `user`
- `deleteBlog` / `createCategory` / `createTag`：`admin`

## 3. 数据类型（Schema）
以下为核心类型摘要，完整结构以 `schema.graphqls` 为准。

### 3.1 标量
- `DateTime`：ISO-8601 时间
- `Long`：64 位整数

### 3.2 核心类型
- `User`：用户信息
- `Blog`：文章
- `Category`：分类
- `Tag`：标签
- `Comment`：评论
- `BlogPage`：分页

## 4. Query

### 4.1 获取当前用户
```graphql
query {
  me {
    id
    username
    displayName
  }
}
```

### 4.2 获取文章列表
```graphql
query Blogs($page: Int!, $size: Int!, $publishedOnly: Boolean) {
  blogs(page: $page, size: $size, publishedOnly: $publishedOnly) {
    items {
      id
      title
      summary
      category { id name }
    }
    total
    page
    size
  }
}
```

变量示例：
```json
{ "page": 1, "size": 10, "publishedOnly": true }
```

### 4.3 获取文章详情
```graphql
query Blog($id: ID!) {
  blog(id: $id) {
    id
    title
    content
    category { id name }
    tags { id name }
    author { id username displayName }
  }
}
```

### 4.4 获取分类/标签
```graphql
query {
  categories { id name slug }
  tags { id name slug }
}
```

### 4.5 获取评论
```graphql
query Comments($blogId: ID!) {
  comments(blogId: $blogId) {
    id
    content
    user { id username displayName }
    createdAt
  }
}
```

## 5. Mutation

### 5.1 登录
```graphql
mutation Login($input: LoginInput!) {
  login(input: $input) {
    token
    user { id username displayName }
  }
}
```

变量示例：
```json
{ "input": { "username": "admin", "password": "admin123" } }
```

### 5.2 创建文章（admin/user）
```graphql
mutation CreateBlog($input: BlogInput!) {
  createBlog(input: $input) {
    id
    title
    published
  }
}
```

变量示例：
```json
{
  "input": {
    "title": "第一篇文章",
    "summary": "摘要",
    "content": "# Hello",
    "categoryId": 1,
    "coverUrl": "https://example.com/cover.png",
    "published": true,
    "tagIds": [1, 2]
  }
}
```

### 5.3 更新文章（admin/user）
```graphql
mutation UpdateBlog($id: ID!, $input: BlogInput!) {
  updateBlog(id: $id, input: $input) {
    id
    title
    published
  }
}
```

### 5.4 删除文章（admin）
```graphql
mutation DeleteBlog($id: ID!) {
  deleteBlog(id: $id)
}
```

### 5.5 创建评论（登录用户）
```graphql
mutation CreateComment($input: CommentInput!) {
  createComment(input: $input) {
    id
    content
    createdAt
  }
}
```

变量示例：
```json
{ "input": { "blogId": 1, "parentId": null, "content": "写得不错" } }
```

### 5.6 创建分类（admin）
```graphql
mutation CreateCategory($name: String!, $slug: String!) {
  createCategory(name: $name, slug: $slug) {
    id
    name
    slug
  }
}
```

### 5.7 创建标签（admin）
```graphql
mutation CreateTag($name: String!, $slug: String!) {
  createTag(name: $name, slug: $slug) {
    id
    name
    slug
  }
}
```

## 6. 错误处理
- GraphQL 标准错误响应结构：
  - `errors[].message`：错误信息
- 业务层常见错误：
  - `用户名或密码错误`
  - `未登录`
  - `文章不存在`
- 权限不足时会被 Spring Security 拦截并返回 401/403。

## 7. 调用示例（curl）

### 7.1 登录
```bash
curl -X POST http://localhost:8888/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation($input: LoginInput!){ login(input:$input){ token } }","variables":{"input":{"username":"admin","password":"admin123"}}}'
```

### 7.2 带 token 查询
```bash
curl -X POST http://localhost:8888/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"query":"query($page:Int!,$size:Int!){ blogs(page:$page,size:$size){ total } }","variables":{"page":1,"size":10}}'
```
