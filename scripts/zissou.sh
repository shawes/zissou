#!/usr/bin/env bash
# Runs Zissou with the JVM flags that suit it:
#
#   scripts/zissou.sh path/to/config.yaml
#
# Zissou is a batch job, so it uses the throughput (Parallel) collector. On the
# bench configuration this was ~15% faster and steadier than the default G1.
# The heap size is left at the JVM default, a quarter of the machine's memory;
# for a run with many larvae give it more:
#
#   ZISSOU_JAVA_OPTS="-Xmx12g" scripts/zissou.sh config.yaml
#
# JAVA_HOME is used if set, and ZISSOU_JAR overrides the jar (default: the
# one built by `sbt assembly`).
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
jar="${ZISSOU_JAR:-$root/target/zissou.jar}"
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"

if [ ! -f "$jar" ]; then
  echo "Cannot find $jar - build it with 'sbt assembly'" >&2
  exit 1
fi

# shellcheck disable=SC2086  # ZISSOU_JAVA_OPTS is meant to word-split
exec "$java_bin" -XX:+UseParallelGC ${ZISSOU_JAVA_OPTS:-} -jar "$jar" "$@"
