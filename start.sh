#!/bin/bash
# start.sh — builds and launches the ATM CLI application.
# Requires: Java 21+. Uses system Gradle if available, otherwise downloads Gradle.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

GRADLE_CMD="gradle"
if ! command -v gradle >/dev/null 2>&1; then
  GRADLE_VERSION="9.5.1"
  GRADLE_HOME="$SCRIPT_DIR/.gradle-dist/gradle-$GRADLE_VERSION"
  GRADLE_CMD="$GRADLE_HOME/bin/gradle"

  if [ ! -x "$GRADLE_CMD" ]; then
	echo "Gradle not found. Downloading Gradle $GRADLE_VERSION..."
	mkdir -p "$SCRIPT_DIR/.gradle-dist"
	ZIP_PATH="$SCRIPT_DIR/.gradle-dist/gradle-$GRADLE_VERSION-bin.zip"

	if command -v curl >/dev/null 2>&1; then
	  curl -fsSL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP_PATH"
	elif command -v wget >/dev/null 2>&1; then
	  wget -q -O "$ZIP_PATH" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
	else
	  echo "Error: neither curl nor wget is available to download Gradle."
	  exit 1
	fi

	unzip -q -o "$ZIP_PATH" -d "$SCRIPT_DIR/.gradle-dist"
	chmod +x "$GRADLE_CMD"
  fi
fi

echo "Building ATM application (skipping tests for fast startup)..."
"$GRADLE_CMD" build -x test --quiet

echo "ATM ready. Starting..."
echo ""
java -jar build/libs/ATM-0.0.1-SNAPSHOT.jar

