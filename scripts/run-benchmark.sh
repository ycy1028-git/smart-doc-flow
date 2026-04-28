#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MANIFEST="$ROOT_DIR/benchmarks/sample-manifest.txt"
CLI_BIN="$ROOT_DIR/smartdoc-flow-cli/build/install/smartdoc-flow-cli/bin/smartdoc-flow-cli"
RESULTS_DIR="$ROOT_DIR/benchmarks/results"
CURRENT_DIR="$RESULTS_DIR/current"
GOLDEN_DIR="$RESULTS_DIR/golden"

"$ROOT_DIR/gradlew" :smartdoc-flow-cli:generateBenchmarkSamples >/dev/null

mkdir -p "$CURRENT_DIR" "$GOLDEN_DIR"
rm -f "$CURRENT_DIR"/*.txt

if [[ ! -f "$MANIFEST" ]]; then
  printf 'Missing manifest: %s\n' "$MANIFEST" >&2
  exit 1
fi

if [[ ! -x "$CLI_BIN" ]]; then
  printf 'CLI not found. Run: ./gradlew :smartdoc-flow-cli:installDist\n' >&2
  exit 1
fi

sanitize_name() {
  printf '%s' "$1" | tr '/ .' '___'
}

compare_or_update_golden() {
  current_file="$1"
  golden_file="$2"

  if [[ "${UPDATE_GOLDEN:-0}" == "1" ]]; then
    cp "$current_file" "$golden_file"
    printf 'golden updated: %s\n' "$(basename "$golden_file")"
    return
  fi

  if [[ ! -f "$golden_file" ]]; then
    cp "$current_file" "$golden_file"
    printf 'golden created: %s\n' "$(basename "$golden_file")"
    return
  fi

  if ! cmp -s "$current_file" "$golden_file"; then
    printf 'benchmark drift: %s\n' "$(basename "$golden_file")" >&2
    diff -u "$golden_file" "$current_file" || true
    exit 1
  fi

  printf 'golden matched: %s\n' "$(basename "$golden_file")"
}

run_and_compare() {
  sample_path="$1"
  sample_key="$2"
  kind="$3"
  shift 3

  output_file="$CURRENT_DIR/${sample_key}.${kind}.txt"
  golden_file="$GOLDEN_DIR/${sample_key}.${kind}.txt"
  command_name="$1"
  shift 1
  "$CLI_BIN" "$command_name" --input "$sample_path" "$@" 2>&1 | perl -ne 'print unless /Log4j2 could not find a logging implementation|Using SimpleLogger to log to the console/' | tee "$output_file"
  normalize_output "$output_file" "$kind"
  compare_or_update_golden "$output_file" "$golden_file"
}

run_and_compare_with_env() {
  sample_path="$1"
  sample_key="$2"
  kind="$3"
  env_name="$4"
  env_value="$5"
  shift 5

  output_file="$CURRENT_DIR/${sample_key}.${kind}.txt"
  golden_file="$GOLDEN_DIR/${sample_key}.${kind}.txt"
  command_name="$1"
  shift 1
  env "$env_name=$env_value" "$CLI_BIN" "$command_name" --input "$sample_path" "$@" 2>&1 | perl -ne 'print unless /Log4j2 could not find a logging implementation|Using SimpleLogger to log to the console/' | tee "$output_file"
  normalize_output "$output_file" "$kind"
  compare_or_update_golden "$output_file" "$golden_file"
}

normalize_output() {
  output_file="$1"
  kind="$2"

  if [[ "$kind" == *json ]]; then
    perl -0pi -e 's/"documentId":"[^"]*"/"documentId":"<doc-id>"/g' "$output_file"
    perl -0pi -e 's/"id":"[^"]*"/"id":"<node-id>"/g' "$output_file"
    return
  fi

  if [[ "$kind" == *diagnostics ]]; then
    perl -0pi -e 's/: [0-9]{10,}/: <number>/g; s/(startedAt|durationMs): [^\n]+/$1: <number>/g' "$output_file"
  fi
}

while IFS= read -r line; do
  [[ -z "$line" || "$line" == \#* ]] && continue
  sample_path="$ROOT_DIR/$line"
  if [[ ! -f "$sample_path" ]]; then
    printf 'Missing sample: %s\n' "$sample_path" >&2
    exit 1
  fi
  printf '\n== %s ==\n' "$line"
  sample_key="$(sanitize_name "$line")"
  run_and_compare "$sample_path" "$sample_key" profile profile
  run_and_compare "$sample_path" "$sample_key" markdown parse
  run_and_compare "$sample_path" "$sample_key" json parse --format json
  run_and_compare "$sample_path" "$sample_key" diagnostics parse --diagnostics

  if [[ "$line" == "benchmarks/generated/ocr-sample.png" || "$line" == "benchmarks/generated/scanned-sample.pdf" ]]; then
    run_and_compare_with_env "$sample_path" "$sample_key" noop-markdown SMARTDOC_FLOW_OCR_PROVIDER noop parse
    run_and_compare_with_env "$sample_path" "$sample_key" noop-json SMARTDOC_FLOW_OCR_PROVIDER noop parse --format json
    run_and_compare_with_env "$sample_path" "$sample_key" noop-diagnostics SMARTDOC_FLOW_OCR_PROVIDER noop parse --diagnostics
  fi
done < "$MANIFEST"
