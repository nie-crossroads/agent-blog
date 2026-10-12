# agent-blog

个人博客三端。改代码时遵循下列约定。功能与 UI 细节按端阅读 skill，不要把整份约定塞回一个文件。

| 目录 | 技术 | 端口 | Skill |
|------|------|------|--------|
| `BlogService` | Spring Boot 3.3.5 + Java 21 + MyBatis XML | 8080 | `.cursor/skills/blog-backend` |
| `Blog` | Vue3 + TS + Element Plus + Pinia + Router | 5173 | `.cursor/skills/blog-front` |
| `BlogAdmin` | 同上 | 5174 | `.cursor/skills/blog-admin` |

云端库只用现有表和数据；连接信息在 `application-local.yml`，不要提交。后台登录见 `application.yml` 的 `blog.admin.reset-password`。

## 硬约束

- 不要建表或 ALTER 现有库（`blog_email_fail` 是已按用户要求新建的表；`blog_web_update_log.event_date`、`blog_log.consumer` 是已按用户要求新增的列，不要再改其它表）。已有 `blog_web_update_log`、`blog_music` 直接用。
- 只有用户明确要求时才 commit / push。
- Controller / Service / Mapper 用 `@Resource` 字段注入，不要用构造函数。
- 前后台图标用 `@vicons/tabler` 按文件导入，不要用 `@element-plus/icons-vue`。
- 不要用 Druid、APlayer、markdown-it。
- 不要物理删除评论/留言（只改 `visible`）。
- 有 thumbnail 时不要用原图做封面/正文/配图展示。

## Git 提交

commit 信息按实际涉及的类型分行写（中文全角冒号），不涉及的项不要写：

- `feat`：增加功能
- `fix`：bug 修复
- `style`：样式调整
- `perf`：优化代码
- `refactor`：重构
- `docs`：优化文档
- `chore`：日常（升级 SDK、移除废弃代码）

```
- feat：后台仪表盘改为未处理留言/评论数
- fix：恢复访客黑名单与每日 5 条限流
- refactor：MiscService 拆成独立 Service
```

用户要求 commit 且提交成功后：若本次包含 `BlogService/` 下文件，立刻执行 `BlogService/package-linux.cmd`，打出 Linux x86_64 可运行的 `blog-service` jar（关掉 `natives-windows`、启用 `natives-linux`）。产物在 `BlogService/release/`（`blog-service-linux.jar` 为最新一份，另有带上海时区时间戳的一份）。jar 不要提交。**不要**把该步骤加进 `.github/workflows/deploy.yml` 或任何 GitHub Actions。若 git `post-commit` 钩子已经为同一次提交打出 jar，不必再打一遍。

## 启动

- `JAVA_HOME=E:\environment\Java\JDK\jdk21`（系统默认可能是 JDK 8）。
- Maven：`E:\java\maven\apache-maven-3.6.3`，settings 用其 `conf/settings.xml`。
- 后端：`BlogService/start.ps1`。改了 Java / MyBatis XML / `application*.yml` 等必须立刻用它重启，不要假设热更新。
- 后端 `server.servlet.context-path: /agent-blog/server`（`spring.application.name` 不参与路径）。
- 前端 Vite：`host: '0.0.0.0'`。代理 `/api` 与 `/uploads` 的 `target` 必须是 `http://127.0.0.1:8080/agent-blog/server`（不要用 `localhost`）。不要 `rewrite` 去掉 `/api`。
- 后台 Vite `base: '/blog-manager/'`，本地打开 `http://127.0.0.1:5174/blog-manager/`。路由用 `createWebHistory(import.meta.env.BASE_URL)`，401 跳转 `` `${import.meta.env.BASE_URL}login` ``，不要写死 `/login`。
- 安装新 npm 包后必须重启对应 Vite。

CORS：`blog.cors.origins` 是逗号分隔字符串，`WebMvcConfig` 自行 split 后走 `allowedOriginPatterns`（本机可用 `http://localhost:[*]` 匹配任意端口）；`@Value` 绑不上 YAML 列表。

## 返回体（后端）

Controller 返回 `ResponseEntity`，不要自定义 `Result`。成功 HTTP 200，body 即业务数据；失败 HTTP 40X/50X，body 为 `{ code, message }`。异常文案放 `ErrorCode`。
