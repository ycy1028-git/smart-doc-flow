# SmartDoc-Flow

`SmartDoc-Flow` 是一个轻量型文档解析工具，面向本地运行、二次集成和快速验证场景。

它的目标是把常见文档尽可能稳定地转换为结构化、可消费的 `Markdown` 和 `JSON`，而不是做一个重型文档处理平台。

当前仓库主要提供：

1. 开源解析代码
2. CLI 启动方式
3. 轻量 Demo Web
4. 基础运行配置

## Demo 演示地址

在线 Demo：

```text
https://demo.yyapi.cc
```

## 开源版定位

开源版聚焦“基础支持”，强调：

1. 本地可跑
2. 可嵌入 Java 项目
3. 可直接输出 `Markdown` 和 `JSON`
4. 适合做主链路验证和基础集成

对于 RAG / 向量化场景，当前开源版也适合作为前置文档解析层使用：

1. 负责把上传文档解析为结构化 block 数据
2. 为外部 chunking / embedding 流程提供统一输入
3. 不在当前仓库内内建切块、向量入库和召回链路

## RAG 接入建议

如果你是把 `SmartDoc-Flow` 接到 RAG / 向量化链路里，推荐按下面方式使用：

1. 先上传文档并完成解析
2. 优先消费结构化 block 结果，而不是直接基于整篇 `Markdown` 切块
3. 在外部系统中按标题、页码、块类型或相邻 block 自定义切块策略
4. 将切块结果与 `fileName`、`sourceType`、`page`、`order` 等元信息一起写入向量库

当前仓库负责把文档转换为后续可切块、可向量化的基础数据，不负责内建 embedding、向量索引和召回排序。

当前适合优先验证的文档类型包括：

1. 数字原生 PDF
2. 扫描 PDF 基础 OCR 路径
3. 图片文档，如 `PNG`、`JPG`、`TIFF`
4. `DOCX` 基础解析
5. `XLSX` 基础表格提取
6. `PPTX` 基础解析

开源版当前重点保证：

1. 基础阅读顺序可用
2. 标题、正文、列表等基础结构可用
3. 表格至少能输出基本可消费结构
4. `Markdown` 和 `JSON` 输出稳定

对当前开源版的 Office 支持范围，建议理解为“基础解析”，当前适合承诺的范围包括：

1. `DOCX` 的标题、正文和基础表格文本提取
2. `XLSX` 的 sheet 级基础表格文本提取
3. `PPTX` 的 slide 级标题、文本框、基础表格、图片和备注提取
4. 输出基础可消费的 `Markdown` 和 `JSON`

当前不建议对开源版承诺的 Office 能力包括：

1. 复杂版式的高保真还原
2. SmartArt 语义重建
3. 复杂图表结构恢复
4. 动画和出现顺序语义
5. 老 `PPT` 格式的高质量解析

## 企业服务

如果你需要更强的复杂文档解析能力，也可以基于同一解析内核提供企业级增强服务，重点包括：

1. 复杂财务报表恢复
2. 双栏论文和技术手册处理
3. 低质量扫描件增强
4. 行业专项规则包
5. 私有化部署支持
6. Docker 化交付
7. 付费支持与专项优化

这个仓库聚焦开源代码、启动脚本和基础配置。企业增强能力、专项适配和交付服务可按实际场景单独提供。

## 开源版与企业服务边界

建议按下面原则理解这个仓库的边界：

1. 开源版提供基础支持，目标是让用户可以本地运行、集成验证和二次开发
2. 企业服务提供增强支持，目标是提升复杂文档场景下的精度、稳定性和交付效率
3. 开源版和企业增强能力共享同一解析内核，但企业增强部分不包含在当前公开仓库中

当前公开仓库主要包含：

1. 基础解析内核代码
2. CLI
3. Java SDK
4. 轻量 Demo Web
5. 启动脚本和基础配置

## Docker 使用

当前仓库支持将 `smartdoc-flow-service` 发布为 Docker 镜像，便于用户直接拉取和运行 Demo Web。

### 镜像地址

当 GitHub 仓库推送形如 `v*` 的 tag 后，GitHub Actions 会自动构建并推送两类镜像到 GitHub Container Registry：

1. 基础镜像
2. OCR 扩展镜像

基础镜像：

```text
ghcr.io/ycy1028-git/smart-doc-flow:<tag>
```

