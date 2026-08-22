#!/bin/bash
set -eu

REPO="michael72/pumlsrv"
DATA_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/pumlsrv"
BIN_DIR="${XDG_BIN_HOME:-$HOME/.local/bin}"
# Maven directory listing for the PlantUML jar (mirrors Download.kt).
PLANTUML_MAVEN_BASE="https://repo1.maven.org/maven2/net/sourceforge/plantuml/plantuml"

# Overrides for scripted / reproducible installs:
#   $1 or PUMLSRV_VERSION  release tag to install (e.g. v2.1.4); default: latest
#   PUMLSRV_SHA256         expected sha256 of the jar; overrides the digest
#                          reported by the GitHub API
#   PUMLSRV_START          "n" to never start the server, "y" to always start;
#                          unset: prompt on a terminal, don't start otherwise
VERSION="${1:-${PUMLSRV_VERSION:-}}"

# Check Java
if ! command -v java &>/dev/null; then
    echo "Error: java is not installed or not in PATH." >&2
    exit 1
fi

if command -v sha256sum &>/dev/null; then
    hash_file() { sha256sum "$1" | awk '{print $1}'; }
elif command -v shasum &>/dev/null; then
    hash_file() { shasum -a 256 "$1" | awk '{print $1}'; }
else
    echo "Error: sha256sum or shasum is required to verify the download." >&2
    exit 1
fi

if [ -n "$VERSION" ]; then
    echo "Fetching release info for ${VERSION}..."
    API_URL="https://api.github.com/repos/${REPO}/releases/tags/${VERSION}"
else
    echo "Fetching latest release info..."
    API_URL="https://api.github.com/repos/${REPO}/releases/latest"
fi
RELEASE_JSON=$(curl -fsSL "$API_URL")

VERSION=$(echo "$RELEASE_JSON" | grep '"tag_name"' | head -1 | sed 's/.*"tag_name": *"\(.*\)".*/\1/')
if [ -z "$VERSION" ]; then
    echo "Error: could not determine release version." >&2
    exit 1
fi

JAR_URL=$(echo "$RELEASE_JSON" | grep '"browser_download_url"' | grep '\.jar"' | head -1 | sed 's/.*"browser_download_url": *"\(.*\)".*/\1/')
if [ -z "$JAR_URL" ]; then
    echo "Error: could not find jar download URL in release." >&2
    exit 1
fi

# GitHub computes a digest for every uploaded release asset. The release
# carries a single jar, so the first digest belongs to it.
EXPECTED_SHA256="${PUMLSRV_SHA256:-$(echo "$RELEASE_JSON" | grep '"digest"' | grep -o 'sha256:[0-9a-fA-F]\{64\}' | head -1)}"
EXPECTED_SHA256="${EXPECTED_SHA256#sha256:}"
if [ -z "$EXPECTED_SHA256" ]; then
    echo "Error: no sha256 digest available for the jar (set PUMLSRV_SHA256 to provide one)." >&2
    exit 1
fi

JAR_FILE=$(basename "$JAR_URL")

echo "Installing pumlsrv ${VERSION}..."
mkdir -p "$DATA_DIR" "$BIN_DIR"

echo "Downloading ${JAR_FILE}..."
TMP_JAR=$(mktemp "${DATA_DIR}/.${JAR_FILE}.XXXXXX")
trap 'rm -f "$TMP_JAR"' EXIT
curl -fsSL -o "$TMP_JAR" "$JAR_URL"

ACTUAL_SHA256=$(hash_file "$TMP_JAR")
if [ "$(echo "$ACTUAL_SHA256" | tr 'A-F' 'a-f')" != "$(echo "$EXPECTED_SHA256" | tr 'A-F' 'a-f')" ]; then
    echo "Error: checksum mismatch for ${JAR_FILE}" >&2
    echo "  expected: ${EXPECTED_SHA256}" >&2
    echo "  actual:   ${ACTUAL_SHA256}" >&2
    exit 1
fi
echo "Checksum verified (sha256: ${ACTUAL_SHA256})"
mv "$TMP_JAR" "${DATA_DIR}/${JAR_FILE}"
trap - EXIT

# Download the latest PlantUML jar next to the pumlsrv jar, so the server has a
# renderer available on first start (it can also fetch this itself at runtime,
# so a failure here is only a warning). This mirrors the version detection in
# Download.kt: parse the Maven directory listing and pick the entry with the
# newest upload date.
PLANTUML_JAR=""
echo "Looking up latest PlantUML jar..."
PLANTUML_INDEX=$(curl -fsSL "${PLANTUML_MAVEN_BASE}/" 2>/dev/null || true)
PLANTUML_VERSION=$(printf '%s' "$PLANTUML_INDEX" |
    grep -oE '<a href="[0-9]+\.[0-9]{4}\.[0-9]+/"[^>]*>[^<]+</a>[[:space:]]+[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}' |
    sed -E 's#<a href="([0-9]+\.[0-9]{4}\.[0-9]+)/"[^>]*>[^<]+</a>[[:space:]]+([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2})#\2 \1#' |
    sort | tail -1 | awk '{print $3}')
