# SmartDoc-Flow

`SmartDoc-Flow` 是一个轻量型文档解析工具，面向本地运行、二次集成和快速验证场景。

它的目标是把常见文档尽可能稳定地转换为结构化、可消费的 `Markdown` 和 `JSON`，而不是做一个重型文档处理平台。

当前仓库主要提供：

1. 开源解析代码
2. CLI 启动方式
3. 轻量 Demo Web
4. 基础运行配置

## 开源版定位

开源版聚焦“基础支持”，强调：

1. 本地可跑
2. 可嵌入 Java 项目
3. 可直接输出 `Markdown` 和 `JSON`
4. 适合做主链路验证和基础集成

当前适合优先验证的文档类型包括：

1. 数字原生 PDF
2. 扫描 PDF 基础 OCR 路径
3. 图片文档，如 `PNG`、`JPG`、`TIFF`
4. `DOCX` 基础解析
5. `XLSX` 基础表格提取

开源版当前重点保证：

1. 基础阅读顺序可用
2. 标题、正文、列表等基础结构可用
3. 表格至少能输出基本可消费结构
4. `Markdown` 和 `JSON` 输出稳定

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

建议不放入公开仓库的内容包括：

1. 更强 OCR Provider 接入实现
2. 复杂表格恢复策略
3. 财报、法务、科研、工业等专项规则包
4. 低质量扫描件增强链路
5. 私有化部署模板和企业交付脚本
6. 企业专用 benchmark、规则库和数据资产

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
./smartdoc-flow-cli/build/install/smartdoc-flow-cli/bin/smartdoc-flow-cli parse --input sample.txt
./smartdoc-flow-cli/build/install/smartdoc-flow-cli/bin/smartdoc-flow-cli parse --input sample.txt --format json
```

重点观察：

1. 是否能正常读取输入文件
2. 是否能稳定输出 `Markdown`
3. 是否能稳定输出 `JSON`

### 2. Web 验证

启动 Demo Web 后，打开 `http://localhost:8080` 上传文件。

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

### 3. API 验证

Demo 服务接口：

```text
POST /api/demo/parse
```

可以直接测试：

```bash
curl -X POST http://localhost:8080/api/demo/parse \
  -F "file=@sample.txt"
```

## 使用方式

当前推荐两种主要使用方式：

1. 用 CLI 做本地解析和快速验证
2. 用 Java SDK 嵌入现有 Java 系统

`smartdoc-flow-service` 主要用于演示和轻量接口验证，不是重型平台入口。

## 开源与合作

欢迎基于开源版进行使用、集成和贡献。

如果你的需求集中在以下方向，也欢迎进一步沟通企业服务：

1. 更高精度复杂文档解析
2. 行业专项规则包
3. 私有化部署与交付
4. 企业级支持和专项优化
