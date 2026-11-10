# 初始化与部署

本目录集中存放**从零跑起本项目所需的一切**：数据库脚本、Nacos 配置模板、中间件编排、本地调试与服务器部署说明。

## 目录内容

| 文件 | 用途 |
| --- | --- |
| [`README.md`](README.md) | 本文件：初始化总流程与检查清单 |
| [`数据库.md`](数据库.md) | 数据库结构说明、初始化脚本、迁移脚本与备份恢复 |
| [`sql/`](sql/) | 全部 SQL：初始化脚本、可选迁移脚本、开发辅助脚本 |
| [`nacos-template.yml`](nacos-template.yml) | Nacos 配置模板（dataId `application-cloud.yml`） |
| [`bootstrap.properties.template`](bootstrap.properties.template) | 后端 Nacos 引导配置模板 |
| [`docker-compose.yml`](docker-compose.yml) | 本地/单机中间件编排（MySQL、Redis、RabbitMQ、Nacos、ES、Maxwell、MinIO） |
| [`.env.example`](.env.example) | compose 使用的环境变量示例 |
| [`本地调试.md`](本地调试.md) | 本地启动、接口冒烟、Maxwell→ES 链路、常见问题 |
| [`服务器部署.md`](服务器部署.md) | 生产服务器安装、镜像构建、nginx 配置与 CI 说明 |
| [`镜像拉取与整理.md`](镜像拉取与整理.md) | 全部镜像清单、拉取/离线搬迁/加速与版本固化 |

## 初始化流程

### 1. 准备环境

| 依赖 | 版本 |
| --- | --- |
| JDK | 25 |
| Maven | 3.9+ |
| Node.js | 24 |
| Docker | 20+（含 compose 插件） |

### 2. 启动中间件

```bash
cd setup
cp .env.example .env      # 然后编辑 .env，把所有 CHANGEME 换成自己的值
docker compose up -d
docker compose ps         # 7 个容器全部 Up
```

> 没有使用 compose 时，可按 [`本地调试.md`](本地调试.md#一启动中间件) 中的 `docker run`
> 命令逐个启动；端口必须与下表一致。

| 容器 | 宿主端口 | 说明 |
| --- | --- | --- |
| `aurora-mysql` | 3306 | 库名 `aurora` |
| `aurora-redis` | 6379 | 应用使用 DB 1 |
| `aurora-rabbitmq` | 5672 / 15672 | 管理台 15672 |
| `aurora-nacos` | 8300→8848、9300→9848、控制台 8880 | 端口平移原因见下文 |
| `aurora-es` | 9200 | 搜索策略为 `mysql` 时可不启动 |
| `aurora-maxwell` | — | MySQL → ES 增量同步 |
| `aurora-minio` | 9000 / 9001 | 对象存储 |

> **Nacos 端口平移**：编排把 HTTP 映射为宿主 `8300`、gRPC 映射为 `9300`、控制台映射为 `8880`
> （容器内仍为 8848/9848/8080）。**gRPC 宿主端口必须等于「server-addr 端口 + 客户端偏移」**：
> 默认偏移 1000 正好落在 9300（Nacos gRPC）。因此 ES 传输端口在宿主机改映射为 `9301`，
> 后端**无需**额外参数（保持默认）。详见 [`服务器部署.md`](服务器部署.md)。

### 3. 初始化数据库

详见 [`数据库.md`](数据库.md)。**全新环境**最简流程：

```bash
# compose 首次启动时已自动导入 sql/aurora-init.sql；手动导入（在仓库根执行）：
docker exec -i aurora-mysql mysql -uroot -p'<数据库口令>' < setup/sql/aurora-init.sql
```

**从旧版 Aurora 升级**（已有 24 张表、无 `t_ai_persona`）不要导入上面的初始化脚本，改执行增量包
[`sql/upgrade-from-old/`](sql/upgrade-from-old/)：直接执行打包好的 `00-upgrade-all.sql`（已内联 01/01b/02~05），
执行后重启后端。

初始化脚本包含 25 张表、基础配置、资源鉴权登记与初始管理员账号：

```
用户名：admin@163.com
密码：123456
```

**首次登录后立即修改密码。**

### 4. 配置 Nacos

1. 打开 Nacos 控制台（默认 <http://localhost:8880/nacos>），用 `.env` 中配置的账号登录。
2. 新建配置：dataId `application-cloud.yml`、group `DEFAULT_GROUP`、格式 YAML。
3. 复制 [`nacos-template.yml`](nacos-template.yml) 的内容，替换其中所有 `CHANGEME`（数据库/Redis/MinIO 口令、JWT 密钥、AI Key 等）后发布。
4. 在仓库根目录执行下面的复制命令，然后编辑生成的 `bootstrap.properties`，填写你的命名空间 ID 与 Nacos 地址：

```bash
cp setup/bootstrap.properties.template aurora-xinchen/src/main/resources/bootstrap.properties
```

> Nacos 口令通过环境变量注入，不写入仓库：
> `export NACOS_PASSWORD='<你的Nacos口令>'`（PowerShell：`$env:NACOS_PASSWORD = '<口令>'`）。

### 5. 启动后端与前端

以下命令均在**仓库根目录**执行：

```bash
cd aurora-xinchen
mvn -B -ntp clean package -DskipTests
NACOS_PASSWORD='<Nacos口令>' java -jar target/aurora-xinchen-0.0.1.jar
```

```bash
cd aurora-blog  && npm ci && npm run serve -- --port 8081
cd aurora-admin && npm ci && npm run serve -- --port 8082
```

后端 `8080`，前台 `8081`，后台 `8082`；前端 `/api` 代理到 `http://localhost:8080`。

## 上线前检查清单

- [ ] `.env` 中所有口令已替换为强口令，且 `setup/.env` 未被提交
- [ ] Nacos 配置中的 `auth.secret`（JWT 密钥）、数据库/Redis/MinIO 口令、AI Key 已替换
- [ ] `knife4j.production` 设为 `true`（关闭生产接口文档）
- [ ] 初始化管理员密码已修改
- [ ] `aurora-blog/src/config/config.ts` 与 `aurora-blog/public/index.html` 中的 QQ AppID/回调域名已改为自己的
- [ ] 生产环境启用 HTTPS 后，保留或移除 `index.html` 中的 `upgrade-insecure-requests` 视情况决定
- [ ] 数据库已做首次备份
