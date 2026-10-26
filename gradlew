#!/usr/bin/env sh
# Bootstrap-Wrapper: laedt Gradle beim ersten Start selbst herunter.
GRADLE_VERSION=9.2.0
DIR="$(cd "$(dirname "$0")" && pwd)"
BOOT="$DIR/.gradle-bootstrap"
GRADLE_HOME="$BOOT/gradle-$GRADLE_VERSION"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  echo "Lade Gradle $GRADLE_VERSION herunter..."
  mkdir -p "$BOOT"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$BOOT/gradle.zip" || exit 1
  else
    wget -O "$BOOT/gradle.zip" "$URL" || exit 1
  fi
  unzip -q -o "$BOOT/gradle.zip" -d "$BOOT" || exit 1
  rm -f "$BOOT/gradle.zip"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