OCR 扩展镜像：

```text
ghcr.io/ycy1028-git/smart-doc-flow:<tag>-ocr
```

例如：

```bash
docker pull ghcr.io/ycy1028-git/smart-doc-flow:v0.1.0
docker pull ghcr.io/ycy1028-git/smart-doc-flow:v0.1.0-ocr
```

同时会额外维护两个便捷标签：

```text
ghcr.io/ycy1028-git/smart-doc-flow:latest
ghcr.io/ycy1028-git/smart-doc-flow:ocr
```

### 运行方式

基础镜像：

```bash
docker run --rm -p 8080:8080 ghcr.io/ycy1028-git/smart-doc-flow:v0.1.0
```

OCR 扩展镜像：

```bash
docker run --rm -p 8080:8080 ghcr.io/ycy1028-git/smart-doc-flow:v0.1.0-ocr
```

默认访问地址：

```text
http://localhost:8080
```

### Docker Compose 运行

仓库根目录提供了一个最小可用的 `docker-compose.yml`，可以直接启动 Demo：

```bash
docker compose up -d
```

停止并删除容器：

```bash
docker compose down
```

默认访问地址：

```text
http://localhost:8080
```

当前 `docker-compose.yml` 默认配置为：

1. 直接使用已发布的 Demo 镜像
2. 暴露 `8080` 端口
3. 不要求配置附件目录、上传目录、输出目录等环境变量
4. 不做文件持久化存储，上传文件仅在容器临时目录内参与解析
5. 默认使用基础镜像，不启用内置 OCR 运行时

如果你想固定镜像版本，可以直接修改：

```yaml
image: ghcr.io/ycy1028-git/smart-doc-flow:v0.1.0
```

例如升级到新版本时，只需要改成：

```yaml
image: ghcr.io/ycy1028-git/smart-doc-flow:v0.2.0
```

如果你希望直接使用 OCR 扩展镜像，可以改成：

```yaml
image: ghcr.io/ycy1028-git/smart-doc-flow:ocr
```

或者固定 OCR 版本：

```yaml
image: ghcr.io/ycy1028-git/smart-doc-flow:v0.2.0-ocr
```

### 环境变量说明

当前 Demo 默认不需要配置额外环境变量。

也就是说，下面这些内容当前都不是必填项：

1. 上传附件存储目录
2. 生成结果存储目录
3. 临时文件目录

原因是当前开源版 Demo 的处理方式是：

1. 上传文件先写入系统临时目录
2. 解析完成后立即删除临时文件
3. `Markdown` 和 `JSON` 直接通过接口返回，不默认落盘保存

当前唯一和运行行为相关的可选环境变量是：

```text
SMARTDOC_FLOW_TESSERACT_PATH
```

它的作用是指定 `tesseract` 可执行文件路径，用于图片和扫描 PDF 的 OCR。

例如：

```yaml
services:
  smart-doc-flow:
    image: ghcr.io/ycy1028-git/smart-doc-flow:v0.1.0
    ports:
      - "8080:8080"
    environment:
      SMARTDOC_FLOW_TESSERACT_PATH: /usr/bin/tesseract
```

但要注意：

1. 这是环境变量，不是 `--xxx=yyy` 这种应用启动参数
2. 基础镜像默认不内置 `tesseract`
3. 所以即使配置了这个环境变量，基础镜像内如果没有对应二进制，也无法启用真实 OCR
4. OCR 扩展镜像已经内置 `tesseract`，默认路径就是 `/usr/bin/tesseract`

### Docker Compose 使用建议

建议按下面方式对外说明：

1. 只做基础 Demo 验证时，直接 `docker compose up -d` 即可
2. 默认无需配置存储目录类环境变量
3. 如需图片 OCR / 扫描 PDF OCR，优先直接切换到 OCR 扩展镜像
4. 当前开源版更适合做基础解析验证，不是附件持久化平台

### OCR 说明

基础 Docker 镜像默认不内置 `tesseract`。

这意味着：

1. 数字原生 PDF、基础 Office、普通文本链路可直接使用
2. 图片 OCR 和扫描 PDF OCR 在容器内会按当前开源版逻辑降级
3. diagnostics 中会保留 OCR 不可用提示，方便定位问题

如果你需要容器内直接启用 OCR，建议使用 OCR 扩展镜像：

