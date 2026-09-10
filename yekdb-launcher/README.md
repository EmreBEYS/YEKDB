# YEKDB Launcher — Sprint 00–39

YEKDB Launcher is a separate JavaFX 21 application. It does not depend on or modify the YEKDB core source tree.

## Features

- NVIDIA App-inspired black/purple dashboard
- Latest GitHub Release lookup through the public GitHub API
- OS-specific release asset selection
- Download progress and temporary `.part` files
- ZIP path traversal protection and rollback-safe installation
- Installed/latest version comparison
- Launch control for a native executable or `yekdb.jar`
- Windows `jpackage` and macOS `jpackage` scripts
- A first-run bundled YEKDB core, so one installer provides both Launcher and YEKDB

## Development

Requirement: JDK 21. The included Maven Wrapper downloads the pinned Maven version on first use.

```shell
./mvnw clean test
./mvnw javafx:run
```

The default update repository and asset patterns are in `src/main/resources/launcher-config.properties`. Release files should follow these names:

- `yekdb-<version>-windows.zip`
- `yekdb-<version>-macos.zip`
- `yekdb-<version>-linux.zip`

Each archive should contain `yekdb.jar` at its root, or a platform executable at `bin/yekdb.exe`. The launcher installs files under `%LOCALAPPDATA%/YEKDB/runtime` on Windows and `~/.yekdb/runtime` elsewhere. Downloads are cached beside the runtime directory and removed after a successful install. Override the runtime path during development with:

```shell
java -Dyekdb.launcher.install-dir=/custom/path ...
```

## Packaging

Build the YEKDB core JAR from the repository root first. Then create a GitHub Release asset on Windows:

```powershell
.\scripts\package-yekdb-release.ps1 -Version 1.0.1
```

Build the native launcher installer on Windows:

```powershell
.\scripts\build-windows.ps1 -Version 1.0.0
```

On macOS:

```shell
./scripts/build-macos.sh dmg
```

`jpackage` must run on the target operating system; Windows installers are built on Windows and macOS images on macOS.

The Windows build uses the built-in Windows IExpress packager, so it does not require WiX. The generated single-file installer is `dist/YEKDB-Setup-<version>.exe` and presents a branded directory-selection wizard. The macOS build creates a native `.dmg` or `.pkg` with the same bundled Launcher + YEKDB core. Code signing/notarization credentials can be added later for public distribution.
