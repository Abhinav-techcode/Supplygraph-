#!/usr/bin/env bash
# Starts SupplyGraph connected to PostgreSQL (Docker) by default. Open http://localhost:${PORT:-8080}
#   ./run-demo.sh            -> PostgreSQL 16 in Docker (container "supplygraph-postgres", port 5433); data survives restarts
#   DB=h2 ./run-demo.sh      -> in-memory H2 (no Docker); data is lost when the app stops
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

ARGS="--server.port=${PORT:-8080} --supplygraph.auth.seed-demo-users=true --supplygraph.auth.show-demo-accounts=true"
PROFILE_ARG=""
if [ "${DB:-postgres}" = "h2" ]; then
  PROFILE_ARG="-Dspring-boot.run.profiles=local"
  echo "Database: in-memory H2 (data is lost when the app stops)"
else
  command -v docker >/dev/null || { echo "Docker is required for PostgreSQL. Install it, or run: DB=h2 ./run-demo.sh"; exit 1; }
  if ! docker info >/dev/null 2>&1; then
    if command -v colima >/dev/null; then echo "Starting Colima (Docker)..."; colima start; else echo "Docker is not running. Start Docker Desktop, or run: DB=h2 ./run-demo.sh"; exit 1; fi
  fi
  if docker ps -a --format '{{.Names}}' | grep -qx supplygraph-postgres; then
    docker start supplygraph-postgres >/dev/null
  else
    echo "Creating the PostgreSQL container supplygraph-postgres..."
    docker run -d --name supplygraph-postgres -e POSTGRES_DB=supplygraph -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5433:5432 -v supplygraph-pgdata:/var/lib/postgresql/data postgres:16-alpine >/dev/null
  fi
  printf "Waiting for PostgreSQL"
  for _ in $(seq 1 30); do docker exec supplygraph-postgres pg_isready -U postgres >/dev/null 2>&1 && break; printf "."; sleep 1; done; echo
  docker exec supplygraph-postgres pg_isready -U postgres >/dev/null 2>&1 || { echo "PostgreSQL did not become ready."; exit 1; }
  echo "Database: PostgreSQL on localhost:5433 (database supplygraph; data is kept between runs)"
fi
echo "Open http://localhost:${PORT:-8080}  ->  Sign in as manager / Demo!2026 (also: admin, procurement, production, logistics, quality, analyst, viewer)"
MVN_REPO_ARG=""
[ -d .m2-demo ] && MVN_REPO_ARG="-Dmaven.repo.local=$PWD/.m2-demo"
./mvnw $MVN_REPO_ARG -q install -DskipTests
# shellcheck disable=SC2086
exec ./mvnw $MVN_REPO_ARG -pl supplygraph-web spring-boot:run $PROFILE_ARG -Dspring-boot.run.arguments="$ARGS"
