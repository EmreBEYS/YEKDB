#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.0.14}"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPOSITORY_ROOT="$(cd "$PROJECT_ROOT/.." && pwd)"
DIST_DIR="$PROJECT_ROOT/dist"
PACKAGE_INPUT="$PROJECT_ROOT/target/package-input"
APP_NAME="YEKDB"
APP_PATH="$DIST_DIR/$APP_NAME.app"
DMG_PATH="$DIST_DIR/YEKDB-$VERSION-$(uname -m).dmg"
IMAGE_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/yekdb-image.XXXXXX")"
DMG_STAGE="$(mktemp -d "${TMPDIR:-/tmp}/yekdb-dmg.XXXXXX")"
trap 'rm -rf "$IMAGE_ROOT" "$DMG_STAGE"' EXIT

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Hata: macOS paketi yalnızca macOS üzerinde üretilebilir." >&2
  exit 1
fi

if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Hata: sürüm x.y.z biçiminde olmalıdır (örnek: 1.0.12)." >&2
  exit 1
fi

for command in java jpackage hdiutil codesign; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "Hata: gerekli araç bulunamadı: $command" >&2
    exit 1
  fi
done

JAVA_MAJOR="$(java -version 2>&1 | sed -nE '1s/.*version "([0-9]+).*/\1/p')"
if [[ "$JAVA_MAJOR" != "21" ]]; then
  echo "Hata: paketleme için ARM64 JDK 21 gerekli; etkin Java sürümü: ${JAVA_MAJOR:-bilinmiyor}." >&2
  echo "JAVA_HOME değerini JDK 21'e ayarlayıp yeniden deneyin." >&2
  exit 1
fi

JAVA_ARCH="$(java -XshowSettings:properties -version 2>&1 | sed -nE 's/^[[:space:]]*os.arch = (.*)$/\1/p')"
if [[ "$(uname -m)" == "arm64" && "$JAVA_ARCH" != "aarch64" ]]; then
  echo "Hata: Apple Silicon için ARM64 JDK gerekli; etkin JDK mimarisi: $JAVA_ARCH" >&2
  exit 1
fi

cd "$PROJECT_ROOT"
MAVEN_OPTIONS=()
if [[ -n "${MAVEN_REPO_LOCAL:-}" ]]; then
  MAVEN_OPTIONS+=("-Dmaven.repo.local=$MAVEN_REPO_LOCAL")
fi
./mvnw -f "$REPOSITORY_ROOT/pom.xml" clean package -DskipTests "${MAVEN_OPTIONS[@]}"
./mvnw clean package "${MAVEN_OPTIONS[@]}"

CORE_JAR="$REPOSITORY_ROOT/target/yekdb-$VERSION.jar"
LAUNCHER_JAR="yekdb-launcher-$VERSION.jar"
if [[ ! -f "$CORE_JAR" || ! -f "$PACKAGE_INPUT/$LAUNCHER_JAR" ]]; then
  echo "Hata: beklenen JAR çıktıları bulunamadı." >&2
  exit 1
fi
cp "$CORE_JAR" "$PACKAGE_INPUT/yekdb.jar"

rm -rf "$APP_PATH"
rm -f "$DMG_PATH"
mkdir -p "$DIST_DIR"

jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$VERSION" \
  --vendor "YEKDB" \
  --copyright "Copyright © YEKDB 2026" \
  --description "YEKDB Launcher and Database Engine" \
  --input "$PACKAGE_INPUT" \
  --main-jar "$LAUNCHER_JAR" \
  --main-class "com.yekdb.launcher.LauncherMain" \
  --add-launcher "YEKDB Engine=$PROJECT_ROOT/packaging/macos/yekdb-engine.properties" \
  --icon "$PROJECT_ROOT/packaging/icons/yekdb.icns" \
  --dest "$IMAGE_ROOT" \
  --mac-package-name "$APP_NAME" \
  --mac-package-identifier "com.yekdb.app"

# Developer ID bulunmayan yerel demo build'i için bütün bundle'ı ad-hoc imzala.
# Paket görüntüsü önce yerel geçici dizinde imzalanır; iCloud/File Provider'ın
# çıktı klasörüne eklediği Finder xattr'ları codesign girdisine karışmamalıdır.
TEMP_APP="$IMAGE_ROOT/$APP_NAME.app"
xattr -dr com.apple.FinderInfo "$TEMP_APP" 2>/dev/null || true
xattr -dr 'com.apple.fileprovider.fpfs#P' "$TEMP_APP" 2>/dev/null || true
codesign --force --deep --sign - --timestamp=none "$TEMP_APP"
codesign --verify --deep --strict --verbose=2 "$TEMP_APP"
mv "$TEMP_APP" "$APP_PATH"

ditto --noextattr --noqtn "$APP_PATH" "$DMG_STAGE/$APP_NAME.app"
codesign --verify --deep --strict --verbose=2 "$DMG_STAGE/$APP_NAME.app"
ln -s /Applications "$DMG_STAGE/Applications"
if ! hdiutil create -volname "YEKDB $VERSION" -srcfolder "$DMG_STAGE" \
  -ov -format UDZO "$DMG_PATH"; then
  # macOS 27 hdiutil create yerine yeni DiskManagement yolunu zorlayabiliyor.
  # DiskArbitration bulunmayan CI/sandbox ortamlarında hibrit HFS görüntüsünü
  # önce üretip ardından sıkıştırılmış UDIF'e dönüştürmek aynı sonucu verir.
  RAW_DMG="$IMAGE_ROOT/YEKDB-$VERSION-raw.dmg"
  hdiutil makehybrid -hfs -hfs-volume-name "YEKDB $VERSION" \
    -o "$RAW_DMG" "$DMG_STAGE"
  hdiutil convert "$RAW_DMG" -format UDZO -o "$DMG_PATH"
fi
hdiutil verify "$DMG_PATH"

echo "YEKDB macOS paketleri hazır:"
echo "  $APP_PATH"
echo "  $DMG_PATH"
