# SmartDoc-Flow

`SmartDoc-Flow` 是一个轻量文档解析工具，面向本地运行、二次集成和效果验证。

支持：`PDF`、图片（`PNG/JPG/JPEG/TIFF`）、`DOCX`、`XLSX`、`PPTX`

输出：`Markdown`、`JSON`、结构化 block、diagnostics

## Demo

在线地址：

```text
https://demo.yyapi.cc
```

本地启动：

```bash
./gradlew :smartdoc-flow-service:bootRun
```

访问：

```text
http://localhost:8080
```

## 快速开始

CLI：

```bash
./gradlew :smartdoc-flow-cli:installDist
./smartdoc-flow-cli/build/install/smartdoc-flow-cli/bin/smartdoc-flow-cli parse --input sample.pdf
./smartdoc-flow-cli/build/install/smartdoc-flow-cli/bin/smartdoc-flow-cli parse --input sample.pdf --format json
./smartdoc-flow-cli/build/install/smartdoc-flow-cli/bin/smartdoc-flow-cli parse --input sample.pdf --diagnostics
```

Java SDK：

```java
import io.ycy.smartdocflow.sdk.SmartDocFlow;
import java.nio.file.Path;

public class Demo {
    public static void main(String[] args) {
        SmartDocFlow smartDocFlow = new SmartDocFlow();
        Path file = Path.of("sample.pdf");
        System.out.println(smartDocFlow.parseToMarkdown(file));
        System.out.println(smartDocFlow.parseToJson(file));
    }
}
```

常用方法：

1. `profile(Path source)`
2. `parse(Path source)`
3. `parseToIr(Path source)`
4. `parseDiagnostics(Path source)`
5. `parseToMarkdown(Path source)`
6. `parseToJson(Path source)`

发布到本地 Maven：

```bash
./gradlew publishToMavenLocal
```

## Docker

镜像：

```text
ghcr.io/ycy1028-git/smart-doc-flow:<tag>
ghcr.io/ycy1028-git/smart-doc-flow:<tag>-ocr
ghcr.io/ycy1028-git/smart-doc-flow:latest
ghcr.io/ycy1028-git/smart-doc-flow:ocr
```

运行：

```bash
docker run --rm -p 8080:8080 ghcr.io/ycy1028-git/smart-doc-flow:<tag>
docker run --rm -p 8080:8080 ghcr.io/ycy1028-git/smart-doc-flow:<tag>-ocr
docker compose up -d
docker compose -f docker-compose.ocr.yml up -d
```

## OCR

基础镜像：不内置 `tesseract`，图片 OCR / 扫描 PDF OCR 会降级。

OCR 镜像：

1. 内置 `tesseract`
2. 内置 `chi_sim` 中文简体语言包
3. 默认 `SMARTDOC_FLOW_OCR_LANG=chi_sim+eng`

关键环境变量：

```text
SMARTDOC_FLOW_TESSERACT_PATH
SMARTDOC_FLOW_OCR_PROVIDER
SMARTDOC_FLOW_OCR_LANG
```

若 diagnostics 出现：

```json
{"stage":"OCR","key":"backend","value":"tesseract-not-found"}
```

说明未启用真实 OCR。

## 测试与基准

运行测试：

```bash
./gradlew test
./gradlew :smartdoc-flow-service:bootJar
```

运行 benchmark：

```bash
./gradlew :smartdoc-flow-cli:installDist
./scripts/run-benchmark.sh
```

补充说明见：`benchmarks/README.md`

## 发布

推送 `v*` tag 后，GitHub Actions 自动发布基础镜像和 OCR 镜像。

```bash
git tag v0.2.0
git push origin v0.2.0
```

OCR 镜像构建使用 `Dockerfile.ocr`。

## 边界

适合：本地运行、集成验证、基础二次开发。

不包含：高级 OCR Provider、企业规则包、私有化交付模板、复杂行业增强策略。
