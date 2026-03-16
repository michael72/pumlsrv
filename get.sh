#!/bin/bash
set -e

REPO="michael72/pumlsrv"
DATA_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/pumlsrv"
BIN_DIR="${XDG_BIN_HOME:-$HOME/.local/bin}"

# Check Java
if ! command -v java &>/dev/null; then
    echo "Error: java is not installed or not in PATH." >&2
    exit 1
fi

echo "Fetching latest release info..."
API_URL="https://api.github.com/repos/${REPO}/releases/latest"
RELEASE_JSON=$(curl -sSL "$API_URL")

VERSION=$(echo "$RELEASE_JSON" | grep '"tag_name"' | head -1 | sed 's/.*"tag_name": *"\(.*\)".*/\1/')
if [ -z "$VERSION" ]; then
    echo "Error: could not determine latest release version." >&2
    exit 1
fi

JAR_URL=$(echo "$RELEASE_JSON" | grep '"browser_download_url"' | grep '\.jar"' | head -1 | sed 's/.*"browser_download_url": *"\(.*\)".*/\1/')
if [ -z "$JAR_URL" ]; then
    echo "Error: could not find jar download URL in release." >&2
    exit 1
fi

JAR_FILE=$(basename "$JAR_URL")

echo "Installing pumlsrv ${VERSION}..."
mkdir -p "$DATA_DIR" "$BIN_DIR"

echo "Downloading ${JAR_FILE}..."
curl -sSL -o "${DATA_DIR}/${JAR_FILE}" "$JAR_URL"

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
printf "Start pumlsrv? [Y/n] "
read -r REPLY
REPLY="${REPLY:-y}"
if [[ "$REPLY" =~ ^[Nn] ]]; then
    echo "You can start it later by running: pumlsrv"
else
    exec "$LAUNCHER"
fi
