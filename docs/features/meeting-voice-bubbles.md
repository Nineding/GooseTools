# 内置会议语音气泡

集成版本：1.16.0+Alpha0.16。作为独立功能分支提交评审，尚未替换真实游戏文件。

来源：用户提供的“会议聊天气泡 PRO.jar”，mod id meetingvoice，版本 1.0.0-beta.9-dreamfix.1+26.3。提供文件与此前找回的梦境隐私修复 JAR 逐文件校验一致。基于找回的 MIT 原源码集成，保留原始材质与完整许可，代码位于 client/meetingvoice；许可见 licenses/MeetingVoice-MIT.txt 与包内 MeetingVoice-LICENSE.txt。

## 功能与使用

- 仅会议阶段、会议语音组 ggd_meeting 中显示薄荷色音量气泡；根据收到的声音强度绘制三条声柱，支持渐显/消退和明确断线图标。
- 原有 /meetingvoice status、test、reload、self on/off 命令保留。配置仍为 config/meetingvoice.properties，已有配置无需迁移。
- test 仅在有效会议语音组内模拟 15 秒气泡；showSelf 默认关闭，开启后在第三人称可见。
- 本地入梦、梦境地图人偶和活动梦境化身不显示真人语音气泡；真正的会议替身沿用原可见性规则。
- 仅使用已收到、未取消、可感知且未关闭音量的会议组声音。PCM 立即计算 RMS，不保存、录音或转发音频。切换世界/语音组/阶段时清除音量和动画缓存。
- Simple Voice Chat 为可选依赖：编译依赖官方 API，未打包语音模组本体；无语音模组时 GooseTools 可正常启动。
- 若独立 meetingvoice 模组仍安装，内置实现自动让位，避免两套气泡与命令冲突。改用内置气泡时移出独立 meetingvoice JAR，保留 Simple Voice Chat。
- 客户端/服务端仍按 GooseTools 原有要求使用一致版本；专用服务端不会注册客户端渲染或命令。

## 验证

Alpha0.16 发布前重新执行完整 test/build：4583 项测试零失败、零错误、零跳过；Xaero 与职业入场回归入口编译通过，运行 JAR 的版本／协议及 Changelog 经 release_info.py 核验。

下列隔离语音、GPU 与专服功能验收已在此前 Alpha0.13 本地测试包完成；本次未重复执行真人多人或完整 GPU 回归。

- 发布前整合主分支 3eb992e，保留训练假人临时外观与 Punchy 佩戴头颅表情更新。
- 4583 项单元测试通过，含配置保留、严格会议范围、声音状态代际清理和动画消退。
- 从原气泡测试迁移的 194 项隔离回归通过，覆盖语音事件、音量、取消事件、自身显示、断线和生命周期；额外检查旧 mod 共存与服务端环境门控。
- 梦境隐私 16 项专项检查通过。
- 无 Simple Voice Chat 的真实客户端启动及原 Xaero 回归通过。
- 安装 Simple Voice Chat 的真实客户端加载 goosetools_meetingvoice 插件成功，使用合成 PCM 驱动正式渲染并检查 GPU 截图。没有录制麦克风声音。
- 独立专用服务端成功加载语音插件，171 项原生服务器断言通过，无客户端类加载错误。
- 尚未进行多人服务器真实语音对话实测。