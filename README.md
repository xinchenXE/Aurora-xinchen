# Aurora Xinchen · 前后端分离博客

基于 [Aurora](https://github.com/auroral-ui/hexo-theme-aurora) 主题风格改造的个人博客系统，前后台分离：

- **前台**：Vue 3 + TypeScript，文章 / 说说 / 归档 / 标签 / 留言 / 友链 / 相册 / AI 音乐等页面，附带 Live2D 看板娘与 AI 对话助手
- **后台**：Vue 2 + Element UI，文章、分类、标签、评论、用户、角色、菜单、资源、日志、定时任务、AI 人设等管理
- **后端**：Spring Boot 3.5 + Java 25 + MyBatis-Plus + Spring Security 6，配置托管在 Nacos

本仓库为原作者仓库的深度改造分支，在原有功能之上完成了 Java 25 / Spring Boot 3 整体迁移、Nacos 3 配置中心、玻璃拟态 UI 升级、Live2D 本地化与 Spring AI 对话模块。

线上效果：<https://nahida.pink/>

## 目录结构

| 目录 | 说明 |
| --- | --- |
| [`aurora-xinchen/`](aurora-xinchen/) | 后端服务，主类 `com.aurora.AuroraXinchenApplication`，产物 `target/aurora-xinchen-0.0.1.jar` |
| [`aurora-blog/`](aurora-blog/) | 前台站点（Vue 3 + TS），dev 端口 8081 |
| [`aurora-admin/`](aurora-admin/) | 后台管理端（Vue 2 + Element UI），dev 端口 8082 |
| [`setup/`](setup/) | **初始化与部署**：数据库脚本、Nacos 配置模板、docker compose、本地调试与服务器部署文档 |
| [`Dockerfile.frontend`](Dockerfile.frontend)、[`nginx.frontend.conf`](nginx.frontend.conf) | 前端统一镜像：blog 挂 `/`，admin 挂 `/admin` |
| [`.github/workflows/aurora.yml`](.github/workflows/aurora.yml) | CI：构建后端与前端两个镜像并推送 |

## 功能概览

**前台**

- 文章列表 / 详情、置顶与推荐、标签筛选、归档、全文搜索（Elasticsearch / MySQL 双策略）
- 说说、留言板、友链、相册、AI 翻唱音乐页
- 评论（含邮件 / 订阅通知）、QQ 登录、用户中心
- 中英文双语（vue-i18n）、明暗主题切换、渐变配色与玻璃拟态界面
- AI 对话助手（流式输出）、Live2D 看板娘（子集本地化，不依赖外域资源）

**后台**

- 文章、分类、标签、说说、评论、相册、友链管理
- 用户、角色、菜单、资源（`t_resource` / `t_role_resource` 细粒度路由鉴权）
- 操作日志、异常日志、定时任务与任务日志
- 站点配置、AI 人设设置

## 技术栈

### 后端

| 类别 | 选型 |
| --- | --- |
| 运行环境 | Java 25（Temurin）、Spring Boot 3.5.16 |
| 配置 / 注册 | Spring Cloud Alibaba + Nacos 3.2.3（配置中心，可选服务发现） |
| ORM | MyBatis-Plus 3.5.17 |
| 鉴权 | Spring Security 6 + JWT（jjwt 0.13） |
| 数据 / 缓存 | MySQL 8、Redis（应用使用 DB 1）、Druid 连接池 |
| 搜索 | Elasticsearch 7.17（经 Maxwell 同步）或 MySQL 内置搜索 |
| 消息 | RabbitMQ（评论通知、订阅通知） |
| 对象存储 | MinIO / 阿里云 OSS（可切换） |
| 定时任务 | Quartz |
| AI | Spring AI 1.1（OpenAI 兼容协议，可对接 OpenAI / DeepSeek / Kimi / 智谱 / Ollama 等） |
| API 文档 | knife4j 4.5（springdoc OpenAPI 3） |

### 前台 / 后台

| 侧 | 选型 |
| --- | --- |
| `aurora-blog` | Vue 3、TypeScript 4.5（`strict`）、vue-cli 5、pinia、vue-router 4、vue-i18n、Tailwind（postcss7-compat）、markdown-it + KaTeX + Prism |
| `aurora-admin` | Vue 2、vuex、vue-router 3、Element UI 2、mavon-editor、ECharts |

## 快速开始

完整步骤见 [`setup/README.md`](setup/README.md)。最短路径：

```bash
# 1. 启动中间件（MySQL / Redis / RabbitMQ / Nacos / ES / Maxwell / MinIO）
cd setup
cp .env.example .env        # 替换其中所有 CHANGEME
docker compose up -d

# 2. 初始化数据库（compose 首次启动会自动导入 setup/sql/aurora-init.sql）
docker exec -i aurora-mysql mysql -uroot -p'<数据库口令>' < setup/sql/aurora-init.sql

# 3. 在 Nacos 控制台（默认 http://localhost:8880/nacos）新建 application-cloud.yml，
#    内容参照 setup/nacos-template.yml；并把 setup/bootstrap.properties.template
#    复制为 aurora-xinchen/src/main/resources/bootstrap.properties 后填写命名空间与地址。

# 4. 启动后端
cd aurora-xinchen
mvn -B -ntp clean package -DskipTests
NACOS_PASSWORD='<Nacos口令>' java -jar target/aurora-xinchen-0.0.1.jar

# 5. 启动前台 / 后台
cd aurora-blog  && npm ci && npm run serve -- --port 8081
cd aurora-admin && npm ci && npm run serve -- --port 8082
```

访问：前台 <http://localhost:8081>，后台 <http://localhost:8082>，后端 <http://localhost:8080>，
接口文档（knife4j）<http://localhost:8080/doc.html>。

初始化后的管理员账号为 `admin@163.com` / `123456`，**首次登录后请立即修改密码**。

## 初始化与部署文档

| 文档 | 内容 |
| --- | --- |
| [`setup/README.md`](setup/README.md) | 初始化总流程与上线前检查清单 |
| [`setup/数据库.md`](setup/数据库.md) | 数据库结构、初始化脚本、迁移与开发脚本、备份恢复 |
| [`setup/sql/`](setup/sql/) | 初始化 SQL 与可选脚本 |
| [`setup/nacos-template.yml`](setup/nacos-template.yml) | Nacos 配置模板（dataId `application-cloud.yml`） |
| [`setup/docker-compose.yml`](setup/docker-compose.yml) | 本地/单机中间件编排 |
| [`setup/本地调试.md`](setup/本地调试.md) | 本地启动、接口冒烟、Maxwell→ES 链路、常见问题 |
| [`setup/服务器部署.md`](setup/服务器部署.md) | 服务器安装、镜像构建、nginx 与 CI 配置 |

## AI 对话（可选）

在 Nacos 配置中填写 `spring.ai.openai.base-url`、`api-key`、`chat.options.model` 即可接入任意
OpenAI 兼容服务，无需改代码。`/ai/chat`、`/ai/chat/stream` 需登录；可选的请求级模型覆盖由
`aurora.ai.chat.allowed-models` 白名单控制（仅 admin 角色可用，空列表即禁用）。

## 升级记录（Java 8 → Java 25）

本项目原基于 Java 8 + Spring Boot 2.3.7，已于 2026 年 8 月完成整体升级：

| 项 | 升级前 | 升级后 |
| --- | --- | --- |
| JDK | 1.8.0_202 | Java 25（Temurin） |
| Spring Boot | 2.3.7.RELEASE | 3.5.16 |
| 命名空间 | `javax.*` | `jakarta.*` |
| API 文档 | springfox（knife4j 2.0.7） | knife4j-openapi3（springdoc）4.5.0 |
| JWT | jjwt 0.9.0 | jjwt 0.13.0（api/impl/jackson） |
| Docker 基础镜像 | `openjdk:8` | `eclipse-temurin:25-jre` |
| CI 构建 | JDK 8 | Temurin 25 |
| Nacos | 2.x | 3.2.3 |

> 升级前的 Java 8 基线与相关依赖快照已清理，不再参与构建与运行。

## 鸣谢

- 原始项目：[linhaojun857/aurora](https://github.com/linhaojun857/aurora)（SpringBoot + Vue 个人博客系统）
- 轻量版后端：[zhouyqxy/aurora_Lite](https://github.com/zhouyqxy/aurora_Lite)（上游作者的社区轻量分支）
- 主题风格：[hexo-theme-aurora](https://github.com/auroral-ui/hexo-theme-aurora)
- 感谢 [JetBrains](https://www.jetbrains.com/) 提供的开源开发许可证

## 许可证

[Apache License 2.0](LICENSE)
