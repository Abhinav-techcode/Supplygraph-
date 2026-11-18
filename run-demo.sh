#!/usr/bin/env bash
# Starts SupplyGraph with an in-memory H2 database and demo data. No PostgreSQL needed. Open http://localhost:${PORT:-8080}
set -euo pipefail
cd "$(dirname "$0")"
find_jdk21() {
  if [ -n "${JAVA_HOME:-}" ] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -q '"21'; then echo "$JAVA_HOME"; return; fi
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then /usr/libexec/java_home -v 21 2>/dev/null && return; fi
  for d in "$HOME/.jdks/jdk21/Contents/Home" /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home /usr/lib/jvm/java-21-openjdk-amd64; do [ -x "$d/bin/java" ] && { echo "$d"; return; }; done
  return 1
}
export JAVA_HOME="$(find_jdk21)" || { echo "JDK 21 not found. Install Temurin 21 (brew install --cask temurin@21) and re-run."; exit 1; }
echo "Using JDK: $JAVA_HOME"
export SUPPLYGRAPH_UI_DEMO_ROLE_SWITCHER=true
MVN_REPO_ARG=""
[ -d .m2-demo ] && MVN_REPO_ARG="-Dmaven.repo.local=$PWD/.m2-demo"
./mvnw $MVN_REPO_ARG -q install -DskipTests
exec ./mvnw $MVN_REPO_ARG -pl supplygraph-web spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--server.port=${PORT:-8080}"