if [ -z "$PLANTUML_VERSION" ]; then
    echo "Warning: could not determine latest PlantUML version; skipping." >&2
    echo "         pumlsrv will download it on first start unless disabled." >&2
else
    PLANTUML_JAR="plantuml-${PLANTUML_VERSION}.jar"
    if [ -f "${DATA_DIR}/${PLANTUML_JAR}" ]; then
        echo "Already have latest PlantUML jar ${PLANTUML_JAR}"
    else
        echo "Downloading ${PLANTUML_JAR}..."
        TMP_PLANTUML=$(mktemp "${DATA_DIR}/.${PLANTUML_JAR}.XXXXXX")
        trap 'rm -f "$TMP_PLANTUML"' EXIT
        if curl -fsSL -o "$TMP_PLANTUML" \
            "${PLANTUML_MAVEN_BASE}/${PLANTUML_VERSION}/${PLANTUML_JAR}"; then
            mv "$TMP_PLANTUML" "${DATA_DIR}/${PLANTUML_JAR}"
            trap - EXIT
        else
            echo "Warning: failed to download ${PLANTUML_JAR}; skipping." >&2
            echo "         pumlsrv will download it on first start unless disabled." >&2
            rm -f "$TMP_PLANTUML"
            trap - EXIT
            PLANTUML_JAR=""
        fi
    fi
fi

# Write the launcher script
LAUNCHER="${BIN_DIR}/pumlsrv"
cat > "$LAUNCHER" <<EOF
#!/bin/bash
cd "${DATA_DIR}"
java -cp "./${JAR_FILE}" com.github.michael72.pumlsrv.Main "\$@"
EOF
chmod +x "$LAUNCHER"

# Write a headless server launcher (handy for containers): start pumlsrv without
# update checks (-u), without storing settings (-n) and without opening a
# browser (-N), detached with output discarded. The port is taken from
# \$PUMLSRV_PORT, defaulting to 8080.
SERVER="${BIN_DIR}/pumlsrv-server"
cat > "$SERVER" <<EOF
#!/bin/bash
# Start pumlsrv in headless server mode (no updates, no stored settings, no
# browser), detached with output discarded. Port: \$PUMLSRV_PORT (default 8080).
"${LAUNCHER}" -u -n -N "\${PUMLSRV_PORT:-8080}" &> /dev/null
EOF
chmod +x "$SERVER"

# Install the pumlcli command-line client next to the pumlsrv launcher. It is
# a standalone bash script shipped in the repo; fetch it at the same ref we are
# installing so client and server stay in sync.
CLI="${BIN_DIR}/pumlcli"
echo "Installing pumlcli..."
if curl -fsSL -o "$CLI" \
    "https://raw.githubusercontent.com/${REPO}/${VERSION}/tools/pumlcli" ||
    curl -fsSL -o "$CLI" \
        "https://raw.githubusercontent.com/${REPO}/master/tools/pumlcli"; then
    chmod +x "$CLI"
else
    echo "Warning: failed to download pumlcli; skipping." >&2
    rm -f "$CLI"
    CLI=""
fi

echo "Installed:"
echo "  jar:      ${DATA_DIR}/${JAR_FILE}"
if [ -n "$PLANTUML_JAR" ]; then
    echo "  plantuml: ${DATA_DIR}/${PLANTUML_JAR}"
fi
echo "  script:   ${LAUNCHER}"
echo "  server:   ${SERVER}"
if [ -n "$CLI" ]; then
    echo "  cli:      ${CLI}"
fi

# Warn if BIN_DIR is not in PATH
if [[ ":$PATH:" != *":${BIN_DIR}:"* ]]; then
    echo ""
    echo "Note: ${BIN_DIR} is not in your PATH."
    echo "Add the following to your shell profile to use 'pumlsrv' directly:"
    echo "  export PATH=\"${BIN_DIR}:\$PATH\""
fi

echo ""
START="${PUMLSRV_START:-}"
if [ -z "$START" ]; then
    if [ -t 0 ]; then
        printf "Start pumlsrv? [Y/n] "
        read -r REPLY || REPLY=""
        START="${REPLY:-y}"
    else
        # Non-interactive install (piped stdin): don't start the server.
        START="n"
    fi
fi
if [[ "$START" =~ ^[Nn] ]]; then
    echo "You can start it later by running: pumlsrv"
else
    exec "$LAUNCHER"
fi
