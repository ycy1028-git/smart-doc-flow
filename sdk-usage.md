# SDK Usage

本文档说明开源版 `SmartDoc-Flow` 的 Java SDK 如何打包、如何在本地项目中接入，以及当前对外暴露的调用方式。

## 1. 当前 SDK 模块

当前对外 Java SDK 模块为：

```text
smartdoc-flow-sdk
```

它依赖以下内部模块：

1. `smartdoc-flow-core`
2. `smartdoc-flow-format`
3. `smartdoc-flow-ocr`
4. `smartdoc-flow-layout`
5. `smartdoc-flow-render`

对外入口类是：

```java
io.ycy.smartdocflow.sdk.SmartDocFlow
```

## 2. 如何打包 SDK

### 方式一：构建 SDK JAR

在仓库根目录执行：

```bash
./gradlew :smartdoc-flow-sdk:build
```

产物位置：

```text
smartdoc-flow-sdk/build/libs/smartdoc-flow-sdk-0.1.0-SNAPSHOT.jar
```

注意：

这个 JAR 是普通模块 JAR，不是包含所有依赖的 fat jar。

### 方式二：发布到本地 Maven 仓库

如果你希望在另一个 Java 项目里像普通依赖一样引用，建议先发布到本地 Maven 仓库。

执行：

```bash
./gradlew :smartdoc-flow-sdk:publishToMavenLocal
```

发布后，相关产物会进入本地 Maven 仓库，默认位置通常是：

```text
~/.m2/repository/io/github/smartdocflow/
```

由于 `smartdoc-flow-sdk` 会带上模块依赖信息，本地 Maven 中也需要存在相关模块。最稳妥的方式是直接发布全部可发布模块：

```bash
./gradlew publishToMavenLocal
```

## 3. 当前最简单的接入方式

### 方式一：同仓或多模块项目直接依赖

如果你的 Java 项目和当前项目在同一套 Gradle 多模块工程里，可以直接：

```kotlin
dependencies {
    implementation(project(":smartdoc-flow-sdk"))
}
```

### 方式二：外部项目通过 Maven Local 引用

在你的外部 Gradle 项目中添加：

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.ycy.smartdocflow:smartdoc-flow-sdk:0.1.0-SNAPSHOT")
}
```

### 方式三：先用本地 JAR 调试

先构建：

```bash
./gradlew :smartdoc-flow-sdk:build
```

然后把下面这些模块产物一起加入你的项目 classpath：

1. `smartdoc-flow-sdk`
2. `smartdoc-flow-core`
3. `smartdoc-flow-format`
4. `smartdoc-flow-ocr`
5. `smartdoc-flow-layout`
6. `smartdoc-flow-render`
7. `smartdoc-flow-common`

因为当前 `smartdoc-flow-sdk` 本身不是 fat jar，只引一个 SDK JAR 还不够。

## 4. Java 如何调用

当前 SDK 的 Facade 非常简单：

```java
import io.ycy.smartdocflow.sdk.SmartDocFlow;
import java.nio.file.Path;

public class Demo {
    public static void main(String[] args) {
        SmartDocFlow smartDocFlow = new SmartDocFlow();
        Path file = Path.of("sample.txt");

        String markdown = smartDocFlow.parseToMarkdown(file);
        String json = smartDocFlow.parseToJson(file);

        System.out.println(markdown);
        System.out.println(json);
    }
}
```

## 5. 当前可调用的方法

`io.ycy.smartdocflow.sdk.SmartDocFlow` 当前提供：

1. `profile(Path source)`
2. `parse(Path source)`
3. `parseToMarkdown(Path source)`
4. `parseToJson(Path source)`

示例：

```java
import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.DocumentResult;
import io.ycy.smartdocflow.sdk.SmartDocFlow;
import java.nio.file.Path;

public class Demo {
    public static void main(String[] args) {
        SmartDocFlow smartDocFlow = new SmartDocFlow();
        Path file = Path.of("demo.pptx");

        DocumentProfile profile = smartDocFlow.profile(file);
        DocumentResult result = smartDocFlow.parse(file);
        String markdown = smartDocFlow.parseToMarkdown(file);
        String json = smartDocFlow.parseToJson(file);

        System.out.println("sourceType = " + profile.sourceType());
        System.out.println("scanned = " + profile.scanned());
        System.out.println("blocks = " + result.blocks().size());
        System.out.println(markdown);
        System.out.println(json);
    }
}
```

## 6. 当前支持的输入类型

当前 SDK 基础支持以下类型：

1. `PDF`
2. 图片文档，如 `PNG`、`JPG`、`TIFF`
3. `DOCX` 基础解析
4. `XLSX` 基础提取
5. `PPTX` 基础解析

其中 `PPTX` 当前属于基础支持，适合期待：

1. slide 标题提取
2. 文本框提取
3. 基础表格文本提取
4. 图片占位说明
5. 备注提取
6. `Markdown` / `JSON` 输出

当前不建议期待：

1. 复杂版式高保真恢复
2. SmartArt 语义重建
3. 复杂图表结构恢复
4. 动画语义
5. 老 `PPT` 格式支持

## 7. 现阶段最推荐的接入方式

如果你现在只是验证功能，最推荐顺序是：

1. 先用 CLI 验证解析效果
2. 再用 Demo Web 看输出
3. 最后在 Java 项目里通过 `SmartDocFlow` 直接接入

这样定位问题会更快。

## 8. 推荐发布方式

当前最推荐的 SDK 本地发布方式：

```bash
./gradlew publishToMavenLocal
```

如果你只想单独发布 SDK 模块，也可以：

```bash
./gradlew :smartdoc-flow-sdk:publishToMavenLocal
```

但从依赖完整性看，建议优先发布全部模块。

## 9. 当前文档结论

当前已经有基础 SDK 能力，也已经具备本地 Maven 发布能力。现阶段最准确的说法是：

1. SDK 已可在源码工程内直接使用
2. SDK 已可打出模块 JAR
3. SDK 已可发布到本地 Maven 仓库
4. SDK 还未完善为发布到 Maven Central 的正式流程

如果你需要，我下一步可以继续补：

1. Maven Central 所需的发布元数据
2. 更完整的外部示例工程
3. README 中的 SDK 快速接入片段
