# GooseTools

[![Build](https://github.com/Nineding/GooseTools/actions/workflows/build.yml/badge.svg)](https://github.com/Nineding/GooseTools/actions/workflows/build.yml)

GooseTools 是 Whoiskiller 鹅鸭杀玩法使用的 Fabric 客户端/服务端基础模组。它提供双端版本握手、安全 UI、HUD 与名称标签、摄像机和视野效果，以及 Xaero 小地图/世界地图集成。

GooseTools is the required Fabric client/server foundation for the Whoiskiller Goose Goose Duck experience. It provides exact-version handshaking, secure UI, HUD and nametag features, camera and vision effects, and Xaero map integration.

## 安装要求 / Requirements

- Minecraft 26.3
- Java 25
- Fabric Loader 0.19.5 or newer
- Fabric API 0.161.0+26.3
- Xaero's Minimap 26.5.3
- Xaero's World Map 1.46.4
- GooseTools 1.11.0 on both the server and every client

服务端和所有客户端必须安装完全相同的 GooseTools 版本。当前网络协议版本为 21；缺少模组、版本不同或协议不兼容的客户端会被服务器拒绝。

The server and every connecting client must use the exact same GooseTools version. The current network protocol is 21; clients with a missing, mismatched, or incompatible mod are rejected during login.

## 安装 / Installation

1. 安装 Minecraft 26.3、Fabric Loader 和上方列出的依赖。
2. 从 [Releases](https://github.com/Nineding/GooseTools/releases) 下载 `goosetools-1.11.0.jar`。
3. 将 GooseTools 及依赖 JAR 放入服务端和每位玩家客户端的 `mods` 目录。

Install Minecraft 26.3 with Fabric Loader and the dependencies listed above, then place the GooseTools JAR in the `mods` directory on both the server and every client.

## 从源码构建 / Building from source

Windows:

```powershell
.\gradlew.bat clean test build
```

Linux/macOS:

```bash
./gradlew clean test build
```

构建产物位于 `build/libs/`。主模组 JAR 和 sources JAR 会分别生成。

Build artifacts are written to `build/libs/`, including the main mod JAR and a sources JAR.

## 版本与更新 / Versions and changes

发布记录及兼容性变化参见 [CHANGELOG.md](CHANGELOG.md)。修改源码、资源、配置、网络或行为时必须递增模组版本；破坏网络兼容时还必须递增协议版本。

See [CHANGELOG.md](CHANGELOG.md) for release notes and compatibility changes. Source, resource, configuration, network, or behavior changes require a new mod version; breaking network changes also require a protocol bump.

## 许可证 / License

GooseTools is licensed under [GPL-3.0-or-later](LICENSE).
