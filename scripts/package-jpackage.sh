#!/usr/bin/env bash
set -eu

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="$ROOT/target"
DIST_BASE="$ROOT/dist/jpackage"
STAGING_DIR="$TARGET_DIR/jpackage-staging"
OS_NAME="$(uname -s)"

case "$OS_NAME" in
  Linux)
    DIST_DIR="$DIST_BASE/linux"
    ZIP_NAME="GBUXML-portable-linux.zip"
    ;;
  Darwin)
    DIST_DIR="$DIST_BASE/mac"
    ZIP_NAME="GBUXML-portable-mac.zip"
    ;;
  *)
    echo "Unsupported OS for jpackage packaging: $OS_NAME"
    exit 1
    ;;
esac

rm -rf "$DIST_BASE" "$STAGING_DIR"
mkdir -p "$DIST_DIR" "$STAGING_DIR"

if [ ! -d "$TARGET_DIR" ]; then
  echo "Build target not found. Run 'mvn clean package' first."
  exit 1
fi

JAR_PATH="$(find "$TARGET_DIR" -maxdepth 1 -type f -name '*.jar' ! -name '*original*' | head -n 1 || true)"
if [ -z "$JAR_PATH" ]; then
  echo "No runnable jar found in target directory."
  exit 1
fi

cp "$JAR_PATH" "$STAGING_DIR/"
JAR_NAME="$(basename "$JAR_PATH")"

JPACKAGE_BIN="${JAVA_HOME:-}/bin/jpackage"
if [ ! -x "$JPACKAGE_BIN" ]; then
  JPACKAGE_BIN="$(command -v jpackage || true)"
fi

if [ -z "$JPACKAGE_BIN" ] || [ ! -x "$JPACKAGE_BIN" ]; then
  echo "jpackage wurde nicht gefunden. Bitte JAVA_HOME auf ein JDK mit jpackage setzen."
  exit 1
fi

"$JPACKAGE_BIN" \
  --type app-image \
  --name GBUXML \
  --app-version 1.1.0 \
  --vendor GBUXML \
  --input "$STAGING_DIR" \
  --main-jar "$JAR_NAME" \
  --main-class com.gbuxml.GBUXMLApplication \
  --dest "$DIST_DIR"

APP_DEST="$DIST_DIR/GBUXML"
if [ ! -d "$APP_DEST" ]; then
  echo "App image was not created at $APP_DEST"
  exit 1
fi

ZIP_DIR="$ROOT/dist"
mkdir -p "$ZIP_DIR"
ZIP_PATH="$ZIP_DIR/$ZIP_NAME"
rm -f "$ZIP_PATH"
zip -r "$ZIP_PATH" "$APP_DEST" >/dev/null

echo "Portable app image created in $DIST_DIR"
echo "Portable ZIP created at $ZIP_PATH"
