## Summary

`SmartDoc-Flow` 的本次发布聚焦开源版基础能力，适合本地运行、效果验证和二次集成。

## Included

- PDF 基础解析
- 图片与扫描 PDF 的基础 OCR 路径
- DOCX 基础解析
- XLSX 基础表格提取
- PPTX 基础解析
- Markdown / JSON 输出
- Java SDK
- CLI
- Demo Web

## Docker

如果当前版本已同步发布 GHCR 镜像，可直接拉取：

```bash
docker pull ghcr.io/ycy1028-git/smart-doc-flow:<tag>
docker run --rm -p 8080:8080 ghcr.io/ycy1028-git/smart-doc-flow:<tag>
```

默认访问地址：

```text
http://localhost:8080
```

## Notes

- 当前版本定位为 OSS 基础版 / 预览版
- 当前仓库聚焦基础可用闭环，不承诺复杂文档高精度恢复
- Docker 镜像默认不内置 `tesseract`，图片 OCR 和扫描 PDF OCR 会按当前开源版逻辑降级
- 如需复杂表格恢复、高级 OCR、专项规则包或私有化交付，建议按企业增强能力单独规划

## Verification

建议在发布前至少完成以下验证：

- `./gradlew test`
- `./gradlew :smartdoc-flow-service:bootJar`
- 本地确认 Demo Web 可启动并访问

## Upgrade Notes

- 如当前版本涉及输出结构、镜像地址或命令行参数调整，请在此补充升级说明
- 如当前版本仅为增量修复，可删除本节
