#!/usr/bin/env bash
# Runs Zissou with the JVM flags that suit it.
#
#   scripts/zissou.sh path/to/config.yaml     run a simulation from the command line
#   scripts/zissou.sh ui [options]            start the local web UI (see --help)
#   scripts/zissou.sh cache-flow CONFIG DIR   copy the flow a configuration reads into a folder
#   scripts/zissou.sh sensitivity DESIGN      run a sensitivity design and sum up its runs
#
# Build first with `sbt stage`, which lays the program out in target/universal/stage.
#
# Zissou is a batch job, so it uses the throughput (Parallel) collector. On the
# bench configuration this was ~15% faster and steadier than the default G1.
# The heap size is left at the JVM default, a quarter of the machine's memory;
# for a run with many larvae give it more:
#
#   ZISSOU_JAVA_OPTS="-Xmx12g" scripts/zissou.sh config.yaml
#
# JAVA_HOME is used if set, and ZISSOU_HOME overrides the staged program
# (default: target/universal/stage).
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
stage="${ZISSOU_HOME:-$root/target/universal/stage}"
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"

if [ ! -d "$stage/lib" ]; then
  echo "Cannot find $stage/lib - build it with 'sbt stage'" >&2
  exit 1
fi

main=model.Simulator
case "${1:-}" in
  ui) main=ui.Server; shift ;;
  cache-flow) main=io.FlowCache; shift ;;
  sensitivity) main=analysis.Sensitivity; shift ;;
esac

# shellcheck disable=SC2086  # ZISSOU_JAVA_OPTS is meant to word-split
exec "$java_bin" -XX:+UseParallelGC ${ZISSOU_JAVA_OPTS:-} -cp "$stage/lib/*" "$main" "$@"
