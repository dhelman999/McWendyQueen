#!/bin/sh
set -e

MVNW_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_DIR="$MVNW_DIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_PROPERTIES="$WRAPPER_DIR/maven-wrapper.properties"

if [ ! -f "$WRAPPER_PROPERTIES" ]; then
  echo "Error: $WRAPPER_PROPERTIES not found."
  exit 1
fi

WRAPPER_URL="$(sed -n 's/^wrapperUrl=//p' "$WRAPPER_PROPERTIES")"
if [ -z "$WRAPPER_URL" ]; then
  WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
fi

mkdir -p "$WRAPPER_DIR"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Downloading Maven wrapper jar from $WRAPPER_URL"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -qO "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "Error: curl or wget is required to download Maven wrapper jar."
    exit 1
  fi
fi

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_EXE="$JAVA_HOME/bin/java"
else
  JAVA_EXE="java"
fi

exec "$JAVA_EXE" \
  -classpath "$WRAPPER_JAR" \
  -Dmaven.multiModuleProjectDirectory="$MVNW_DIR" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