```yaml
services:
  smart-doc-flow:
    image: ghcr.io/ycy1028-git/smart-doc-flow:ocr
    container_name: smart-doc-flow
    ports:
      - "8080:8080"
    environment:
      SMARTDOC_FLOW_TESSERACT_PATH: /usr/bin/tesseract
      TZ: Asia/Shanghai
    restart: unless-stopped
```

也可以固定到具体 OCR 版本：

```yaml
image: ghcr.io/ycy1028-git/smart-doc-flow:v0.1.0-ocr
```

当前不支持仅通过单独新增一个 `tesseract` sidecar 容器就让基础镜像自动启用 OCR，因为当前 OCR 调用方式是应用容器内本地进程调用，不是网络服务调用。

### 本地构建镜像

如果你想本地先验证镜像，可以执行：

```bash
./gradlew :smartdoc-flow-service:bootJar
docker build -t smart-doc-flow:local .
docker run --rm -p 8080:8080 smart-doc-flow:local
```

如果你想本地验证 OCR 扩展镜像，可以执行：

```bash
./gradlew :smartdoc-flow-service:bootJar
docker build -t smart-doc-flow:local-ocr -f Dockerfile.ocr .
docker run --rm -p 8080:8080 smart-doc-flow:local-ocr
```

建议不放入公开仓库的内容包括：

1. 更强 OCR Provider 接入实现
2. 复杂表格恢复策略
3. 财报、法务、科研、工业等专项规则包
4. 低质量扫描件增强链路
5. 私有化部署模板和企业交付脚本
6. 企业专用 benchmark、规则库和数据资产

## 开源版 / 企业服务对比

| 维度 | 开源版 | 企业服务 |
|------|--------|---------|
| 定位 | 基础支持 | 增强支持 |
| 目标 | 本地运行、集成验证、二次开发 | 提升复杂文档精度、稳定性与交付效率 |
| 输出 | `Markdown`、`JSON` | `Markdown`、`JSON`，并支持更强场景增强 |
| 入口 | CLI、Java SDK、轻量 Demo Web | 企业增强 SDK、企业 CLI、私有化服务 |
| 文档类型 | 主流 PDF、图片、基础 Office（`DOCX`、`XLSX`、`PPTX`） | 复杂财报、法务、科研、工业等专项文档 |
| OCR | 基础 OCR 路径 | 更强 OCR Provider 与路由策略 |
| 结构恢复 | 基础阅读顺序与基础结构恢复 | 高级阅读顺序、复杂表格和专项结构恢复 |
| 部署 | 本地运行、基础配置 | 私有化部署、Docker 化交付、专项支持 |
| 数据与规则 | 不包含企业规则库和数据资产 | 可提供行业规则包、benchmark 和专项优化 |

## 项目结构

```text
smartdoc-flow-common
smartdoc-flow-core
smartdoc-flow-format
smartdoc-flow-ocr
smartdoc-flow-layout
smartdoc-flow-render
smartdoc-flow-sdk
smartdoc-flow-cli
smartdoc-flow-service
```

模块说明：

1. `smartdoc-flow-common`：通用模型和基础工具
2. `smartdoc-flow-core`：核心解析流程和结果模型
3. `smartdoc-flow-format`：输入格式抽取
4. `smartdoc-flow-ocr`：OCR 抽象与接入
5. `smartdoc-flow-layout`：版面分析和结构恢复
6. `smartdoc-flow-render`：`Markdown` / `JSON` 渲染
7. `smartdoc-flow-sdk`：Java SDK 入口
8. `smartdoc-flow-cli`：命令行入口
9. `smartdoc-flow-service`：轻量 HTTP Demo

## 环境要求

1. `Java 21`
2. `Gradle 8+`

先确认 Java 版本：

```bash
java -version
```

如果需要固定 Gradle 使用的 JDK，可以在 `gradle.properties` 中配置 `org.gradle.java.home`。

## 快速启动

### 1. 构建验证

在仓库根目录执行：

```bash
./gradlew test
```

### 2. 启动 Demo Web

执行：

```bash
./gradlew :smartdoc-flow-service:bootRun
```

默认访问地址：

```text
http://localhost:8080
```

页面会展示：

1. 文档基础 Profile
2. `Markdown` 输出
3. `JSON` 输出

### 3. 运行 CLI

先安装 CLI：

