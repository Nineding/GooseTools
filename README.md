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
- GooseTools 1.14.0 on both the server and every client

服务端和所有客户端必须安装完全相同的 GooseTools 版本。当前网络协议版本为 25；缺少模组、版本不同或协议不兼容的客户端会被服务器拒绝。

The server and every connecting client must use the exact same GooseTools version. The current network protocol is 25; clients with a missing, mismatched, or incompatible mod are rejected during login.

## 安装 / Installation

1. 安装 Minecraft 26.3、Fabric Loader 和上方列出的依赖。
2. 从 [Releases](https://github.com/Nineding/GooseTools/releases) 下载 `goosetools-1.14.0.jar`。
3. 将 GooseTools 及依赖 JAR 放入服务端和每位玩家客户端的 `mods` 目录。

Install Minecraft 26.3 with Fabric Loader and the dependencies listed above, then place the GooseTools JAR in the `mods` directory on both the server and every client.

## CustomSkinLoader 兼容 / Compatibility

GooseTools 已内置 GooseThings 动态皮肤与 CustomSkinLoader 的客户端兼容桥接，不再需要单独安装 `goosethings-csl-bridge`。客户端安装 CSL 时桥接会自动启用；未安装 CSL 时模块保持关闭，原版皮肤加载流程不受影响。CSL 不是 GooseTools 的必需依赖，也不应安装到专用服务端。

GooseTools includes the client-side compatibility bridge between GooseThings runtime skins and CustomSkinLoader, so the separate `goosethings-csl-bridge` mod is no longer needed. The bridge enables itself when CSL is present and remains inactive otherwise. CSL is optional and is not required on dedicated servers.

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

## 指令式名称标签图标 / Command-managed nametag icons

公开图标使用带名称的槽位保存；重复设置同一槽位会原地更新，不会堆叠。`order` 越小越靠左，序号牌固定绘制在所有公开图标之后。纹理必须是客户端资源包中安全的 `namespace:textures/...png` 路径，颜色使用六位十六进制 RGB。

Public icons use keyed slots, so setting the same slot updates it without duplication. Lower `order` values render farther left, and the serial badge follows all public slots. Textures must use a safe `namespace:textures/...png` path available in the client resource pack; colours are six-digit hexadecimal RGB values.

```mcfunction
goosetools nametags icon set <targets> <slot> <texture> <width> <height> <rgb> <order>
goosetools nametags icon remove <targets> <slot>
goosetools nametags icon clear <targets>

# Example: upsert a 10x10 trust icon in order position 300.
goosetools nametags icon set @s trust minecraft:textures/item/level_guest.png 10 10 FFFFFF 300
```

这些指令创建的是所有获准看到该名称标签的客户端都能看到的公开图标。涉及隐藏身份或仅特定观察者可见的信息，应继续使用服务端按观察者过滤的附件规则。指令槽位保存在内存中，数据包应在玩家重连或服务器重启后重新同步所需槽位。

These commands create public icons visible to every client allowed to see that nametag. Secret roles or viewer-private information must continue to use server-filtered attachment rules. Command slots are memory-only, so data packs should reapply desired slots after reconnects and server restarts.

## 版本与更新 / Versions and changes

发布记录及兼容性变化参见 [CHANGELOG.md](CHANGELOG.md)。修改源码、资源、配置、网络或行为时必须递增模组版本；破坏网络兼容时还必须递增协议版本。

See [CHANGELOG.md](CHANGELOG.md) for release notes and compatibility changes. Source, resource, configuration, network, or behavior changes require a new mod version; breaking network changes also require a protocol bump.

## 许可证 / License

GooseTools is licensed under [GPL-3.0-or-later](LICENSE).
