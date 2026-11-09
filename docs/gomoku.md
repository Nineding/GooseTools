# GUI 五子棋

从大厅／死亡玩家的背包右上角、Esc 菜单右上角、F9 或 `/gomoku` 打开。F9 可在按键设置的 GooseTools 游戏分类修改。普通玩家可使用 `/gomoku`，无需管理员权限或满血 DLC。

## 玩法

- 15×15，无禁手，黑棋先行；连续五子或更多获胜，棋盘填满而无人获胜则和棋。
- 每步 60 秒；关闭 GUI 只收起棋盘，计时继续。重新打开会恢复服务端最新棋局。超时、主动认输和进行中的断线判负。
- 玩家邀请必须主动接受，30 秒过期；每人最多参加一盘。首盘随机黑白，再来一局需要双方确认并交换颜色；重开请求 30 秒后撤销。
- 人机可选简单、普通、困难及黑白；默认普通、执黑。结算界面保留两分钟，供返回查看或再战。
- 不含悔棋、观战、排行、任务进度或奖励。

## 会话与权限

服务端使用现有 `ggdGameId` 与玩家标签判定。大厅等待者为一组；同局已死亡玩家和旁观者为另一组，两组不能相互邀请。死亡需要 `gamingGGD`、`spectator`、`deadInMap` 及当前会话身份；普通旁观者使用 `ggdLateSpectator`，不需要 `players` 或 DLC。

会议开始／结束及其跨维度传送不改变棋局或计时。开局准备、结算、复活、离开当前会话或临时离场会取消棋局，不记输赢；吞下、梦境和投影等状态不能借用死亡入口。服务器停止时取消所有棋局，重启不恢复。

管理员可用 `/goosetools gomoku open <players>` 或 `close <players>`。打开仍检查资格；关闭取消所选玩家的棋局，并让双方退出该局。管理员命令继承现有 `/goosetools` 权限。

`gomoku_action_v1` 和 `gomoku_view_v1` 独立于旧小游戏协议。服务端检查握手、成员、棋局编号、版本、单调消息序号、轮次、坐标、占位、截止时间和操作速率。所有状态变更在服务器线程；两个有界 AI 工作线程仅接收棋盘副本，返回后重新验证棋局版本。

客户端缓存与 Screen 分离。收到同步不会自动抢占 GUI；只有对应的打开请求或管理员打开可以显示棋盘，开局展示、结算等界面保持优先。Screen 不暂停 Minecraft，并复用 `ProtectedInputScreen` 阻止点击穿透。

## 验证入口

`./gradlew test build` 执行规则、会话、资格、AI 与编码测试及项目已有回归。JDK 25，Minecraft 26.3，具体前置版本见 `gradle.properties`。

`gomokuRegressionTest` 为单独测试模组，不打入运行 JAR。用三个终端启动：

```text
./gradlew runGomokuServerTest
./gradlew runGomokuClientTest -PgomokuRole=A
./gradlew runGomokuClientTest -PgomokuRole=B
```

测试目录分别是 `build/gomoku-server-test`、`build/gomoku-client-A`、`build/gomoku-client-B`。服务器需要在该隔离目录接受 Minecraft EULA，并设置 `server-ip=127.0.0.1`、`server-port=25586`、`online-mode=false`、`enforce-secure-profile=false`、`white-list=false`、`gamemode=creative`、`difficulty=peaceful`、`spawn-protection=0`。仅绑定回环地址，不能用于公网服。两客户端建议 `renderDistance:3`、`simulationDistance:3`、`pauseOnLostFocus:false`。

每次运行前清理 `build/gomoku-regression/*.txt` 的上次测试状态。自动流程通过真实握手、网络、鼠标坐标和原生 Screen 交互，结果与截图写入此目录；必须检查 `A-result.txt`、`B-result.txt` 和 `server-pass.txt`，不能仅以 Gradle 进程退出为依据。会话场景使用从当前数据包核对的标签／模式夹具，包含 DLC 冒险模式幽灵与原版旁观模式，未把整张生产地图或 GooseThings 加入测试服务器。

素材原图、PerfectPixel 工作流及记录见 [素材说明](art/gomoku/README.md)。

## Alpha0.3 实际验收（2026-11-09）

- `build` 成功，JUnit 4,604 项通过、0 失败；其中五子棋 17 项，覆盖规则、邀请、会话隔离、截止时间、再战、资格矩阵、AI 预算／取消及有界消息编解码。
- 一个独立专服与两个独立客户端实际以 `1.17.0+Alpha0.3 / protocol 50` 完成握手。两端结果及服务器均报告 PASS；专服正常停止，无客户端类加载错误。
- 使用 Minecraft 26.3 的真实左键常量完成邀请／接受、交替落子、双方棋盘一致、连五获胜、共同重开换色、F9 收起后超时、`/gomoku` 返回，以及断线判负。
- GUI 缩放 1／2／3、中文、获胜连线、三档 AI 合法应答、认输确认、背包原生按钮、普通旁观者 Esc 原生按钮均通过。菜单与获胜截图保留于素材目录。
- 当前数据包标签夹具验证：大厅、DLC 冒险模式幽灵、无 DLC 原版旁观者、会议前后／跨维度、复活、吞下、结算、开局准备、教学、会话切换及管理员开关。还直接发送了不符合资格的开局请求，服务端拒绝。
- 运行 JAR 已检查版本、双语键、材质及测试模组排除。测试没有启动完整生产地图流程；生产服务器和玩家实际对局未被中断，也未宣称已在运行中的旧版本热安装。
