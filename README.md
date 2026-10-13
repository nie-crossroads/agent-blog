# 个人博客（三端）

同级目录：

| 目录 | 说明 | 本地地址 |
|------|------|----------|
| `BlogService` | Spring Boot 3.3.5 + Java 21 + MyBatis + MySQL8 | http://127.0.0.1:8080/agent-blog/server |
| `Blog` | Vue3 前台 | http://localhost:5173 |
| `BlogAdmin` | Vue3 后台 | http://localhost:5174/blog-manager/ |

使用已有 MySQL 库（默认 `blogtest`），密钥只写 `application-local.yml`，不要提交。

## 功能概览

**前台**

- 文章列表 / 详情、分类、标签、归档、动态瀑布流、友链、留言板、关于、网站日志
- 文章与留言评论：审核通过后才显示；勾选「邮件回复」后，被回复方可收到邮件
- 昵称填 QQ 号可回填昵称、头像、邮箱（UAPI）
- 侧栏原生音乐播放器（循环 / 单曲 / 随机）
- 日夜间主题与主题色

**后台**（`admin`，密码见 local 配置）

- 文章、分类、标签、评论；友链、留言；账号、动态、记录
- 功能日志、音乐、运行日志、失败邮件（可重发）
- 未填文章简介时，保存会去掉 Markdown 后取正文开头（最多 80 字）
- 评论 / 留言：处理、通过 / 不通过、博主回复；回复走异步发信，页面不等 SMTP

**后端**

- JWT 登录；对象存储 `local` / `cos` / `qiniu`
- 评论留言邮件：线程池异步发送；失败写入 `blog_email_fail`，后台可重发
- 三方 HTTP（QQ 资料、IP 归属）走 `RestTemplate`，URL 在 `application.yml`，密钥在 local
- 上传图会生成 avif 缩略图（GIF 除外）


## 三方平台
| 平台 | 用途 |
|------|------|
| [UApiPro](https://uapis.cn/docs/api-reference/get-social-qq-userinfo?qq=2393523153) | 查询QQ信息 |
| [腾讯位置服务](https://lbs.qq.com/service/webService/webServiceGuide/position/webServiceIp) | 查询ip位置信息 |
| [腾讯邮件服务](https://mail.qq.com/) | 邮件发送 |

## 启动后端

必须使用 JDK 21（系统默认若是 JDK 8 则无法启动 Spring Boot 3）。

修改`application.yml`中`blog.admin.reset-password`的值，该配置项登录后台管理的密码，然后执行如下操作

```powershell
cd BlogService
.\start.ps1
```

或：

```powershell
$env:JAVA_HOME="E:\environment\Java\JDK\jdk21"
$env:Path="$env:JAVA_HOME\bin;" + $env:Path
& "E:\java\maven\apache-maven-3.6.3\bin\mvn.cmd" -s "E:\java\maven\apache-maven-3.6.3\conf\settings.xml" spring-boot:run
```

改了 Java / MyBatis XML / `application*.yml` 后需要重新执行 `start.ps1`，没有热更新。

接口带 context-path，例如：

- http://127.0.0.1:8080/agent-blog/server/api/front/articles

## 启动前台 / 后台

```powershell
cd Blog
npm install
npm run dev
```

```powershell
cd BlogAdmin
npm install
npm run dev
```

Vite 把 `/api`、`/uploads` 代理到 `http://127.0.0.1:8080/agent-blog/server`（不要用 `localhost` 作代理 target）。

## 本地配置

复制 `BlogService/src/main/resources/application-local.yml.example` 为 `application-local.yml`（已 gitignore），填写：

- 数据库连接
- JWT、`blog.admin.reset-password`（需要重置 admin 密码时填写后重启）
- `spring.mail.username` / `password`（QQ 邮箱 SMTP 授权码）
- 对象存储密钥（`blog.storage.type`：`local` / `cos` / `qiniu`）
- `blog.map.qq-key`、`blog.uapi.qq-info-key`
- `blog.site.title`、`msg-avatar`、`site-url`

`application.yml` 里是非密钥项（含 QQ / 地图请求 URL）。改存储类型或邮件配置后必须重启 Java。

自动部署（push `main` → 只发前台/后台静态资源）见 [deploy/README.md](deploy/README.md)。
