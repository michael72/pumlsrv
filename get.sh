#!/bin/bash
set -eu

REPO="michael72/pumlsrv"
DATA_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/pumlsrv"
BIN_DIR="${XDG_BIN_HOME:-$HOME/.local/bin}"

# Overrides for scripted / reproducible installs:
#   $1 or PUMLSRV_VERSION  release tag to install (e.g. v2.1.2); default: latest
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

# Write the launcher script
LAUNCHER="${BIN_DIR}/pumlsrv"
cat > "$LAUNCHER" <<EOF
#!/bin/bash
cd "${DATA_DIR}"
java -cp "./${JAR_FILE}" com.github.michael72.pumlsrv.Main "\$@"
EOF
chmod +x "$LAUNCHER"

echo "Installed:"
echo "  jar:    ${DATA_DIR}/${JAR_FILE}"
echo "  script: ${LAUNCHER}"

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
