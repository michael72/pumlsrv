#!/bin/bash
#
# SessionStart hook for Claude Code on the web.
#
# The project targets Java 25 (see pom.xml: maven.compiler.source / jvmTarget).
# Remote web-session containers ship with an older default JDK, so this hook
# installs OpenJDK 25 and makes it the default for the session. Without it,
# `mvn test` compiles to Java-25 bytecode that the older forked test JVM cannot
# load ("class file version 69.0 ... only recognizes up to 65.0").
#
# Safe to run repeatedly: it only installs when JDK 25 is missing.
set -euo pipefail

# Only run inside Claude Code on the web (remote) environments.
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

JAVA25_HOME=/usr/lib/jvm/java-25-openjdk-amd64

SUDO=""
if [ "$(id -u)" -ne 0 ]; then
  SUDO="sudo"
fi

# Install OpenJDK 25 if it is not already available.
if [ ! -x "$JAVA25_HOME/bin/javac" ]; then
  echo "Installing OpenJDK 25 ..."
  export DEBIAN_FRONTEND=noninteractive
  # Refresh the package index first; the shipped index can be stale and 404 on
  # the pinned openjdk-25 version otherwise.
  $SUDO apt-get update
  $SUDO apt-get install -y openjdk-25-jdk-headless
fi

if [ ! -x "$JAVA25_HOME/bin/javac" ]; then
  echo "ERROR: OpenJDK 25 not found at $JAVA25_HOME after install." >&2
  exit 1
fi

# Make Java 25 the default for this session (mvn honours JAVA_HOME).
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    echo "export JAVA_HOME=$JAVA25_HOME"
    echo "export PATH=$JAVA25_HOME/bin:\$PATH"
  } >> "$CLAUDE_ENV_FILE"
fi

echo "OpenJDK 25 ready: $("$JAVA25_HOME/bin/java" -version 2>&1 | head -1)"
