# pumlsrv
Small and efficient PlantUML HTTP Server

# Abstract

This is a small (fast) replacement of [Plant-UML Server](https://github.com/plantuml/plantuml-server) which can run without installing any other http servers.

Ease of use: the main settings can be configured on the main page and will be saved and restored on next startup.

Up to date: the newest plantuml*.jar is automatically downloaded. The download is saved to the current directory of pumlsrv.jar. The updates can be switched off or done manually - just copy a valid plantuml*.jar file to the same directory - zip files have to be unpacked first. 

![pumlsrv mainpage](mainpage.png "Main Page")

## Installation

`get.sh` downloads the release jar to `~/.local/share/pumlsrv` and creates a `pumlsrv` launcher in `~/.local/bin`. The download is verified against the sha256 digest that GitHub records for the release asset; the install fails on any mismatch.

```
curl -sSL https://raw.githubusercontent.com/michael72/pumlsrv/master/get.sh | bash
```

For scripted or reproducible installs, pin the version — and preferably also fetch the script itself from a tag or commit instead of `master`, so the executed code cannot change underneath you (available from tags newer than v2.1.1):

```
curl -sSL https://raw.githubusercontent.com/michael72/pumlsrv/<tag-or-commit>/get.sh \
  | PUMLSRV_VERSION=<tag> PUMLSRV_SHA256=<expected-jar-sha256> bash
```

Supported overrides:

- first argument or `PUMLSRV_VERSION` — release tag to install (default: latest)
- `PUMLSRV_SHA256` — expected sha256 of the jar; overrides the digest reported by the GitHub API
- `PUMLSRV_START` — `n` to never start the server after installing, `y` to always start; when unset, the script prompts on a terminal and does not start otherwise (e.g. when piped into `bash`)

## Usage

This http server runs on localhost on with the given port - default port is 8080. When using `h` parameter the options are shown: 

```
java -jar pumlsrv*.jar -h

Usage: pumlsrv [-cDhjLMnNruV] [--debug=<debugdir>] [-i=<include_file>] [PORT]
An efficient and small implementation of a PlantUML server.
      [PORT]           Port of the http server to connect to
  -c, --clear          Clear default settings (except used port)
  -D, --dark           Switch to dark mode
      --debug=<debugdir>
                       Directory to write debug files (incoming .puml and
                         rendered output) for each /plantuml/... request
  -h, --help           Show this help message and exit.
  -i, --include=<include_file>
                       Additional style to include for each UML
  -j, --nodynamicjar   Do not try to load the plantuml.jar dynamically.
  -L, --light          Switch to light mode
  -M, --monochrome     Switch to monochrome mode
  -n, --nosettings     Do not use and store current settings. By default the
                         last settings are saved and used on next startup
                         (without parameters).
  -N, --nobrowser      Do not show browser on startup. By default the browser
                         is opened on the current root page.
  -r, --reload         Reload the include file on every access
  -u, --noupdates      Do not check for updates of plantuml.jar and pumlsrv.
  -V, --version        Print version information and exit.
```

The main page that pops up (except when using `-N`) can be used to configure the settings. The settings configured here are automatically saved and restored on next startup.

### Debug Mode

The `--debug <debugdir>` option enables request/response logging for all `/plantuml/...` rendering requests. For each such request pumlsrv writes two files into `<debugdir>` (created automatically if it does not exist):

- `<yyyyMMdd-HHmmss>.puml` — the incoming PlantUML source
- `<yyyyMMdd-HHmmss>.<format>` — the rendered output (e.g. `.svg`, `.png`, …)

Example:

```
java -jar pumlsrv*.jar --debug /tmp/puml-debug
```

Also the environment variable `PUMLSRV_PORT`is checked and used when no parameter is given to configure the port.

The environment variables `HTTP_PROXY` and `HTTPS_PROXY` are checked when downloading updates behind a proxy.

Editors that can connect to the official PlantUML server - such as jebbs excellent PlantUML plugin running in Visual Studio Code or CodiMD- can simply connect to pumlsrv via http protocol.

Like the official Plant-UML server pumlsrv also supports the output types png, svg, eps, epstext and txt.

Enjoy!
