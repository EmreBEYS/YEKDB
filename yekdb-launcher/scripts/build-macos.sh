#!/usr/bin/env bash
set -euo pipefail

TYPE="${1:-dmg}"
VERSION="${2:-1.0.0}"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPOSITORY_ROOT="$(cd "$PROJECT_ROOT/.." && pwd)"
cd "$PROJECT_ROOT"

./mvnw -f "$REPOSITORY_ROOT/pom.xml" clean package -DskipTests
./mvnw clean package
cp "$REPOSITORY_ROOT/target/yekdb-1.0.0.jar" "$PROJECT_ROOT/target/package-input/yekdb.jar"
mkdir -p dist
jpackage \
  --type "$TYPE" \
  --name "YEKDB Launcher" \
  --app-version "$VERSION" \
  --vendor "YEKDB" \
  --description "YEKDB update and launch manager" \
  --input "$PROJECT_ROOT/target/package-input" \
  --main-jar "yekdb-launcher-1.0.0.jar" \
  --main-class "com.yekdb.launcher.LauncherMain" \
  --add-launcher "YEKDB Engine=$PROJECT_ROOT/packaging/macos/yekdb-engine.properties" \
  --icon "$PROJECT_ROOT/packaging/icons/yekdb.icns" \
  --dest "$PROJECT_ROOT/dist" \
  --mac-package-name "YEKDB Launcher" \
  --mac-package-identifier "com.yekdb.launcher"
