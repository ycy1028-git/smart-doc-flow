# Benchmark Baseline

This directory stores the minimal benchmark manifest for OSS regression checks.

## Current scope

1. Track project-relative sample files in `sample-manifest.txt`
2. Run CLI profile / markdown / json / diagnostics over every listed sample
3. Save current outputs into `benchmarks/results/current/`
4. Compare them against `benchmarks/results/golden/`

Current normalization:

1. `json` replaces `documentId`
2. `diagnostics` replaces volatile numeric values such as `startedAt` and `durationMs`

Special downgrade coverage:

1. `ocr-sample.png`
2. `scanned-sample.pdf`

These two samples additionally run once with `SMARTDOC_FLOW_OCR_PROVIDER=noop` to lock degraded OCR behavior.

## Run

```bash
./gradlew :smartdoc-flow-cli:installDist
./scripts/run-benchmark.sh
```

`run-benchmark.sh` 会先生成 `benchmarks/generated/` 下的正常样本、空白边界样本和损坏失败样本：`PDF` / 扫描 `PDF` / 图片 / `DOCX` / `XLSX` / `PPTX`，并复制 `PPTX notes/image` 资源样本，再逐个跑 benchmark。

Current failure-sample coverage:

1. `broken.pdf`
2. `broken.docx`
3. `broken.xlsx`
4. `broken.pptx`

首次运行会自动创建 `golden` 文件；后续运行若有输出漂移会直接失败并打印 diff。

如需刷新 golden：

```bash
UPDATE_GOLDEN=1 ./scripts/run-benchmark.sh
```