```bash
./gradlew :smartdoc-flow-cli:installDist
```

再执行：

```bash
./smartdoc-flow-cli/build/install/smartdoc-flow-cli/bin/smartdoc-flow-cli parse --input sample.txt
```

如需只看基础 Profile：

```bash
./smartdoc-flow-cli/build/install/smartdoc-flow-cli/bin/smartdoc-flow-cli profile --input sample.txt
```

如需 `JSON` 输出：

```bash
./smartdoc-flow-cli/build/install/smartdoc-flow-cli/bin/smartdoc-flow-cli parse --input sample.txt --format json
```

## 验证方式

建议按下面顺序验证开源版功能。

### 1. CLI 最小验证

仓库根目录包含一个示例文件：`sample.txt`。

执行：

```bash
./smartdoc-flow-cli/build/install/smartdoc-flow-cli/bin/smartdoc-flow-cli profile --input sample.txt
./smartdoc-flow-cli/build/install/smartdoc-flow-cli/bin/smartdoc-flow-cli parse --input sample.txt
./smartdoc-flow-cli/build/install/smartdoc-flow-cli/bin/smartdoc-flow-cli parse --input sample.txt --format json
```

重点观察：

1. 是否能正常读取输入文件
2. 是否能输出基础 Profile
3. 是否能稳定输出 `Markdown`
4. 是否能稳定输出 `JSON`

### 2. Web 验证

启动 Demo Web 后，打开 `http://localhost:8080` 上传文件。

如果你要验证图片或扫描 PDF 的真实 OCR 文本，请先准备本地 `tesseract` 环境。

常见安装方式：

```bash
# macOS (Homebrew)
brew install tesseract

# Ubuntu / Debian
sudo apt-get update
sudo apt-get install -y tesseract-ocr
```

如果 `tesseract` 不在默认路径，也可以通过环境变量指定：

```bash
export SMARTDOC_FLOW_TESSERACT_PATH=/path/to/tesseract
```

当前开源版不会把 `tesseract` 二进制打包进仓库，而是把它作为本地可选运行依赖：

1. 已安装时，图片和扫描 PDF 会走真实 OCR 路径
2. 未安装时，系统会稳定降级，并在 Demo / CLI / diagnostics 中给出提示

建议优先验证：

1. `sample.txt`
2. 数字原生 PDF
3. 基础图片文档

重点观察：

1. `sourceType`
2. `scanned`
3. `multiColumn`
4. `tableHeavy`
5. `imageHeavy`
6. `Markdown` 可读性
7. `JSON` 结构化结果
8. `Diagnostics` 中的 pipeline 过程

当前 Demo 页面还会直接展示：

1. `PIPELINE` 阶段识别出的基础画像
2. `EXTRACT` / `OCR` / `NORMALIZE` / `TABLE_RECOVER` / `POST` 等阶段的关键诊断信息
3. 是否发生了 OCR 降级或回退

### 3. API 验证

Demo 服务接口：

```text
POST /api/demo/parse
```

接口当前返回：

1. 文件名
2. 基础 Profile 字段
3. `Markdown`
4. `JSON`
5. `diagnostics`

可以直接测试：

```bash
curl -X POST http://localhost:8080/api/demo/parse \
  -F "file=@sample.txt"
```

如果想直接查看返回的 `diagnostics`，可以配合 `jq`：

```bash
curl -s -X POST http://localhost:8080/api/demo/parse \
  -F "file=@sample.txt" | jq '.diagnostics'
```

如果你上传的是图片或扫描 PDF，且 `diagnostics` 中出现：

```json
{"stage":"OCR","key":"backend","value":"tesseract-not-found"}
```

说明当前 Demo 所在环境没有检测到 `tesseract`，OCR 已降级。

## 使用方式

当前推荐两种主要使用方式：

1. 用 CLI 做本地解析和快速验证
2. 用 Java SDK 嵌入现有 Java 系统

`smartdoc-flow-service` 主要用于演示和轻量接口验证，不是重型平台入口。

Java SDK 的打包和接入说明见：`sdk-usage.md`

## 开源与合作

欢迎基于开源版进行使用、集成和贡献。

如果你的需求集中在以下方向，也欢迎进一步沟通企业服务：

1. 更高精度复杂文档解析
2. 行业专项规则包
3. 私有化部署与交付
4. 企业级支持和专项优化
