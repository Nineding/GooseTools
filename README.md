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
- GooseTools 1.14.0+Alpha0.25 on both the server and every client

服务端和所有客户端必须安装完全相同的 GooseTools 版本。当前网络协议版本为 29；缺少模组、版本不同或协议不兼容的客户端会被服务器拒绝。

The server and every connecting client must use the exact same GooseTools version. The current network protocol is 29; clients with a missing, mismatched, or incompatible mod are rejected during login.

## 安装 / Installation

1. 安装 Minecraft 26.3、Fabric Loader 和上方列出的依赖。
2. 从 [Releases](https://github.com/Nineding/GooseTools/releases) 下载 `goosetools-1.14.0+Alpha0.25.jar`。
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

## GUI 任务试玩 / GUI task trials

管理员可通过以下指令打开独立任务界面；玩家执行时 `@s` 指自己，控制台请使用玩家名或有效的玩家选择器。

Administrators can open independent task panels using the commands below. Players can use `@s`; consoles should use a player name or another valid player selector.

```mcfunction
/goosetools tasks open @s timing
/goosetools tasks open @s wires
/goosetools tasks open @s swipe
/goosetools tasks open @s garbage
/goosetools tasks open @s knobs
/goosetools tasks close @s
```

依次为指针转盘、接电线、刷卡、拖垃圾和旋钮校准。界面内显示操作说明；完成后可点击“再试一次”，ESC 可退出。试玩不暂停游戏，不接入地图，也不增加正式任务进度、成就或角色奖励。服务端负责判定完成，死亡、换维度、进入会议、断线或重新打开时清理旧会话。

The five trials are a timing dial, wire matching, card swiping, garbage dragging and knob calibration. Each panel displays instructions, offers replay after completion and closes with ESC. Trials do not pause the game, register map tasks or grant normal task progress, achievements or role rewards. The server confirms completion and clears the session on death, dimension changes, meetings, disconnect or replacement.

With Java 25 and a graphics-capable desktop session, `gradlew runTaskRegressionTest` exercises all five panels using real commands, network packets and mouse/keyboard handlers in a new isolated save. Screenshots and results are written under `build/task-regression-test`.

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

客户端默认自动检查并安装官方 GitHub Release，Alpha / Pre-release 更新也默认开启。Minecraft 加载后会单独显示检查、实际下载量、百分比和校验进度；失败时可以重试或继续游戏。更新设置可从客户端设置面板或 `/goosetools-update` 打开，也可以编辑 `config/goosetools/auto-update.properties` 中的 `enabled` 与 `includeAlpha`。对局中的后台检查不会打断界面或替换正在使用的模组。

HMCL 独立实例首次运行后会自动接入启动前更新，设置页也提供接入按钮。更新器包含在发布 JAR 中，无需另外下载；接入后 HMCL 会先打开独立进度窗口，检查、下载和安装完成才启动 Minecraft，新版在这次启动直接生效。原有启动前命令会保留，已有命令的实例需要自行合并；可以从 `.hmcl/config/instance-game-settings.before-goosetools.json` 恢复原配置。启动前命令使用 HMCL 的 `$INST_JAVA` 和 `$INST_MC_DIR`，仅连接当前实例。

未接入启动器或进入游戏后才发布的更新，仍在安全状态显示可取消的 10 秒倒计时，退出游戏后安装并重启。启动前更新保留旧 JAR，正常进入首个界面后确认成功；若新版未能完成启动，下次启动回退并跳过该版本。首次使用须安装一次带更新器的版本；专用服务器仍由管理员安排停服更新，客户端与服务端版本必须完全一致。

Clients check compatible official GitHub releases, including Alpha / Pre-release versions by default. A dedicated Minecraft screen displays checking, actual transfer progress, verification and results. HMCL instances with no existing pre-launch command connect automatically after their first run, or through the update settings button. The embedded standalone updater opens its own progress window and installs before HMCL starts Minecraft, so the new mod loads on that launch. Existing custom commands are preserved. Network failures offer retry or continuing with the installed version; unconfirmed startup rolls back on the following launch. Updates discovered after the game loads retain the cancellable restart flow. Dedicated servers remain administrator-managed, and the exact client/server version lock is preserved.

Each release uses a matching `v<mod_version>` tag and only the runtime JAR attachment. Tags containing `Alpha` publish as Pre-release, and the release body contains only that version's entry from `CHANGELOG.md`.

发布记录及兼容性变化参见 [CHANGELOG.md](CHANGELOG.md)。修改源码、资源、配置、网络或行为时必须递增模组版本；破坏网络兼容时还必须递增协议版本。

See [CHANGELOG.md](CHANGELOG.md) for release notes and compatibility changes. Source, resource, configuration, network, or behavior changes require a new mod version; breaking network changes also require a protocol bump.

## 许可证 / License

GooseTools is licensed under [GPL-3.0-or-later](LICENSE).
