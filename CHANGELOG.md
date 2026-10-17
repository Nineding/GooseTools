# Changelog

## 1.14.0+Alpha0.79 - 2026-10-18

- 满血 DLC 管家的守护对象铭牌新增守护之翼图标，仅管家和旁观者可见，旁观者关闭身份展示后仍可查看。
- 图标跟随实际守护对象，会议及梦境替身沿用同一标记；目标死亡、进入肚内或整局结束时隐藏，更换守护对象后自动更新。
- 需配套本次 Whoiskiller 数据包与资源包：DLC 开局及会后更换目标改用图标提示，新增守护对象与复仇之刃日志并向旁观者开放。
- 房主／服务端与所有客户端须同步升级并重启，网络协议保持 35。GooseThings 和现有铭牌配置无需更新。

## 1.14.0+Alpha0.78 - 2026-10-18

- 修复报告尸体时，尸体主人退出游戏便不播放报告动画的问题；现在按本局尸体编号读取保存的身份及尸体装备，离线后仍显示对应玩家。
- 离线尸体的动画模型复用当前连接中缓存的玩家皮肤；兼容普通报告、自动报告及没有地面尸体的强制报告。
- 房主／服务端与所有客户端须同步升级并重启，同时更新配套 Whoiskiller 数据包。网络协议保持 35，GooseThings、资源包和现有配置无需更新。

## 1.14.0+Alpha0.77 - 2026-10-17

- 所有旁观者现在都能看见麻雀干扰中的临时图标与成功干扰后的永久图标，不受旁观身份展示开关限制。
- 永久图标优先于临时图标；存活玩家仍只可查看本人持有或借用的麻雀技能标记，保留 DLC、教程及整局结束限制。
- 房主／服务端与所有客户端须同步升级并重启，网络协议保持 35。肚内连带干扰和日志修复需配套 GooseThings 1.14.0+Alpha0.59 及本次 Whoiskiller 数据包、资源包。

## 1.14.0+Alpha0.76 - 2026-10-17

- 伊格尔顿泉的满血 DLC 死亡旁观玩家现在可以领取并完成精工的土木、食品安全、核能、电气和通信委托；支持生前委托保留、鬼魂任务抽取及 NPC 附近的交互道具。
- 保留个人委托、服务端会话、会议与附身限制，完整通关才结算原有成就与四阶段任务权重；旁观鹅不能通过任务直接触发胜利。
- 房主／服务端与所有客户端须同步升级并重启，网络协议保持 35。需配套本次伊格尔顿泉精工数据包与资源包；GooseThings 无需升级。

## 1.14.0+Alpha0.75 - 2026-10-17

- 修复渡鸦和清醒梦者在会议梦境中仍能看见其他玩家说话或低语喇叭的问题，避免语音状态暴露梦境替身对应的玩家；醒来后恢复正常显示。
- 会议语音收听与发言保持原有规则，梦境提线操控期间同样隐藏喇叭。
- 房主／服务端与所有客户端须同步升级并重启，网络协议保持 35。兼容现有 GooseThings、数据包与资源包，无需调整配置。

## 1.14.0+Alpha0.74 - 2026-10-15

- 修复麻雀在开局身份展示中被识别成呆呆鸟的问题，正确显示麻雀名称、中立阵营与技能简介。
- 补齐赛后身份与结算展示的麻雀登记；海鸥借用麻雀技能后仍保留海鸥身份。
- 房主／服务端与所有客户端须同步升级并重启，网络协议保持 35。需配套麻雀数据包、资源包及 GooseThings 1.14.0+Alpha0.56。

## 1.14.0+Alpha0.73 - 2026-10-15

- 新增麻雀干扰铭牌图标：干扰中的听众显示临时图标，成功后显示持久图标；仅技能持有者可见，普通观战者不会收到该状态。
- 标记以真实目标和技能持有者的座位区分，海鸥借用与麻雀本体互不混用；补齐麻雀手动身份标记和赛后中立获胜展示。
- 房主／服务端与所有客户端须同步升级并重启，网络协议保持 35。需配套麻雀数据包、资源包及 GooseThings 1.14.0+Alpha0.56。

## 1.14.0+Alpha0.72 - 2026-10-15

- 网页商城可预览玩家本人游戏内实际使用的皮肤与 MVP 获胜动作，支持 CustomSkinLoader、皮肤站、本地皮肤和细手臂模型；换肤后自动刷新，不以官方皮肤代替已加载的自定义皮肤。
- 官网服务器可按需启用皮肤快照同步；仅同步当前连接玩家本人的已加载皮肤，采用受限 PNG 分块、校验和原子保存，不读取其他玩家皮肤或上传皮肤站配置。
- 服务端和客户端须同时升级并重启。网络协议保持 35；需配套网页商城预览更新及服务器皮肤快照目录配置，未启用的网站集成不采集皮肤。GooseThings、数据包和资源包无需随此功能更新。

## 1.14.0+Alpha0.71 - 2026-10-15

- 恢复大厅与会议中玩家铭牌旁的说话喇叭，停止说话后自动隐藏，并区分普通说话和低语。
- 喇叭随自定义铭牌一起排版、遮挡和淡出，会议替身按对应玩家的实时语音显示；尊重 Simple Voice Chat 的图标设置。
- 房主/服务端与所有客户端须同步升级并重启。网络协议保持 35；从 Alpha0.70 升级无需额外更新数据包、资源包或 GooseThings，更旧版本须满足 Alpha0.70 的配套要求。未安装 Simple Voice Chat 时不显示语音图标。

## 1.14.0+Alpha0.70 - 2026-10-15

- 死亡玩家附身旁观时保留自己的快捷栏、数字键与滚轮选择，并允许右键使用虚化物品退出和切换形态。
- 附身采用 Minecraft 原版摄像机；虚化沿用现有冒险模式穿墙与飞行，身体、皮肤、披风、盔甲和头颅随玩家移动。
- 房主/服务端与所有客户端须同步升级并重启，并更新配套 Whoiskiller 数据包、资源包及 GooseThings 1.14.0+Alpha0.55。网络协议保持 35。

## 1.14.0+Alpha0.69 - 2026-10-15

- 修复结算“顶级嘲讽”舞蹈中基础皮肤弯曲、3D 外层衣袖与裤腿却保持直立而导致的穿模；保留 SkinLayers3D 的立体厚度，让内外层和护甲同步弯曲。
- 修正弯曲部位的光照法线，保留材质颜色、纹理、光照与覆盖层属性；动作结束后普通人物渲染恢复正常。
- 房主/服务端与所有客户端须同步升级并重启。网络协议保持 35，Whoiskiller 数据包、资源包与 GooseThings 无需更新；未安装 SkinLayers3D 时仍支持原版皮肤外层。

## 1.14.0+Alpha0.68 - 2026-10-14

- 修复变形者、身份窃贼和寄生者变身后报告尸体或摇铃时，动画中的皮肤、铭牌与盔甲、头颅混用本人和目标外观的问题；默认统一展示报告者本人的完整外观。
- 配套数据包在鸭子全局独立设置中新增“报告／摇铃显示伪装外观”，默认关闭。开启后，正在变身的报告者或摇铃者统一展示变身目标的皮肤、盔甲、头颅和铭牌；未变身时仍显示本人。
- 房主/服务端与所有客户端须同步升级并重启，并更新配套 Whoiskiller 数据包及资源包。网络协议保持 35，会议动画数据格式不变；GooseThings 无需更新。

## 1.14.0+Alpha0.67 - 2026-10-14

- 修复管家在开局身份介绍、获胜阵营展示和赛后总结中被显示为普通鹅的问题，补齐身份名称、说明与鹅阵营颜色。
- 修复清醒梦者和巫医的存储身份编号与展示职业标签不一致，导致结算展示回退成普通鹅或鸭的问题；结算统一识别职业标签与翻译身份编号。
- 房主/服务端与所有客户端须同步升级并重启。网络协议保持 35，并修正安装包的协议元数据。Whoiskiller 数据包与 GooseThings 无需更新。

## 1.14.0+Alpha0.66 - 2026-10-14

- MVP 获胜动作增加账号商城接入：挥手默认免费，舞蹈 300 鹅鹅币，顶级嘲讽 4000 鹅鹅币；购买只解锁，选择随全服账号保存。
- 原“可爱舞”更名为“顶级嘲讽”，恢复原庆祝舞为独立的“舞蹈”，三个动作分别展示。
- 获胜动作设置加入价格提示；未拥有的付费动作显示网页商店链接，旧动作标签与伪造分数不能绕过拥有检查。
- 房主/服务端与所有客户端须同步升级并重启，网络协议由 34 升至 35；需配套本批地图数据包、商城目录及账号组件。付费动作仅官网服务器扩展开放，展示继续要求满血 DLC。

## 1.14.0+Alpha0.65 - 2026-10-13

- 胜利动作“庆祝舞”更新为“可爱舞”：按参考片段制作约 20 秒循环，包含脸旁手势、左右摆步、侧踢和交替抬腿，加入柔和起势与循环衔接；已选择舞蹈的玩家自动沿用。
- 总结人物增加手肘和膝盖弯曲，衣袖、裤腿与护甲跟随动作；按方块人物比例调整双手与头部的距离，并修正人物脚底与台面的偏移。舞蹈时四肢外层皮肤沿用原版贴图，头部与身体仍可使用 3D 皮肤层。
- 舞蹈仅用于赛后展示，保留披风自然下垂、挥手和两页总结交互，可随时翻页或关闭。原视频与配乐不包含在模组内。
- 房主/服务端与所有客户端须同步升级并重启。网络协议保持 34，Whoiskiller 数据包与 GooseThings 无需更新。

## 1.14.0+Alpha0.64 - 2026-10-13

- 赛后总结的“死亡修正”改为“死亡扣除”。经验增长增加原版经验球音效，升级播放升级音效；零经验和观战不播放增长音效，翻页或关闭立即停止。
- 总结改为两页：第一页点击“下一页”查看全员鹅鸭指数，按鹅、鸭、鸟阵营显示头像、玩家、身份、指数、原资源包评级与 MVP/SMVP 图标，并显示本人的本局任务完成数量；长名单支持滚动查看。
- 全员结果在结算时冻结，保留离线玩家与负数指数，不受大厅清理影响。左侧仍固定展示本局 SMVP、MVP 或无奖时的全场最高指数玩家。
- 修复底部按钮交互，第二页点击“关闭界面”直接退出，也可返回上一页；鼠标与键盘共用标准按钮，适配界面缩放。翻页不重复发奖、不提前触发身份晋升。
- 房主/服务端与所有客户端须同步升级并重启。总结数据通道升级为 v2，网络协议由 33 升至 34；沿用 Alpha0.63 的 Whoiskiller 数据包，GooseThings 无需更新。

## 1.14.0+Alpha0.63 - 2026-10-13

- 获胜阵营展示渐黑结束后立即淡入赛后总结，移除满血 DLC 的额外回厅倒计时等待；地图清理在总结背后继续，不再等待区块清理和实体死亡动画才显示界面。
- 左侧移除鹅头奖章，MVP、SMVP、鹅鸭指数评级及鹅鹅币统一使用原资源包图标，并移除标题下方“高光时刻，成长相伴”小字。
- 所有观看者统一展示本局 SMVP，否则展示 MVP；没有任何获奖者时固定展示全场鹅鸭指数最高者，同分按房间序号。右侧仍为观看者本人的奖励。
- 修复总结预览人物的披风因虚假移动速度而向上飘起，保留披风并以自然下垂姿态展示。
- 房主/服务端与所有客户端须同步升级并重启，搭配本次 Whoiskiller 数据包倒计时更新。网络协议保持 33；GooseThings 无需更新。

## 1.14.0+Alpha0.62 - 2026-10-13

- 满血 DLC 在获胜阵营展示后新增赛后总结：有 SMVP 固定展示 SMVP，否则展示指数最高的 MVP，同分按房间序号；没有奖项时显示普通本局回顾。
- 左侧使用玩家皮肤与服装播放挥手或庆祝舞，右侧展示个人经验增长、连续升级、等级上限储存经验，以及基础经验、荣誉加成、死亡修正和本局鹅鹅币，搭配新绘制的像素奖章与金币材质。
- 大厅客户端设置新增“胜利动作”，也可输入 `/goosetools-actions` 打开选择界面；选择会保存到服务器玩家数据，获奖时自动播放。总结动画可跳过，关闭后继续原有身份晋升展示。
- 奖励仍由数据包一次结算；总结仅读取本局快照，关闭或重复消息不会再次发奖。非 DLC 沿用原结算展示。
- 房主/服务端与所有客户端须同步升级并重启，搭配本次 Whoiskiller 数据包更新。新增兼容的总结通道，原网络协议保持 33；GooseThings 无需更新。

## 1.14.0+Alpha0.61 - 2026-10-13

- 满血 DLC 结算中的鹅阵营获胜大字改为沿用鹅阵营的绿色。
- 所有获胜标题下方的原因文字默认放大至原来的 1.6 倍，可使用屏幕 96% 的宽度；较长文案最多分成两行，避免挤压获胜玩家的名字和模型。
- 房主/服务端与所有客户端须同步升级并重启。网络协议保持 33，Whoiskiller 数据包与 GooseThings 无需更新。

## 1.14.0+Alpha0.60 - 2026-10-13

- 修复满血 DLC 开局转入黑屏遮住模式、地图及作者、阵营人数信息的问题，原聊天栏信息显示在黑屏之上；世界和旧身份展示继续被遮住。
- DLC 结算由当前画面约 1 秒渐暗至全黑，再淡出到结果界面；结果结束返回大厅时也从当前结算画面渐暗，避免突然跳黑或露出后方场景。
- DLC 结果展示前等待由 6 秒缩短至 3 秒，后续回大厅流程提前相应时间，结果展示时长保持不变；非 DLC 结算沿用原时序。
- 房主/服务端与所有客户端须同步升级并重启，搭配本次 Whoiskiller 数据包更新。网络协议保持 33，GooseThings 无需更新。

## 1.14.0+Alpha0.59 - 2026-10-13

- 修复满血 DLC 开局身份展示结束后短暂露出原展示场景的问题；舞台直接衔接不透明进图黑屏，落地准备完成后才揭开，开局仍为 7 秒展示加 3 秒转入。
- 满血 DLC 结算改为阵营背景的上下两排展示：上排未获胜参与者始终单行，人数越多模型与文字越小；下排放大获胜者，中间显示胜利标题和原因，模型上方显示彩色名字、序号与身份。鹅白、鸭红、中立黄、恋人粉，非 DLC 保留原结算形式。
- 开局与结算展示复制玩家所选盔甲和自己的头颅；死亡获胜者隐藏身体和皮肤外层，仅展示头颅及大衣。结算名单、装备和名牌在清理前固定，死亡队伍赢家正常显示。
- 房主/服务端与所有客户端须同步升级并重启，搭配本次 Whoiskiller 数据包更新；GooseThings 无需更新。开局装备通道升级为 v2，网络协议由 32 提升为 33。

## 1.14.0+Alpha0.58 - 2026-10-11

- 修复伊格尔顿泉满血 DLC 死亡鸭子无法接取、继续和完成保持绿灯、剪断电线及破坏净化激光 GUI 前置任务的问题。
- 配套数据包恢复死亡鸭子的配对控制器和入侵电力系统入口；有效幽灵状态、阵营、地图、会议与暂时离场校验仍然生效。
- 死亡鸭子仍不能接取或完成牺牲铃任务，也不能使用牺牲铃破坏；死亡玩家仍不分配系列任务。
- 房主/服务端和所有客户端须同步升级并重启，搭配本次 Whoiskiller 数据包更新。网络协议保持 32；GooseThings 无需追加更新，学校营火右键修复继续使用 Alpha0.52。

## 1.14.0+Alpha0.57 - 2026-10-11

- 开局身份展示延长 3 秒，满血 DLC 舞台与非 DLC 普通展示均由 4 秒延长至 7 秒。
- 配套 Whoiskiller 数据包将后续进图黑屏由 6 秒缩短至 3 秒，开局总时长仍为 10 秒；进图传送、显形、装备恢复、菜单解锁、技能计时和解除静音同步调整，结算黑屏时长不变。
- 房主/服务端和所有客户端须同步升级并重启，搭配本次 Whoiskiller 数据包更新。网络协议保持 32，GooseThings 保持 1.14.0+Alpha0.51。

## 1.14.0+Alpha0.56 - 2026-10-11

- 伊格尔顿泉新增精工系列正式任务：向精工领取随机专业委托，在土木、食品安全、核能、电气或通信点位靠近打开现有四阶段 GUI；可返回精工放弃。鹅阵营完整通关获得 4 点任务进度，鸭子与中立不增加该进度，中立放弃后不补发。
- 五种专业 GUI 接入服务端权威任务会话，只允许当前地图、当前玩家及对应精工委托发布完成结果；指令试玩继续保留。
- Xaero 标记配置支持自定义颜色分类及渐变，精工系列使用金绿渐变，原有系列保留蓝绿渐变。
- 配套数据包及资源包新增五项 Omega 精巧工匠成就、精工称号，以及专家在 250～500 级满足任意传奇配合或精工成就和 1000 成就点后自动突破老资历的方式。
- 房主/服务端和所有客户端须同步升级并重启，并安装本轮配套数据包、资源包及任务标记配置。网络协议保持 32；原专业题目和已有 GUI、身份展示、梦境功能保留，GooseThings 无需更新。

## 1.14.0+Alpha0.55 - 2026-10-11

- 修复寄生者在普通开局展示与满血 DLC 开局展示中缺少名字、颜色和序号的问题；展示模型使用独立名牌，局内寄生状态继续隐藏真实身体的名牌。
- DLC 两人、三人及恋人小队展示改为居中紧凑排列，减少模型之间的空隙。
- DLC 展示小人脚底固定贴合舞台地面，取消上下漂浮，并补充脚下接触阴影。
- 房主/服务端和所有客户端须同步升级并重启。网络协议保持 32，数据包与 GooseThings 无需追加更新；非 DLC 保留原开局展示形式。

## 1.14.0+Alpha0.54 - 2026-10-11

- 修复满血 DLC 开局展示小人缺少帽子、外套、袖子和裤腿皮肤外层的问题；已安装 3D Skin Layers 时，展示小人的立体外层不再因地图距离被降级。
- 开局展示名牌沿用玩家原有颜色和对应颜色序号，恋人名字沿用粉色规则；多人名单缩短名字时完整保留序号标记。
- 房主/服务端和所有客户端须同步升级并重启。网络协议保持 32，数据包无需追加更新，GooseThings 保持 1.14.0+Alpha0.51；非 DLC 身份展示保持原样。

## 1.14.0+Alpha0.53 - 2026-10-11

- 满血 DLC 开局改为聚光舞台身份展示，显示游戏模式、职业说明和玩家自己的 Minecraft 皮肤小人；非 DLC 保留原身份展示。
- 鹅使用白色舞台并展示本局全员；鸭使用红色舞台，仅展示本人、鸭队友和模仿者；中立统一黄色舞台，仅展示本人。名单由服务端按观看者过滤，不发送其他玩家的职业。
- 恋人统一使用粉色舞台，主要展示本人和伴侣；鸭恋人在旁边额外展示小比例鸭队友和模仿者，沿用原队友可见规则。
- 房主/服务端和所有客户端须同步升级并重启，搭配本次 Whoiskiller 数据包更新；GooseThings 保持 1.14.0+Alpha0.51。网络协议保持 32，新增独立身份展示通道，双端版本锁继续生效。

## 1.14.0+Alpha0.52 - 2026-10-10

- 修复旁观者看不见被静语玩家名牌旁静语图标的问题。满血 DLC 下，死亡旁观者（含冒险模式鬼魂和原版旁观）无需开启身份可见即可看到该图标；静语者与叼取该技能的海鸥显示规则不变，目标本人和其他存活玩家仍不可见。
- 房主/服务端与所有客户端须同步升级并重启。网络协议保持 32，已有 `nametag_attachments.json` 无需修改。GooseThings 仍仅由房主/服务端安装。

## 1.14.0+Alpha0.51 - 2026-10-11

- 修复入梦时会议本体冻结连带停止背包动画计时的问题，快捷栏物品更新后的弹跳正常结束；相同梦境装备不再每帧重建。
- 渡鸦飞行改为本地立即预测输入，服务端回传位置、速度和已处理输入序号后重演未确认输入；转向、升降与疾跑不再等待服务端位置回传和三帧插值。
- 服务端逐次处理飞行输入并保留权威速度、地图边界和技能判定；技能位移、乘坐载具、出梦与断线会切换或清除预测。
- 房主/服务端和所有客户端须同步升级并重启，Whoiskiller 数据包保持 Alpha0.50 的配套修复，GooseThings 保持 1.14.0+Alpha0.51。网络协议保持 32，新增梦境飞行状态通道；GooseThings 仍仅由房主/服务端安装。

## 1.14.0+Alpha0.50 - 2026-10-11

- 梦境移动使用连续位置插值，远距离技能位移保留即时切换；本地转头不再被服务端延迟角度覆盖，改善渡鸦飞行视角的顿挫。
- 修复提线操控时原版会议移动包和玩家姿态更新覆盖梦境镜像的问题；本体持续跟随梦境朝向、俯仰和姿态，关闭控制或出梦恢复会议姿态。
- 配套 Whoiskiller 数据包修复：普通举报、地点物品避开梦境按钮槽位，出梦与提线按钮按状态刷新，消除初次入梦时技能物品反复替换。
- 房主/服务端和所有客户端须同步升级并重启，并更新配套 Whoiskiller 数据包；GooseThings 保持 1.14.0+Alpha0.51。网络协议保持 32；GooseThings 仍仅由房主/服务端安装。

## 1.14.0+Alpha0.49 - 2026-10-11

- 修复创建梦境区块查看者时抢走玩家进度奖励执行者的问题：背包刷新仍由会议本体执行，入梦后正常替换出梦、提线操控和角色技能物品。
- 修复上述归属错误导致梦境技能右键无效，以及出梦、强制结束后整个背包替换继续失效的问题；结束梦境时也恢复进度奖励归属。
- 房主/服务端和所有客户端须同步升级并重启，Whoiskiller 数据包保持上一版配套修复，GooseThings 保持 1.14.0+Alpha0.51。网络协议保持 32；GooseThings 仍仅由房主/服务端安装。

## 1.14.0+Alpha0.48 - 2026-10-11

- 修复渡鸦入梦后的飞行过慢：恢复原先两倍飞行速度的加速、惯性、升降与疾跑，穿墙飞行不受地面和水流减速。
- 修复渡鸦与清醒梦者入梦时快捷栏显示锁在第一格的问题，九格物品、数字键选槽和滚轮选槽使用玩家真实背包。
- 梦中胡萝卜钓竿道具的右键直接进入道具使用，避免准星所指方块或实体吞掉醒来、技能和菜单按钮；瞄准信息取自梦境身体。
- 修复跨维度入梦仍从会议本体所在维度发送区块的问题；梦境区块、方块更新和光照使用梦境所在维度，避免地图错位与交互异常。
- 配套 Whoiskiller 数据包修复：强制停局在传送回厅和移除身份前关闭梦境，结算清除入梦状态及背包刷新锁，避免后续背包替换失效。
- 房主/服务端和所有客户端须同步升级并重启，配套更新后的 Whoiskiller 数据包，GooseThings 保持 1.14.0+Alpha0.51。网络协议保持 32；GooseThings 仍仅由房主/服务端安装。

## 1.14.0+Alpha0.47 - 2026-10-11

- 修复伊格尔顿泉死亡玩家无法接取、持续进行和完成普通 GUI 任务的问题。满血 DLC 任务鬼魂可进行试驾、管道疏通、校准电压、控电、街机爱好者和打地鼠；会议、入梦、吞食及其他不可操作状态仍会关闭任务。
- 街机爱好者的任务鬼魂可正常右键四台机器，机器位置与可交互资格由服务端统一校验。
- 鬼魂继续禁止接取系列任务与鸭子破坏前置 GUI，不会因普通任务的资格修复开放这些入口。
- 房主/服务端和所有客户端须同步升级并重启，配套本次 Whoiskiller 数据包修复和 GooseThings 1.14.0+Alpha0.51 的学校营火修复。网络协议保持 32；GooseThings 仍仅由房主/服务端安装。

## 1.14.0+Alpha0.46 - 2026-10-10

- 会议入梦改为独立的可见玩家身体：第一人称保留手与道具，第三人称可见完整皮肤、装备和动作；清醒梦者步行与碰撞、渡鸦飞行与穿墙由服务端控制。
- 入梦和醒来保留真实会议身体的实体与座位，不再移走本体或交接会议替身，消除这条交接路径造成的闪现；普通梦境输入与挥手不会驱动会议本体，渡鸦远程控制沿用既有规则。
- 梦境地图独立加载区块与实体，支持末地等跨维度地图；既有地图任务、梦境命中、伪装和醒来位置使用梦境身体。保留原有 GUI 任务资格限制及满血 DLC 门闩。
- 网络协议升级至 32。房主/服务端及所有客户端必须同步升级至本版，并配套 GooseThings 1.14.0+Alpha0.50 和更新后的 Whoiskiller 数据包；GooseThings 仍仅由房主/服务端安装。退出游戏后替换 JAR 并重启，无需安装 Freecam。

## 1.14.0+Alpha0.45 - 2026-10-10

- 修复伊格尔顿泉任务点离开所在区块后从 Xaero 小地图和世界地图消失的问题。客户端会记住已经收到的任务点，直到服务端明确移除，或玩家断线、重新进入世界。
- 房主/服务端与所有客户端须同步升级并重启。热替换客户端而不重连时，服务端会认为点已下发，新的缓存是空的。网络协议保持 31。GooseThings 仍仅由房主/服务端安装。配套数据包把伊格尔顿泉任务点改为固定坐标，修改后执行 /reload。

## 1.14.0+Alpha0.44 - 2026-10-10

- 修复净化仓激活按钮点按间隔超过 0.5 秒便清空进度的问题：连续点按累计五秒，中断时暂停并保存进度，再次点按继续累积。
- 激活界面显示暂停与进度保留提示；服务端只累计实际有效点按之间的时间，等待、按住和快速连发均不能跳过五秒操作。
- 房主/服务端与所有客户端须同步升级并重启。网络协议保持 31，保留 Alpha0.43 的角色标记以及净化仓、停电、21 人与下水道功能；继续使用现有配套数据包和资源包，GooseThings 仍仅由房主/服务端安装。

## 1.14.0+Alpha0.43 - 2026-10-10

- 会议私人标记目录补齐默剧演员、喜鹊、海鸥和管家。标记这些身份后，名牌旁与头颅槽会显示对应角色图标。
- 安装要求：房主/服务端与所有客户端同步更新至此版本并重启。网络协议保持 31；已含对应菜单选项的 Whoiskiller 数据包无需因本项再改。GooseThings 仍仅由房主/服务端安装。

## 1.14.0+Alpha0.42 - 2026-10-10

- 修复 Alpha0.41 更新遗漏净化仓功能导致操作台方块右键无响应、鸭子无法开始「破坏净化激光」的问题，恢复两个 GUI 入口及完整净化流程。
- 合并净化仓、停电、21 人与下水道功能；保留已有广播、剪线和街机音效。
- 房主/服务端与所有客户端须同步升级并重启；协议保持 31，继续使用现有配套数据包和资源包。GooseThings 仍仅由房主/服务端安装。

## 1.14.0+Alpha0.41 - 2026-10-10

- 伊格尔顿泉支持 21 人：4–8 人自动使用精简版，9–21 人开放下水道；21 人默认 14 鹅、5 鸭、2 鸟。
- 下水道的四处入口支持蹲下进入、到达出口自动返回；地下快捷栏地图使用 down_colored，回地面后恢复。
- 房间编号、会议提醒、私人玩家标记、伪装与梦境替身支持第 21 位玩家，配套数据包补齐出生分配、轮抽及 21 人会议流程。
- 客户端与服务端须同时升级，并安装配套 Whoiskiller 数据包和资源包；满血 DLC 功能需要房主/服务端 GooseThings 1.14.0+Alpha0.47。协议保持 31，GooseThings 仍仅由房主/服务端安装；替换 JAR 后重启。

## 1.14.0+Alpha0.40 - 2026-10-10

- 新增伊格尔顿泉停电状态识别，非鸭玩家的有限视野、停电辅助与观鸟限制与鹅教堂一致；工程师在管道内也受停电影响。
- 配套「入侵电力控制」解锁 inventory.15 的拉闸道具，两处各 8 灯共享维修，全部点亮后恢复视野并开始默认 90 秒破坏冷却；首次解锁默认无冷却。
- 补充鸭子前置任务与两处紧急维修点的 Xaero 名称及分类。客户端与服务端须同时升级，并使用配套 Whoiskiller 数据包和资源包；协议保持 31，无新增 GooseThings 依赖。

- 新增伊格尔顿泉净化仓绿色全息操作台：右击两个操作台方块，输入随机四位密码后连续点击五秒激活，服务端验证密码、点击间隔和实际游戏时间。
- 新增「破坏净化激光」反光镜 GUI：旋转三个镜面将光束反射回机关，完成后解锁「破坏净化仓」。破坏禁用净化仓六十秒，恢复后才开始六十秒共享冷却。
- 配套数据包净化仓支持拆除炸弹、解除临时心链及驱除并杀死附身者；正式恋人关系保留。需要配套 Whoiskiller 数据包和资源包，附身分支沿用角色本身的满血 DLC 与 GooseThings 要求。
- 客户端与服务端须同时升级到本版本。协议保持 31，沿用现有任务数据包格式；保留剪线、广播、保持绿灯与原有 GUI 功能。

## 1.14.0+Alpha0.39 - 2026-10-10

- 新增「剪断电线」GUI：拖动剪刀剪断四根电线，服务端校验剪线轨迹；正式任务绑定支持提前关闭重试、多人隔离和完成后解锁。
- 配套伊格尔顿泉广播开局可用，Xaero 小地图与世界地图显示广播位置图标；鸭子解锁「掐断广播」后可禁用广播 30 秒，恢复广播时才开始破坏道具的 45 秒共享冷却。
- 客户端与服务端须同时升级到本版本；协议保持 31。地图玩法需要配套 Whoiskiller 数据包和资源包，广播及其破坏前置需要满血 DLC 与房主/服务端 GooseThings。

## 1.14.0+Alpha0.38 - 2026-10-10

- 为飞鸟、贪吃蛇、乒乓、打地鼠、扫雷、2048 和躲车加入 18 种原创街机音效，使用 REAPER 分层混音并导出。振翅、吃食物、击球、敲击、翻格、插旗、滑动、合并、变道、刹车及得分拥有对应反馈，开始、暂停、继续、胜利和失误也有独立提示。
- 根据服务端确认的状态播放操作与结算反馈，重复状态及重试清零不再误播音效；飞鸟振翅保留即时反馈并避免回包重复播放。音效随 Minecraft 主音量和界面声音音量调整。
- 房主/服务端与所有客户端须同步更新并重启；网络协议保持 31，现有地图任务、数据包、资源包及 GooseThings 兼容性不变。音效随模组提供，无需额外安装资源包。

## 1.14.0+Alpha0.37 - 2026-10-10

- 新增「保持绿灯」GUI：用绿色画笔涂满三个红灯，服务端校验涂色覆盖并支持正式地图任务绑定。画笔使用像素风生图及 ComfyUI PerfectPixel 制作的透明材质。
- 伊格尔顿泉数据包可接入「召唤大运」：解锁后首次立即可用，使用后共享 45 秒冷却，货车沿指定路线在 6 秒内驶过并留下撞击尸体。
- 客户端与服务端须同时升级到本版本；协议保持 31。货车地图玩法需要配套更新的 Whoiskiller 数据包和资源包。

## 1.14.0+Alpha0.36 - 2026-10-10

- Added authoritative Cleaning GUI bindings for map tasks. Closing before completion fails the attempt; player and session isolation, server-validated success and completed-state preservation also apply to cleaning.
- Added configurable gradient task-marker backgrounds and the series category. Series markers use a blue-to-green background on both Xaero Minimap and World Map, with configuration reload and reconnect synchronization. Existing single-color configurations remain readable; explicit gold markers retain priority.
- Added default marker names for the three Eagleton Springs Wipe Glass locations. With the matching Whoiskiller data/resource pack, one equally weighted pool entry expands into three locations; all three must finish before the series awards two goose task-progress points and allows its normal replacement. Early closure retries only the current location.
- Install GooseTools 1.14.0+Alpha0.36 on the server/host and every client, with the matching data/resource pack, then restart. Protocol is now 31 because synchronized marker configuration supports new categories and gradient metadata; older clients must update. Minecraft 26.3, Xaero Minimap 26.5.3 and World Map 1.46.4 remain supported. No new GooseThings or Full Blood DLC requirement.

## 1.14.0+Alpha0.35 - 2026-10-09

- Added authoritative GUI map-task sessions for Eagleton Springs: hard Traffic Dodge starts immediately and succeeds after 30 seconds of active survival; hard Whac-A-Mole starts immediately and succeeds on the twentieth valid hit. Difficulty is locked, pause excludes time, and early loss, exit or restart fails the current attempt.
- Added bound Pipes, Knobs and Timing puzzle sessions, with server-validated completion and cancellation. Independent player/session binding prevents stale inputs, command trials and other players from awarding map progress. Datapack controls remain responsible for task assignment, map rules, normal completion and cleanup.
- Added four exact-block arcade entrances for 2048, Flappy Bird, Snake and Minesweeper in Eagleton Springs. The Arcade Fan task counts 30 seconds across station sessions; menus, pause, results and unrelated command trials do not count. Closing, changing machines and game loss preserve the accumulated time inside the arcade; leaving resets the attempt. Completed Arcade Fan games remain playable.
- Install GooseTools 1.14.0+Alpha0.35 on the server/host and every client with the matching Whoiskiller task data/resource pack, then restart. Protocol remains 30 and existing payload layouts are unchanged. Minecraft 26.3, Xaero Minimap 26.5.3, World Map 1.46.4 and existing GooseThings remain compatible; no new Full Blood DLC requirement. Existing professional trials, Traffic Dodge, power-station trial and InvMove input protection remain available.



## 1.14.0+Alpha0.34 - 2026-10-09

- Added four advanced, command-driven professional GUI trials: `/goosetools tasks open @s telecom`, `nuclear`, `foodsafety` and `civil`. Each has four stages, a Chinese/English knowledge handbook, explicit formulas and units, interactive instruments, numeric entry, draggable cards or diagrams, specific error feedback, preserved stage progress and replay with a new question.
- Telecom covers optical power/sensitivity/overload budgets, Shannon capacity versus equipment modulation and net rate, stable QPSK IQ calibration with identified pilots, and graph-based spectrum allocation. Nuclear covers separated PWR heat-transfer circuits, subcriticality and given decay heat, independent cooling branches tested through both single-branch outages, and integrated point-source dose planning. Food safety covers GHP/CCP controls, product cold-point D/z/F processing, raw/ready-to-eat/allergen separation, and mixed-lot traceability with the supplied hold/reject/validated-rework/release plan. Civil covers equal-leg leveling closure, simply supported beam forces and diagrams, stress/deflection/section selection, and SSD/OD moisture-corrected concrete batching with test-report acceptance. Equipment and acceptance limits are visible question data.
- All professional panels, handbooks, waiting and result states reuse movement-input protection. Optional InvMove late input remains blocked while a panel is open; closing restores ordinary inventory behavior without editing configuration. Original task/game types and customized textures remain available; profession dial and terminal textures can be overridden under `assets/goosetools/textures/gui/tasks/professions/`.
- Install GooseTools 1.14.0+Alpha0.34 on the server/host and every client, then restart. Protocol remains 30; earlier task, station and arcade payload layouts are unchanged, and professional state/input use separate bounded channels with server validation. Minecraft 26.3, Xaero Minimap 26.5.3, World Map 1.46.4 and existing GooseThings/data/resource packs remain compatible. These are standalone task trials with no map assignment, normal task-count changes, new achievements, or additional DLC dependency; Alpha0.33 traffic, electrical station and earlier features are retained.

## 1.14.0+Alpha0.33 - 2026-10-09

- Added Traffic Dodge, a standalone three-lane endless driving game with original pixel vehicles, scrolling roadworks, smooth lane changes, held braking, rising bounded speed, collision effects, three traffic difficulties, pause, replay and distance records. Try `/goosetools games open @s traffic`; A/D or Left/Right change lanes, S/Down brakes, and P pauses. The server owns collisions and scoring; every obstacle wave leaves an open lane and enough travel time to switch across the road.
- Added the advanced electrical station trial, `/goosetools tasks open @s powerstation`. Calculate three-phase current and apparent power, select capacity with the stated reserve, switch capacitor banks, match phase sequence/voltage/frequency/angle to close the synchronization breaker, and distribute six loads between A/B/C phases for stable supply. Instruments, editable numeric fields, keyboard dials, draggable load cards, error feedback and an in-panel formula handbook support all four stages. Random stations are solvable, and errors preserve completed stages.
- All ten task panels and seven arcade screens temporarily block player movement inputs while open, including menus, pause, results and the station handbook. Optional InvMove compatibility intercepts its late input update without changing configuration or adding a required dependency. Closing a panel immediately restores normal inventory movement according to the existing configuration.
- Install GooseTools 1.14.0+Alpha0.33 on the server/host and every client, then restart. Protocol remains 30; previous task and arcade packet layouts are unchanged, and station instrumentation uses separate bounded messages. Minecraft 26.3, Xaero Minimap 26.5.3, World Map 1.46.4, GooseThings 1.14.0+Alpha0.45 and existing data/resource packs remain compatible. Includes Alpha0.32's Silencer nametag update. The station remains a command trial without map-task progress; arcade activity remains available for the later one-minute task.

## 1.14.0+Alpha0.32 - 2026-10-09

- 静语者成功标记后不再发送命中 tellraw，改为在目标名牌旁显示静语者图标。图标仅对静语者、叼取该技能的海鸥以及具备身份查看权限的旁观者可见，目标本人不可见，并持续到本轮会议结束。
- 新增名牌附件 `silencer_pending` 与 `silencer_active`，使用 `minecraft:textures/item/ggd/silencer.png`。已有服务器须将这两条写入 `nametag_attachments.json` 并执行 `/goosetools nametags reload` 或重启。
- 安装要求：房主/服务端与所有客户端同步更新至此版本，并搭配配套 Whoiskiller 数据包。网络协议保持 30，双端版本锁保持生效。Minecraft 26.3、Xaero Minimap 26.5.3、World Map 1.46.4 与 GooseThings 1.14.0+Alpha0.45 保持兼容。

## 1.14.0+Alpha0.31 - 2026-10-09

- Added six standalone native arcade games: Flappy Bird, Snake, Pong against an AI, Whac-A-Mole, classic Minesweeper and 2048. Each has its own classic visual theme, original pixel artwork, animation, sounds, score records and replay controls. They are separate from task trials and do not award map-task progress, achievements or items.
- Open games with `/goosetools games open <players> <flappy|snake|pong|whack|minesweeper|2048>` and close them with `/goosetools games close <players>`. Flappy and Snake continue until collision (Snake wins by filling its board); Whac-A-Mole has three lives and endless waves. Pong offers first-to-11 matches and endless practice, Minesweeper has three difficulties and consecutive boards, and 2048 can continue beyond its victory tile.
- Server-owned simulation validates inputs, scores, collisions, mine reveals, tile merges and outcomes. Pause and result screens do not count as active play; long games have no fixed task-time or score cutoff. A server activity event is available for a future one-minute playing task. Opening a task or game replaces the previous session; disconnect, death, dimension changes and meetings clean it up.
- Install GooseTools 1.14.0+Alpha0.31 on the server/host and every client, then restart. Protocol remains 30; existing task payloads are unchanged and arcade messages use separate channels. Minecraft 26.3, Xaero Minimap 26.5.3, World Map 1.46.4, GooseThings 1.14.0+Alpha0.43 and existing data/resource packs remain compatible. No additional GooseThings or Full Blood DLC features are required. The original nine task trials and customized dial/knob textures are retained.

## 1.14.0+Alpha0.30 - 2026-10-09

- Added four independent GUI task trials: sort six Minecraft items into food/mineral/tool trays, repeat three rounds of flashing buttons, rotate a solvable 4x4 copper pipe board and test the water, and wipe six stains from a glass panel with a sponge. Category mistakes return the item; memory mistakes replay only the current round; pipes highlight disconnected/leaking segments; wiping keeps partial progress.
- Administrators can open these trials with `/goosetools tasks open <players> <sorting|memory|pipes|cleaning>`. The original five task IDs, replay and close controls remain available. All nine trials stay independent of maps, normal task progress, achievements and inventory contents.
- The server validates gestures, memory demonstration/input phases, pipe connectivity and brush coverage. GUI scaling uses the same coordinates for rendering and interaction. New panels reuse the iron pixel interface and vanilla item/copper/glass materials; existing customized dial and knob textures are retained.
- Install GooseTools 1.14.0+Alpha0.30 on the server/host and every client, then restart. Protocol increases from 29 to 30 for the expanded task state message; incompatible clients are rejected by the existing version handshake. Minecraft 26.3, Xaero Minimap 26.5.3, World Map 1.46.4, GooseThings 1.14.0+Alpha0.43 and the existing data pack remain compatible. No new GooseThings or Full Blood DLC features are required.

## 1.14.0+Alpha0.29 - 2026-10-08

- 喜鹊会议猜中图标仅向喜鹊和具备身份查看权限的旁观者显示，其他存活玩家不可见。已有服务器须将 nametag_attachments.json 的 magpie_guessed.viewer_tags_all 设置为 ["Magpie"]，并重新加载名牌配置。
- 渡鸦梦境保留断线宽限期内玩家的替身；目标死亡或淘汰后停止展示，断线不再直接移除仍有效的梦境目标。
- 安装要求：房主/服务端与所有客户端同步更新，并搭配 GooseThings 1.14.0+Alpha0.45 和配套 Whoiskiller 数据包。网络协议保持 29，双端版本锁保持生效。重启后生效。

## 1.14.0+Alpha0.28 - 2026-10-08

- 新增伊格尔顿泉·精简版的 Xaero 地图范围、八个区域名称与秘密实验室复合范围。
- 支持该地图的紧急会议标注和地图身份显示；专属会议室沿用会议前地图位置。
- 安装要求：房主/服务端与所有客户端同步更新至此版本，并搭配新增地图的数据包及资源包。网络协议未改变，双端版本锁保持生效。

## 1.14.0+Alpha0.27 - 2026-10-08

- HMCL pre-launch updates now check Windows file sharing before installation, including older clients that do not yet record their running process. An in-use GooseTools JAR leaves the installed mod untouched and asks the player to close the other game before retrying.
- Rollback preserves an intact old JAR when a failed replacement left it in place, so it can remove the staged new version without overwriting a file still used by another Minecraft process.
- Includes the dedicated Minecraft update screen and HMCL progress window from Alpha0.25, plus the task materials from Alpha0.26. Automatic updates and Alpha / Pre-release updates remain enabled by default. Once connected, HMCL installs compatible official updates before Minecraft starts, with checksum verification, backups and startup confirmation.
- Install GooseTools 1.14.0+Alpha0.27 once on every client and the server/host, then restart. Close any already running game before starting the HMCL update flow. Dedicated servers remain administrator-managed. Minecraft 26.3, Xaero Minimap 26.5.3, World Map 1.46.4, GooseThings 1.14.0+Alpha0.43 and the existing data pack remain compatible. Protocol remains 29; packet formats are unchanged.

## 1.14.0+Alpha0.26 - 2026-10-08

- Refined all five task trial interfaces with iron pixel textures, shaded frames, fasteners, connectors and buttons. Timing dials, access readers, bins and calibration knobs share custom sprites processed with ComfyUI PerfectPixel; animated indicators remain clearly visible.
- Garbage now uses Minecraft item rendering, including bottles, paper, bones and discarded food. Card swiping reuses Whoiskiller's existing key-card texture, bundled with GooseTools so the trial works without an external resource pack. Device textures use nearest sampling and are included in the JAR; playing requires no image generator or ComfyUI.
- Trial commands, task rules, difficulty and map independence remain unchanged. Install GooseTools 1.14.0+Alpha0.26 on the server/host and every client, then restart. Protocol remains 29; Minecraft 26.3, Xaero Minimap 26.5.3, World Map 1.46.4, GooseThings 1.14.0+Alpha0.43 and the existing data pack remain compatible.

## 1.14.0+Alpha0.25 - 2026-10-08

- GooseTools now opens a dedicated update screen after Minecraft loads, showing checking, actual download bytes and percentage, verification and results. Players can retry a failed check or continue playing. Checks during a match do not interrupt task screens or install files while the game is running.
- HMCL instances can install compatible official GitHub updates before Minecraft starts, including Alpha / Pre-release releases by default. The standalone progress window finishes its check and installation before HMCL launches the game, so an available compatible update takes effect on that launch without an extra game restart. Network failures allow retrying or continuing with the installed version.
- The independent updater is embedded in the runtime JAR and extracted automatically for HMCL instance settings. Existing launch commands are preserved; the update settings screen offers an HMCL connection button. The game shows the recent launcher check result instead of downloading the same update again.
- Pre-launch installation verifies the official SHA-256, mod identity and Minecraft/Fabric/dependency requirements, keeps the old JAR outside mods, and records startup confirmation. If the updated game never reaches its first screen, the next launch restores the previous version and skips the failed release. Running instances are protected from file replacement; client/server exact version matching still applies.
- Install GooseTools 1.14.0+Alpha0.25 once on every client and the server/host, then restart. HMCL pre-launch checks start on the following launch after connection; other launchers retain in-game automatic updates. Dedicated servers remain administrator-managed. Minecraft 26.3, Xaero Minimap 26.5.3, World Map 1.46.4, GooseThings 1.14.0+Alpha0.43 and the existing data pack remain compatible. Protocol remains 29; packet formats are unchanged.

## 1.14.0+Alpha0.24 - 2026-10-08

- Added five dedicated GUI task trials: a rotating timing dial, four-wire matching, timed card swiping, draggable garbage and three-knob calibration. Each panel includes localized instructions, progress, completion time, retry feedback and a replay button.
- Administrators can try them using `/goosetools tasks open <players> <timing|wires|swipe|garbage|knobs>` and close them with `/goosetools tasks close <players>`. These trials are independent of maps and do not award normal task progress, achievements or role rewards.
- The server owns trial layouts and completion rules. Closing the panel, disconnecting, dying, changing dimensions or entering a meeting clears the session; reopening replaces the previous trial. Trial panels do not pause the game.
- Install GooseTools 1.14.0+Alpha0.24 on the server/host and every client, then restart. Protocol increases from 28 to 29 for the task messages. Minecraft 26.3, Xaero Minimap 26.5.3, World Map 1.46.4, GooseThings 1.14.0+Alpha0.43 and the existing data pack remain compatible; the trials require no new GooseThings or Full Blood DLC features.

## 1.14.0+Alpha0.23 - 2026-10-07

- Automatic client updates now use existing Java HTTP/HTTPS proxy settings or the standard `HTTPS_PROXY` / `HTTP_PROXY` environment configuration, and respect `NO_PROXY`. This allows official GitHub release downloads through an already configured HTTP proxy without changing the system proxy or using unofficial mirrors.
- Automatic updates and Alpha / Pre-release updates remain enabled by default, with verified downloads, a cancellable restart countdown, old-JAR backups and startup-failure rollback. The updater can still obtain the connected server's exact compatible release.
- Install GooseTools 1.14.0+Alpha0.23 once on every client and the server/host, then restart to enable automatic updates. Dedicated servers remain administrator-managed. Minecraft 26.3, Xaero Minimap 26.5.3, World Map 1.46.4, GooseThings 1.14.0+Alpha0.43 and the current data pack remain compatible. Protocol remains 28; packet formats are unchanged. Older releases without automatic-update metadata are not automatically installed.

## 1.14.0+Alpha0.22 - 2026-10-07

- Clients automatically check official GitHub Releases, download compatible GooseTools updates, and install them after leaving the game or at a safe lobby transition with a visible, cancellable restart countdown. Automatic updates and Alpha / Pre-release updates are both enabled by default; players can change them in the client settings or with `/goosetools-update`.
- The version handshake can request the server's exact official release without weakening the client/server version lock. Updates verify the runtime JAR's SHA-256, mod identity, Minecraft/Fabric/dependency requirements and protocol before installation. Unavailable downloads leave the current game running and retry later.
- Installation runs from a separate helper JAR, backs up the old mod, and restores it if the updated client fails to reach startup. Failed versions are skipped on subsequent automatic checks. If automatic launch cannot be captured, installation still completes and the player can reopen the launcher.
- Tagged releases publish only the installable runtime JAR; Alpha versions are marked Pre-release, and release notes are taken directly from this version's Changelog entry.
- Install GooseTools 1.14.0+Alpha0.22 once on every client and the server/host, then restart to enable the updater. Dedicated servers do not automatically stop or update themselves. Minecraft 26.3, Xaero Minimap 26.5.3, World Map 1.46.4, GooseThings 1.14.0+Alpha0.43 and the current data pack remain compatible. Protocol remains 28; packet formats are unchanged. Older releases without automatic-update metadata are not automatically installed.

## 1.14.0+Alpha0.21 - 2026-10-07

- Blocked Xaero settings and waypoint menus now stay closed safely when a shortcut fires, including Right Shift. GooseTools intercepts the actual GUI entry before Xaero 26.5.3's faulty disabled-waypoint redirect can crash Minecraft.
- Server configuration screens remain blocked for administrators and ordinary players alike. The restricted Y-key player settings and minimap style screen remain available; other servers retain normal Xaero menu behavior.
- Install GooseTools 1.14.0+Alpha0.21 on the server/host and every client, then restart. Xaero Minimap 26.5.3, World Map 1.46.4, GooseThings 1.14.0+Alpha0.43 and the current data pack remain compatible. Protocol remains 28; packet formats are unchanged.

## 1.14.0+Alpha0.20 - 2026-10-07

- Sensor and Stalker highlights now render on retained and copied Sniper, Esper, Mime and Astral bodies, using the viewer's original player highlight team and colour even while the authority is hidden. Clearing the highlight removes the body's outline as well.
- The accompanying Whoiskiller data pack highlights an infested Parasite's current host and restores the original tracked player after release, including Seagull-borrowed Stalker tracking.
- Requires GooseTools 1.14.0+Alpha0.20 on the server/host and every client, plus the accompanying data pack. GooseThings 1.14.0+Alpha0.43 remains compatible; protocol remains 28 and packet formats and Full Blood DLC gating are unchanged. Restart after replacing the JAR.

## 1.14.0+Alpha0.19 - 2026-10-07

- Esper possession now leaves a dedicated client-side player body for its owner and other viewers. It no longer reuses the local player skipped by Minecraft when the camera follows the possession target, or retains the authoritative player hidden by the server.
- The body preserves its entry pose, including crouching, appearance and equipment. Active possession does not hide the body when the source overlaps it or its spectator update is delayed; the authoritative source is suppressed once the copy is ready to prevent duplicate heads, armour and nametags. Return retains the existing hand-off and Mime body-control behavior.
- Requires GooseTools 1.14.0+Alpha0.19 on the server/host and every client; GooseThings 1.14.0+Alpha0.43 and the current Whoiskiller data pack remain compatible. Protocol remains 28 and packet formats are unchanged. Restart after replacing the JARs.

## 1.14.0+Alpha0.18 - 2026-10-07

- When an Astral, possessed Esper, or scoped Sniper returns to the body being moved by Mime, the existing Mime session now continues controlling the returned player with its remaining duration unchanged. The target's input locks at hand-off instead of aborting Mime control.
- Return uses the body's latest controlled position and keeps its grace-period visual moving with Mime. Death, meetings, swallowing, disconnects, and invalid control relationships still terminate safely.
- Requires GooseThings 1.14.0+Alpha0.43 on the server/host and GooseTools 1.14.0+Alpha0.18 on the server and every client. Protocol remains 28; packet formats, data-pack commands, and Full Blood DLC gating are unchanged. Restart after replacing the JARs.

## 1.14.0+Alpha0.17 - 2026-10-07

- Fixed adventure no-clip retaining the player's old ground contact. Both sides clear ground contact before no-clip movement so projected and phasing players use airborne movement correctly.
- Added an explicit server-authorized forced-flight lock and speed synchronization. Role flight retains vanilla ascent/descent and configured speeds while preventing landing logic or double-tap jump from canceling flight mid-skill. Skill cleanup, respawn, and disconnect release the lock; ordinary ghost flight remains toggleable.
- Requires GooseThings 1.14.0+Alpha0.42 on the server/host and GooseTools 1.14.0+Alpha0.17 on the server and every client. Protocol increases from 27 to 28 for the new flight payload; older clients are rejected by the existing handshake. Data-pack commands and Full Blood DLC gating are unchanged.

## 1.14.0+Alpha0.16 - 2026-10-07

- Goose Chapel fire, GooseShip fire, and Poolcore stabilizer appearance mirrors now show the observing player's effective nametag identity together with their skin: name, wardrobe colour, serial badge, and identity attachments. Duck observers retain the normal labels selected by the data pack.
- Nametags read the same live GooseThings mirror scopes as skins and armor, so leaving a region, meetings, task completion, disconnects, and scope cleanup restore labels through the existing lifecycle.
- Requires GooseThings 1.14.0+Alpha0.41 on the server/host and GooseTools 1.14.0+Alpha0.16 on the server and every client. Network protocol remains 27; packet formats and Full Blood DLC gating are unchanged.

## 1.14.0+Alpha0.15 - 2026-10-07

- Mime now controls the retained body of an Astral Projection, active Esper possession, or scoped Sniper when that body is selected. The remote player authority remains at its soul, possession camera, or scope position instead of being teleported by Mime movement.
- Projected targets keep control of their remote state while Mime moves the body left behind. Ending that state returns the target to the body's latest controlled position and safely aborts Mime control.
- The controller sees one target-skinned local body while all other viewers receive the moving retained body, including synchronized turning, pose, swing, and walking motion. Ordinary Mime targets keep the existing input-lock and authority-transfer behavior.
- The server and every client must use GooseTools 1.14.0+Alpha0.15 together with GooseThings 1.14.0+Alpha0.40 and the accompanying Whoiskiller data pack. Network protocol remains 27 and packet formats are unchanged.

## 1.14.0+Alpha0.14 - 2026-10-06

- Restored the dedicated client-side player body at Lucid Dreamer and Raven meeting chairs. Dream entry no longer depends on retaining a server-hidden player entity, and wake-up again keeps the chair copy until the authoritative player has returned.
- Astral Projection now always renders its original body through a dedicated GooseTools player copy for other viewers instead of retaining the entity removed by server-side true invisibility. Dedicated servers, late viewers, and tracking rebuilds therefore keep the original position occupied without bringing back mannequins.
- Dream chair bodies and projection bodies have one visual owner each, preventing duplicate copies during staged enter, return, and heartbeat recovery. The server and every client must use GooseTools 1.14.0+Alpha0.14. GooseThings and the updated Whoiskiller data pack remain compatible; network protocol remains 27 and packet formats are unchanged.

## 1.14.0+Alpha0.13 - 2026-10-06

- Meeting report and bell banners now show the reporter or bell-ringer's real skin, name, wardrobe colour, and room-order badge while Morphling, Identity Thief, Parasite, or a Seagull-borrowed Morphling is transformed. The 3D models no longer inherit the live stolen PlayerInfo identity.
- Reported-body appearance is unchanged. Network protocol remains 27 and packet formats are unchanged.
- The server and every client must use GooseTools 1.14.0+Alpha0.13. GooseThings 1.14.0+Alpha0.34 and the updated Whoiskiller data pack remain compatible.

## 1.14.0+Alpha0.12 - 2026-10-06

- Fixed the active Mime body apparently teleporting beside its controller after running or jumping into a wall. Collision correctly left the body at the wall, but the general hand-off overlap guard hid its dedicated clone once the still-moving controller came within 1.5 blocks, exposing the controller-side player visual instead.
- Active moving Mime bodies are now exempt from proximity suppression and remain rendered at their server-authoritative collision position even when the controller catches up or overlaps them. Prepared, returning, and other stationary hand-offs keep their existing duplicate-render protection.
- The server and every client must use GooseTools 1.14.0+Alpha0.12. GooseThings 1.14.0+Alpha0.34 and the updated Whoiskiller data pack remain required. Network protocol remains 27 and packet formats are unchanged.

## 1.14.0+Alpha0.11 - 2026-10-06

- Fixed rapid opposite-direction Mime movement making the retained body oscillate. The client-only player clone now keeps one latest authoritative body sample and replaces an unplayed target instead of appending old directions to the normal sparse-network interpolation queue.
- Removed velocity-driven movement from the no-physics clone. Its coordinates now come exclusively from collision-resolved server body samples, while Minecraft's normal `RemotePlayer` tick still derives walking and running animation from the actual per-tick displacement; sustained wall input can no longer push the visual body through a block.
- Replaced the retained body's full-height step attempt with vanilla-style collision-shape candidate heights. Every direct or stepped result is checked before commit, the last collision-free position is restored if overlap is detected, and a pose expansion is deferred when its larger bounding box would enter a solid block.
- The server and every client must use GooseTools 1.14.0+Alpha0.11. GooseThings 1.14.0+Alpha0.34 and the updated Whoiskiller data pack remain required. Network protocol remains 27 and packet formats are unchanged.

## 1.14.0+Alpha0.10 - 2026-10-06

- Completed the moving Mime clone hand-off by explicitly unloading the previously retained authoritative remote-player instance when control becomes active. The stale instance can no longer overlap the clone, suppress its render, or reintroduce server-position/body-position contention.
- Includes the restored 1.14.0-era movement, jump, local gravity/collision, and normal RemotePlayer animation behavior introduced during the Alpha0.9 repair.
- The server and every client must use GooseTools 1.14.0+Alpha0.10. GooseThings 1.14.0+Alpha0.34 and the updated Whoiskiller data pack remain required. Network protocol remains 27 and packet formats are unchanged.

## 1.14.0+Alpha0.9 - 2026-10-06

- Restored the original GooseTools 1.14.0-era Mime body-control behavior: each plausible one-tick controller displacement is mirrored, a grounded-to-airborne positive Y transition copies the real jump impulse, and the retained body then resolves its own gravity, block collision, stairs, ceiling, and landing at its original location.
- Active Mime bodies now always render through a dedicated client-only player clone instead of reusing the authoritative remote-player entity. Server movement updates can no longer fight body pinning and make the torso, limbs, or cape vibrate.
- Removed the synthetic walk-cycle override. The player clone now uses Minecraft's normal remote-player position/rotation interpolation, velocity, avatar state, and entity animation calculation, so walking, running, jumping, body turns, and cape motion are derived from actual body movement.
- The server and every client must use GooseTools 1.14.0+Alpha0.9. GooseThings 1.14.0+Alpha0.34 and the updated Whoiskiller data pack remain required. Network protocol remains 27 and packet formats are unchanged.

## 1.14.0+Alpha0.8 - 2026-10-06

- Fixed Mime retained bodies gliding while their coordinates were mirrored. Walking and running limb motion now use the body's actual post-collision horizontal displacement with vanilla-equivalent target speed, smoothing, phase progression, and partial-tick interpolation.
- A moving body transitions from walking to running amplitude as its resolved speed increases, then eases naturally to a stop when movement ends or its local collision blocks it. Turning, pose, and hand-swing synchronization are unchanged.
- The server and every client must use GooseTools 1.14.0+Alpha0.8. GooseThings 1.14.0+Alpha0.34 and the updated Whoiskiller data pack remain required. Network protocol remains 27 and packet formats are unchanged.

## 1.14.0+Alpha0.7 - 2026-10-06

- Fixed Mime retained bodies mirroring only animation while remaining stationary. The body now copies the controller's plausible per-tick horizontal displacement from its own starting position.
- Mirrored movement is resolved against blocks at the retained body's location, including player-height step-up handling and the independent gravity introduced in Alpha0.6. Teleports and the initial hand-off displacement are deliberately rebased instead of moving the body across the map.
- The server and every client must use GooseTools 1.14.0+Alpha0.7. GooseThings 1.14.0+Alpha0.34 and the updated Whoiskiller data pack remain required. Network protocol remains 27 and packet formats are unchanged.

## 1.14.0+Alpha0.6 - 2026-10-06

- Astral, Sniper, Esper, and Mime retained bodies now use server-authoritative vertical gravity and block collision. Their invisible spatial anchor, aim geometry, and final return position follow the body to its landed height instead of remaining suspended at the projection entry point.
- A Mime controlling another player now mirrors the controller's turning, pose, walking phase, and hand swings on the retained body while its position stays at the original body location.
- Esper and Mime cleanup now lets their legacy authority manager finish first, then returns to the current retained-body position so an airborne entry coordinate cannot overwrite the landed height.
- The server and every client must use GooseTools 1.14.0+Alpha0.6. GooseThings 1.14.0+Alpha0.34 and the updated Whoiskiller data pack are required. Network protocol is now 27 because retained-body motion uses a new bounded payload.

## 1.14.0+Alpha0.5 - 2026-10-06

- Restored Lucid Dreamer and Raven Dream Remote Control after the projection-body migration. Turning, head/body rotation, pose, and hand swings now target the retained meeting body instead of the removed legacy meeting proxy.
- Motion updates received during the staged body hand-off are retained and applied as soon as the projection body is ready, while the body position and walking effects remain frozen at its meeting chair.
- The server and every client must use GooseTools 1.14.0+Alpha0.5. GooseThings 1.14.0+Alpha0.34 and the accompanying Whoiskiller data pack remain compatible. Network protocol remains 26 and packet formats are unchanged.

## 1.14.0+Alpha0.4 - 2026-10-06

- Restored the seated meeting-chair render pose for Lucid Dreamer and Raven Dream projection bodies.
- Projection bodies now clear stale walk/sprint animation and suppress sprint particles while pinned, preventing remote dream movement from appearing on the body left in the meeting.
- The server and every client must use GooseTools 1.14.0+Alpha0.4. GooseThings 1.14.0+Alpha0.34 and the accompanying Whoiskiller data pack remain compatible. Network protocol remains 26 and packet formats are unchanged.

## 1.14.0+Alpha0.3 - 2026-10-06

- Replaced Astral, Sniper, Esper, Mime, Lucid Dreamer, and Raven Dream visible mannequin hand-offs with retained player bodies and viewer-private client projections. The authoritative player continues to own controls at the remote projection while normal viewers keep the original player render fixed at the entry point.
- Added invisible server marker anchors for data-pack range, area, tracking, and aim validation. Full Blood aim claims resolve retained body hits back to the matching anchor without exposing a visible fake entity.
- Added staged prepare/commit/return synchronization, heartbeat recovery, late-join fallback bodies, duplicate-spawn suppression, and return grace so pose, cape, equipment, and animation state no longer reset during body/projection transitions.
- The server and every client must use GooseTools 1.14.0+Alpha0.3. GooseThings 1.14.0+Alpha0.34 and the accompanying Whoiskiller data pack are required. Network protocol is now 26 because the bounded projection-body payload is new.

## 1.14.0+Alpha0.2 - 2026-10-05

- Magpie's meeting guess-success icon no longer appears on the guessed living player's own nametag in third person. Other players, spectators, and the Magpie still see the public living-target mark.
- The server and every client must all use GooseTools 1.14.0+Alpha0.2. GooseThings and the Whoiskiller data/resource packs do not require changes; network protocol remains 25 and packet formats are unchanged.

## 1.14.0+Alpha0.1 - 2026-10-04

- Dream stand-ins now verify both their entity-ID registration and their presence in the client's active rendering collection. An unload callback invalidates stale models immediately, while unloaded far-away chunks wait until visible instead of repeatedly publishing unusable entities.
- The server now heartbeats each viewer-private dream scene at least once per second even when its contents are unchanged. A client that loses a meeting proxy during a world, chunk, or render-section transition can therefore reconstruct the full scene instead of remaining on an empty chair indefinitely.
- Added rate-limited recovery diagnostics for missing render registrations and rejected synthetic entities. The server and every client must all use GooseTools 1.14.0+Alpha0.1. GooseThings and the Whoiskiller data/resource packs do not require changes; network protocol remains 25 and packet formats are unchanged.

## 1.14.0 - 2026-10-04

- Consolidated every GooseTools change from 1.13.0 through 1.13.0+Alpha0.29 into the 1.14.0 stable release.
- Added Goose Goose Duck-style animated meeting alerts for body reports, emergency bells, and sacrifice bells, with privacy-filtered player appearance, equipment, names, serial badges, localized text, movement locking, and corrected corpse presentation.
- Expanded server-authoritative nametags and meeting markers with keyed public icon slots, trust/readiness/authority indicators, private faction, group, role, and action cards, viewer-specific outline colors, Full Blood spectator status icons, and retained Seagull borrowed-role visibility.
- Integrated optional CustomSkinLoader compatibility and completed local disguise presentation for Morphling, Identity Thief, Parasite, Seagull-borrowed Morphling, and Mime, including first-person arms, third-person skin layers, capes, hats, nametags, and identity-bound icons.
- Added persistent per-player AI report history, a report-selection screen, safe inline emphasis colors, deferred screen opening, configurable retention, and fixes for mouse input across AI and server-provided web interfaces.
- Replaced visible dream mannequins with viewer-private client stand-ins for meetings, frozen bodies, and dream corpse copies. Staged handoffs, resolved skins, synchronized pose/look/swing state, marker outlines, unload recovery, and render blocking prevent real-body, default-skin, origin, and empty-chair flashes.
- Added the required Mime remote-control client synchronization, private controller appearance, input/view/hotbar restrictions, bounded packets, and disconnect cleanup.
- Added optional, spoiler-safe CraftPresence placeholders for public map and coarse match phase, and replaced retired encyclopedia pages with an exact HTTPS allowlist for the official role, faction, game-mode, and website guides.
- Added server-authoritative Adventure no-clip for Raven Dream, Raven Moment, Astral Projection, Sniper scope, and Phoenix Moment without Spectator spoofing, including lifecycle cleanup and fixes for wall pushback and incorrect swimming/crawling poses.
- The server and every client must all use GooseTools 1.14.0. GooseThings 1.14.0+Alpha0.31 and the accompanying Whoiskiller data/resource packs are compatible. Network protocol remains 25; this stable release does not change the Alpha0.29 packet formats.

## 1.13.0+Alpha0.29 - 2026-10-04

- Fixed Adventure no-clip players being forced into the swimming/crawling pose inside low ceilings or solid blocks. Client and server now treat pose dimensions as collision-free only while Adventure no-clip is active, allowing vanilla to retain the correct desired pose without affecting normal movement or legitimate swimming and sleeping poses.
- The server and every client must all use 1.13.0+Alpha0.29. GooseThings 1.14.0+Alpha0.29 and the accompanying Whoiskiller data pack remain compatible and do not require changes. Network protocol remains 25 and packet formats are unchanged.

## 1.13.0+Alpha0.28 - 2026-10-04

- Fixed Adventure no-clip players being pushed back by walls or requiring sustained movement to slowly cross a block. The client now disables vanilla wall-escape handling before movement input is processed, and the server reasserts no-physics at the movement boundary before collision validation.
- The server and every client must all use 1.13.0+Alpha0.28. GooseThings 1.14.0+Alpha0.29 and the accompanying Whoiskiller data pack remain compatible and do not require changes. Network protocol remains 25 and packet formats are unchanged.

## 1.13.0+Alpha0.27 - 2026-10-04

- Added server-authoritative Adventure no-clip without Spectator mode spoofing. Raven Dream, Raven Moment, Astral Projection, Sniper scope, and Phoenix Moment can now fly through blocks while retaining normal Adventure interaction and rendering.
- Added `/goosetools noclip <players> <true|false>` plus disconnect, respawn, and server-shutdown cleanup. The server and every client must all use 1.13.0+Alpha0.27 with GooseThings 1.14.0+Alpha0.29 and the accompanying Whoiskiller data pack. Network protocol remains 25; the packet set adds the bounded `adventure_noclip_s2c_v1` payload.

## 1.13.0+Alpha0.26 - 2026-10-04

- AI match reports now render the server-validated fixed emphasis palette inline, so key turns, correct play, mistakes/danger, player identities, and uncertainty can be scanned quickly without allowing arbitrary formatting.
- Existing plain reports remain compatible. The server and every client must all use 1.13.0+Alpha0.26 with GooseThings 1.14.0+Alpha0.27 for colored AI emphasis. Network protocol remains 25 and the report JSON packet shape is unchanged.

## 1.13.0+Alpha0.25 - 2026-10-04

- When Full Blood DLC role display is enabled, every death or lobby spectator now sees the active status icons on living players: Pigeon infection, Detective result halos, Clown balloons, Gravy bounty, Witch Doctor curse targets (including Seagull borrowing), Guard shields, Lover hearts, Broker shackles, Magpie guesses, and configured nametag attachments.
- Spectator status icons remain visible through meetings while their underlying tag or score still exists, then disappear on the next nametag snapshot after the state is cleared. Living-player role privacy and temporary non-spectator views remain unchanged.
- The server and every client must all use 1.13.0+Alpha0.25. Network protocol remains 25 and packet formats are unchanged.

## 1.13.0+Alpha0.24 - 2026-10-04

- Dream stand-ins now detect when Minecraft has independently unloaded their synthetic client entity and recreate it with a collision-free local ID. This restores the meeting chair proxy plus Raven living-player and corpse-location bodies instead of retaining a permanently detached model.
- Every active dream session now sends its meeting proxy to the entering player's own client as well as other viewers. New and recovered proxies remain render-blocked until their position, seated pose, equipment, resolved skin, nametag alias, and marker glow are all ready, preventing origin, standing-body, default-skin, and real-body flash frames.
- The server and every client must all use 1.13.0+Alpha0.24 with GooseThings 1.14.0+Alpha0.26. Network protocol remains 25 and packet formats are unchanged.

## 1.13.0+Alpha0.23 - 2026-10-03

- A controlled player's client now suppresses the complete Mime controller entity before the controller is moved into the target's position, preventing a one-frame head, armour, held-item, or nametag flash.
- Mime control synchronization is sent before the server-side body handoff. Clients and the server must all use 1.13.0+Alpha0.23 with GooseThings 1.14.0+Alpha0.24 and the accompanying Whoiskiller data pack. Network protocol remains 25 and packet formats are unchanged.

## 1.13.0+Alpha0.22 - 2026-10-03

- Dream stand-ins now reuse the source player's already-resolved client skin and model layers, preserving CustomSkinLoader skins even though each stand-in has a viewer-private fake UUID.
- Meeting proxies are fully created by the prepare packet and locally replace the real chair occupant in one render decision. Entry no longer exposes the real body or an empty chair for a frame, while wake keeps the proxy until the restored player has arrived.
- Clients and the server must all use 1.13.0+Alpha0.22. Network protocol remains 25 and packet formats are unchanged.

## 1.13.0+Alpha0.21 - 2026-10-03

- Added an optional CraftPresence integration with localized `ggd.activity`, `ggd.state`, `ggd.map`, `ggd.phase`, and `ggd.server` placeholders for Discord Rich Presence. GooseTools continues to run normally when CraftPresence is absent.
- The server now synchronizes only the selected public map and a coarse phase: lobby, preparing, playing, meeting, spectating, results, or tutorial. Roles, factions, death details, and other spoiler-sensitive state are never included.
- Clients and the server must all use 1.13.0+Alpha0.21. CraftPresence remains optional and client-only. Network protocol remains 25; the packet set adds the bounded `game_presence_state_s2c_v1` payload.

## 1.13.0+Alpha0.20 - 2026-10-03

- Seagulls now retain the nametag visibility granted by a borrowed role until the next meeting ends or another ability is taken. This covers Detective inspection halos, Pigeon infection badges, Clown balloons, Gravy bounties, Guard shields, and Broker shackles.
- Borrowed Detective, Pigeon, and Clown results keep private Seagull-only visual snapshots through the meeting even when the base role state is cleared earlier; gameplay state and ownership are unchanged.
- Clients and the server must all use 1.13.0+Alpha0.20 with the accompanying Whoiskiller datapack. Full Blood DLC gating is unchanged. Network protocol remains 25 and packet formats are unchanged.

## 1.13.0+Alpha0.19 - 2026-10-03

- While Morphling, Identity Thief, Parasite, or a Seagull-borrowed Morphling is transformed, the disguiser's own client now renders first-person arms and F5 with the stolen player's skin, hat, and cape. Other clients are unchanged and still receive the existing server skin copy.
- The local override reads the target's already-loaded player skin, so CustomSkinLoader profiles continue to work without rewriting the disguiser's own UUID cache. Mime remote-control appearance still takes priority.
- Clients and the server must all use 1.13.0+Alpha0.19. Network protocol remains 25 and packet formats are unchanged.

## 1.13.0+Alpha0.18 - 2026-10-03

- While Mime remote control is active, the Mime's F5 nametag (name, colour, serial badge, and attachments) now follows the target. Hat and cape flags also follow the target skin. Other clients are unchanged.
- Clients and the server must all use 1.13.0+Alpha0.18. Network protocol remains 25 and packet formats are unchanged.

## 1.13.0+Alpha0.17 - 2026-10-03

- While Mime remote control is active, the Mime's own client now receives a private view packet and renders the local player (including first-person arms) with the target's skin. Other clients are unchanged and still see the dummy plus the real target.
- Clients and the server must all use 1.13.0+Alpha0.17; network protocol is now 25.

## 1.13.0+Alpha0.16 - 2026-10-03

- Added required Mime control synchronization: the controlled player's movement input is suppressed, mouse-look is locked to the server-authoritative controller view, and their client hotbar is hidden while the server retains the real role items for ability proxying.
- Added the bounded Mime control payload and disconnect cleanup. Clients and the server must all use 1.13.0+Alpha0.16; network protocol is now 24.

## 1.13.0+Alpha0.15 - 2026-10-02

- Replaced the retired in-game Goose encyclopedia pages with a four-button directory for the official Role Guide, Faction Gameplay guide, Game Modes guide, and Minecraft Goose Goose Duck website.
- Official links open through Minecraft's confirmation screen. Server-provided pages can open only the four exact allowlisted HTTPS destinations; arbitrary, lookalike, query-modified, and non-HTTPS URLs remain blocked.
- Clients and the server must all use 1.13.0+Alpha0.15. Network protocol remains 23 and packet formats are unchanged. Existing server page files under `config/goosetools/web` must be updated with the accompanying directory page.

## 1.13.0+Alpha0.14 - 2026-10-02

- Meeting dream proxies now render with vanilla's seated-player leg state even though their GooseTools-only chair proxy is not a rideable entity.
- Viewer-visible player markers now directly enable their matching outline colour, including on dream proxies that do not have a server-synchronized glow flag. Clients and the server must all use 1.13.0+Alpha0.14; network protocol remains 23 and packet formats are unchanged.

## 1.13.0+Alpha0.13 - 2026-10-02

- Replaced the Lucid Dreamer and Raven's visible vanilla dream mannequins with viewer-private GooseTools client RemotePlayers for meeting proxies, frozen map bodies, and dream-only corpse copies.
- Dream entry and wake now use staged client/server handoffs so the real body and its stand-in never render in the same frame. Meeting markers and their private outline colours now follow dream proxies.
- Added synchronized proxy pose, look, and swing animation while remote control is active. Clients and the server must all use 1.13.0+Alpha0.13; network protocol is now 23.

## 1.13.0+Alpha0.12 - 2026-09-30

- Fixed /aireport briefly opening and then immediately closing when run from chat. The report screen is now opened at the end of the client tick, after the submitting chat screen has completed its own close operation.
- Pending report opens are cancelled on disconnect or report-cache reset. Clients and the server must all use 1.13.0+Alpha0.12; network protocol remains 22 and packet formats are unchanged.

## 1.13.0+Alpha0.11 - 2026-09-30

- AI match reports are now stored per player in the server world's data/goosetools/ai-reports archive and restored after a server restart. /aireport opens a match list when multiple reports are available.
- Each archived match shows its game number, save time, and concise AI-written match summary. The server keeps 50 reports per player by default; config/goosetools/ai-report-history.json can set a value from 1 to 200.
- Starting another match no longer clears saved reports. Clients and the server must all use 1.13.0+Alpha0.11; network protocol remains 22 and packet formats are unchanged.

## 1.13.0+Alpha0.10 - 2026-09-28

- Fixed Morphling, Identity Thief, and Parasite disguises retaining the disguiser's own selected title or trust-rank icon. While transformed, the identity-bound `trust` slot now follows the copied player and restores automatically when the disguise ends.
- Other command-managed nametag icons remain attached to the real player, preventing lobby readiness or authority state from being copied with a disguise.
- Clients and the server must all use 1.13.0+Alpha0.10. Network protocol remains 22; packet formats are unchanged.

## 1.13.0+Alpha0.9 - 2026-09-27

- Corrected the Group 2 and Group 3 meeting-marker colours: Group 2 now uses `#c9ffab`, and Group 3 now uses `#fff0ab`.
- Clients and the server must all use 1.13.0+Alpha0.9 with the corrected Whoiskiller data pack. Network protocol remains 22; packet formats are unchanged.

## 1.13.0+Alpha0.8 - 2026-09-27

- Expanded private meeting markers with three group markers plus Kill, Information, Protection, and Solo cards, and changed faction cards to show the Goose, Duck, or Bird icon with the short faction name.
- Private meeting markers now highlight each marked living player only for the player who placed that marker. Faction and role markers use the existing spectator faction colours; the seven new marker types preserve their configured RGB outline colours through GooseTools rendering.
- Reordered the additional marker menu into aligned faction, group, and card rows. Clients and the server must all use 1.13.0+Alpha0.8 with the accompanying Whoiskiller data/resource packs. Network protocol remains 22; packet formats are unchanged.

## 1.13.0+Alpha0.7 - 2026-09-27

- Integrated the GooseThings CustomSkinLoader bridge into GooseTools for Minecraft 26.3 and CustomSkinLoader 15.1 snapshot builds. Marked runtime disguise profiles bypass CSL's identity cache while ordinary player profiles continue through CSL unchanged.
- The integration detects both the current `customskinloader-bootstrap` mod ID and the legacy `customskinloader` ID. When CSL is absent, its mixin is skipped and the module remains inactive; CSL is not a required dependency.
- Clients and the server must all use 1.13.0+Alpha0.7. Network protocol remains 22; packet formats are unchanged. Servers do not need CustomSkinLoader.

## 1.13.0+Alpha0.6 - 2026-09-24

- Fixed AI name-confirmation vote options, AI report/debug tabs, and in-game web page controls ignoring left clicks. Minecraft 26.3 numbers the left mouse button as 1; these custom screens still treated 0 as left click, so the visible options never received the click.
- Clients and the server must all use 1.13.0+Alpha0.6. Network protocol remains 22; packet formats are unchanged.

## 1.13.0+Alpha0.5 - 2026-09-24

- Witch Doctor `goosetools witchdoctor cansee` now returns 2 when Spirit Watching sees the cursed target through a wall that blocks ordinary line of sight, so the datapack can grant Perfect Alibi. Ordinary sight and camera feeds still return 1; curse charging still treats any positive result as visible.
- Clients and the server must all use 1.13.0+Alpha0.5. Network protocol remains 22; packet formats are unchanged.

## 1.13.0+Alpha0.4 - 2026-09-24

- Added command-managed, keyed public nametag icon slots with data-pack-controlled textures, dimensions, colours, and ordering. New public icons no longer require GooseTools Java changes.
- Nametags now show each player's trust rank at all times. In the lobby, participating players additionally show ready state and authority identity in the order ready, identity, trust, serial badge, then player name; spectators omit only the ready-state icon.
- Preserved the existing Admin rotating overhead icon and glow. Spectators now receive a private self-nametag entry even after releasing their lobby serial slot, so front and rear third-person views keep showing their own nametag without exposing the spectator to other viewers.
- Clients and the server must all use 1.13.0+Alpha0.4. Network protocol remains 22; the existing bounded nametag attachment packet is reused.

## 1.13.0+Alpha0.3 - 2026-09-23

- Meeting labels now use the resource pack's existing `minecraft:serial_badge` font sequence, including its badge, negative spacing, and precomposed 1-20 number glyphs, so badge and name alignment exactly match the established label style.
- Reported victims once again use Minecraft's real sleeping pose. The whole body is centred inside the wide viewport while only the head turns 60 degrees toward the viewer.
- The no-shadow meeting text and non-spectator movement lock remain unchanged.
- Clients and the server must all use 1.13.0+Alpha0.3. Network protocol remains 22; packet formats are unchanged.

## 1.13.0+Alpha0.2 - 2026-09-23

- Body-report victims now use an explicitly front-facing player render rotated horizontally in the HUD, so the full corpse remains visible with its face toward the viewer instead of inheriting Minecraft's inward-facing sleeping pose.
- Meeting labels now reuse the exact existing serial-number texture and compact pixel digits from world nametags, with corrected group alignment.
- Removed text shadows from meeting titles, subtitles, names, and serial digits to keep scaled text crisp.
- Non-spectator movement, jumping, sprinting, sneaking, and residual local velocity are blocked while the meeting transition is visible. Spectators, camera look, and authoritative server teleports remain unaffected.
- Clients and the server must all use 1.13.0+Alpha0.2. Network protocol remains 22; packet formats are unchanged.

## 1.13.0+Alpha0.1 - 2026-09-23

- Fixed the meeting-alert client startup crash by creating the bell item only after Minecraft has bound item component holders.
- Meeting-alert actors no longer display held items. Reporters now look toward the reported body, while the corpse faces the viewer and uses a wider viewport so its complete sleeping model remains visible.
- Reporter, victim, and normal bell-ringer labels now reuse each viewer's privacy-filtered GooseTools nametag colour and serial number.
- Added a dedicated pink sacrifice-bell alert that shows no player, preventing the technical meeting host or the duck who placed the bell from being exposed.
- Clients and the server must all use 1.13.0+Alpha0.1. Network protocol remains 22; the existing meeting-alert packet shape is unchanged.

## 1.13.0 - 2026-09-23

- Added a Goose Goose Duck-style animated meeting alert for authoritative body reports and emergency-bell calls, replacing the previous static title/subtitle notification.
- The alert shows the reporting or ringing player's live skin, name, and equipped appearance. Body reports also show the exact reported victim as a sleeping corpse with the captured skin and equipment; forced reports without a ground corpse use an equivalent victim snapshot.
- Added separate red report and amber bell sweeps with resolution-independent HUD layout, localized text, and frame-rate-independent entrance, hold, and exit timing.
- Clients and the server must all update to 1.13.0. Network protocol is now 22 because meeting alerts carry bounded player appearance snapshots.

## 1.12.4 - 2026-09-21

- Moved the limited-vision shader mask after translucent terrain so glass, stained glass, panes, ice, and other transparent blocks can no longer render over the fog boundary.
- Kept the mask inside Iris world rendering on its supported depth-tested text pipeline, preserving shader-produced distant silhouettes, exceptional bright lights, and glowing-entity outlines outside normal vision.
- Closed the horizontal blackout mask beyond the world's vertical build limits so steep upward or downward views can no longer bypass it, without adding an in-world vertical vision limit.
- Clients and the server must all update to 1.12.4. Network protocol remains 21; no packet format changed.

## 1.12.3 - 2026-09-21

- Restored exact wardrobe colours for world nametags and meeting marker text after the Minecraft 26.3 text submission parameter order changed.
- First-person view now hides only the body carrying the active camera, so a separate mannequin using the local player's profile keeps its nametag.
- Spectators can see their own managed nametag while other invisible entities remain concealed.
- Clients and the server must all update to 1.12.3. Network protocol remains 21; no packet format changed.

## 1.12.2 - 2026-09-21

- Added the Spook identity to the private meeting marker catalogue, using the existing localized role name and resource-pack icon.
- Clients and the server must all update to 1.12.2. Network protocol remains 21; no packet format changed.

## 1.12.1 - 2026-09-20

- Fixed camera terrain drawing zero indices and zero instances after the RenderPearl 26.3 indexed-draw argument order changed. This caused feeds to show entities over a black background even when terrain meshes were generated successfully.
- Use Minecraft's reversed-Z perspective projection, including the active backend's clip-depth range, so nearer blocks and actors correctly occlude distant terrain.
- Added an isolated GPU regression client that renders real block models through the production camera renderer and checks the read-back pixels for visible terrain and near/far occlusion. Test code is not packaged in the release JAR.
- Retained the cached terrain implementation, 960x540 targets and 30 FPS feed cap. Update clients and server to 1.12.1; protocol remains 21 and camera definitions need no migration.

## 1.12.0 - 2026-09-20

- Replaced the Minecraft 26.3 security-camera terrain renderer instead of continuing to reuse chunk or item render pipelines.
- Camera block and fluid meshes now use GooseTools-owned shader pipelines with explicit transforms, block-atlas sampling, lightmap sampling, reversed-Z depth, cutout handling, and translucency.
- Each terrain revision is rendered once into a persistent colour-and-depth cache; live frames copy that cache on the GPU and render only players, name tags, and dynamic block models over it.
- One-shot terrain meshes are released immediately after baking, preventing every visible monitor from resubmitting large static buffers at up to 30 FPS.
- A failed terrain revision is now isolated until a newer revision arrives. The monitor retains its last valid frame, or a stable no-signal background, instead of retrying and stalling every frame.
- Camera targets remain 960x540 at up to 30 FPS, with at most one off-screen feed updated per main-world frame.
- Clients and the server must all update to 1.12.0. Network protocol remains 21; no packet format changed.

## 1.11.6 - 2026-09-19

- Fixed security camera feeds remaining black on Minecraft 26.3 by clearing their depth attachment to the reversed-Z far plane (`0.0`) instead of the near plane (`1.0`).
- Camera terrain and entity/name-tag features now render in one attachment-preserving pass, keeping their depth comparisons consistent and reducing framebuffer churn.
- Iris vertex-format isolation now remains active when Iris is installed but shader packs are disabled, instead of depending on an active Iris shader pipeline.
- Added first-batch mesh/index diagnostics so an empty remote scene can be distinguished from a presentation failure without continuous GPU readback or frame-time cost.
- Camera targets remain 960x540 at up to 30 FPS, with at most one off-screen feed updated per main-world frame.
- Clients and the server must all update to 1.11.6. Network protocol remains 21; no packet format changed.

## 1.11.5 - 2026-09-19

- Fixed security camera framebuffer textures remaining black on Minecraft 26.3 by presenting them through a direct non-OIT world render type instead of the text OIT path.
- Removed the shader-sensitive `debug_quads` monitor backdrop that caused Iris missing-program errors.
- Camera terrain now builds outward from camera height and publishes the initial geometry in batches, so the first picture appears before the complete 128x32x128 capture is meshed.
- Kept camera targets at 960x540, capped at 30 FPS, with at most one off-screen feed updated per main-world frame.
- Clients and the server must all update to 1.11.5. Network protocol remains 21; no packet format changed.

## 1.11.4 - 2026-09-19

- Fixed security camera terrain remaining black with Iris 1.11.6 on Minecraft 26.3 by isolating both mesh construction and drawing from Iris extended vertex formats.
- Reduced security camera targets to 960x540 at up to 30 FPS and staggered visible feed updates so only one off-screen camera is processed per main-world frame.
- Clients and the server must all update to 1.11.4. Network protocol remains 21; no packet format changed.

## 1.11.3 - 2026-09-19

- Fixed security camera feeds remaining black on Minecraft 26.3 because synthetic camera actors did not have the now-required non-zero entity IDs.
- Stopped repeated camera renderer failures and retries from causing severe frame drops while players face active monitor walls.
- Fixed Birdwatcher view rotation temporarily removing all world geometry and flashing the screen by rebuilding only sections whose transparent-wall state changed.
- Clients and the server must all update to 1.11.3. Network protocol remains 21; no packet format changed.

## 1.11.2 - 2026-09-19

- Fixed custom world-map label text still stretching into long triangles with Iris shaders after the 1.11.1 buffer-boundary fix.
- Moved GooseTools room labels, task markers, and special icons on Xaero's World Map to Minecraft 26.3's extracted GUI rendering path, avoiding Xaero's shader-sensitive immediate font renderer.
- Minimap markers and in-world task paths keep their existing rendering path and behavior.
- Clients and the server must all update to 1.11.2. Network protocol remains 21; no packet format changed.

## 1.11.1 - 2026-09-19

- Fixed custom room labels, task markers, and special icons producing stretched triangles across Xaero's World Map while Iris shaders are enabled.
- Isolated the custom map element renderer from adjacent Xaero vertex batches before and after drawing, matching Xaero 26.3's buffer lifecycle.
- Clients and the server must all update to 1.11.1. Network protocol remains 21; no packet format changed.

## 1.11.0 - 2026-09-19

- Updated GooseTools from Minecraft 26.1.2 to 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Xaero's Minimap 26.5.3, and Xaero's World Map 1.46.4.
- Migrated world overlays, name tags, camera screens, vision masks, input handling, GUI screen access, entity lookup, and swing continuity to the 26.3 rendering and client APIs.
- Recommended shader discovery now requests Minecraft 26.3 builds instead of 26.1.2 builds.
- Added official GooseTools mod icon cover and updated open source license to GNU General Public License v3.0 (GPL-3.0-or-later).
- Clients and the server must all update to 1.11.0. Network protocol remains 21; no packet format changed.

## 1.10.12 - 2026-09-19

- Added the Debugger-only AI trace transport and native viewer for request type, timings, evidence budgets, exact prompts, and raw model responses.
- Match-time traces expose metadata only; sensitive content is released by the server only after the AI pipeline ends and is never cached to disk by GooseTools.
- Clients and the server must all update to 1.10.12. Network protocol is now 21 and rejects older clients before AI trace packets can be used.

## 1.10.11 - 2026-09-19

- Added server-authoritative `/goosetools nametags hide`, `show`, and `clear` commands for data-pack-controlled, per-viewer nametag privacy without future client rendering changes.
- Lucid Dreamers no longer see real player names, room-order badges, or nametag attachments above dream bodies; normal labels return on wake and every forced dream cleanup path.
- Visibility overrides are cleared when either player disconnects and when the server stops, preventing stale rules from leaking across sessions.
- Clients and the server must all update to 1.10.11. Network protocol remains 20; no packet format changed.

## 1.10.10 - 2026-09-18

- Fixed lobby spectators and stale spectator state occupying room-order slots and blocking a match from starting, including the four-player minimum case.
- Dead spectators still retain their stable room-order slot during an active match for reconnect and meeting consistency.
- Clients and the server must all update to 1.10.10. Network protocol remains 20; no packet format changed.

## 1.10.9 - 2026-09-18

- Fixed the Guard shield nametag icon not rendering because it requested `textures/item/guard_shield_nametag.png` instead of the canonical `textures/item/guard_shield.png`.
- Embedded `guard_shield.png` directly into GooseTools bundled assets so the nametag icon always resolves regardless of external resource pack status.
- Restored natural `0xffffff` vertex rendering for the Guard shield icon, eliminating the unwanted greenish tint (`0xbeffad`).
- Added a `samePlayer` check to `NameTagAttachmentPolicy.showGuardShield` to ensure Guards do not see shield icons above themselves in third-person view.
- Clients and the server must all update to 1.10.9. Network protocol remains 20; no packet format changed.

## 1.10.8 - 2026-09-18

- Restored the Guard-only shield icon beside a protected player's nametag. It now uses the same private flag path as gravy/pigeon icons, so it still appears without FullBlood and no longer depends on the data-driven attachment JSON.
- Clients and the server must all update to 1.10.8. Network protocol remains 20; no packet format changed.

## 1.10.7 - 2026-09-18

- The infesting Parasite keeps its nametag hidden through the meeting blackout. The label only returns after the seat restores its visible body (`parasiteMeetingVisible`).
- Clients and the server must all update to 1.10.7. Network protocol remains 20; no packet format changed.

## 1.10.6 - 2026-09-18

- Fixed meeting player-marker icons on player heads rendering inverted, stretched across the GUI, and detached from slots due to swapped blit coordinate bounds.
- Added private player-marker role and faction name display to player head item tooltips during meetings.
- Added a Guard-only `guard_shield` nametag icon beside living players who currently have a Guard shield.
- Added Guard (code 129) to the private meeting player-marker catalog.
- Clients and the server must all update to 1.10.6. Network protocol remains 20; no packet format changed.

## 1.10.5 - 2026-09-18

- Fixed meeting player-marker icons stretching from inventory slots across the screen because GUI rectangle bounds were passed as width and height.
- Clients and the server must all update to 1.10.5. Network protocol remains 20; no packet format changed.

## 1.10.4 - 2026-09-18

- Added private, meeting-only player markers on player-head items and living-player nametags, including faction colors and role icons.
- Added explicit dream-mannequin aliases so Lucid Dreamer and Raven dream bodies inherit the marker of their source player.
- Marker values remain stored between meetings but are hidden outside meetings; only manual clearing or game end deletes them.
- Clients and the server must all update to 1.10.4. Network protocol is now 20 because the private nametag snapshot format changed.

## 1.10.3 - 2026-09-18

- Added the data-driven `fan_true` name-tag attachment shown beside living players correctly identified by Magpie during a meeting.
- Clients and the server must all update to 1.10.3 for the meeting icon. Network protocol remains 19; no packet format changed.

## 1.10.2 - 2026-09-17

- Added POLUS map bounds and Xaero room labels (Office, Specimens, Laboratory, Storage, Communications, Weapons, Boiler Room, O2, Security, Dropship).
- POLUS uses the same cave-layer rule as Goose Chapel and Goose Spaceship.
- Client and server must both update to 1.10.2 to see POLUS room names on the minimap. Network protocol remains 19; no packet format changed.

## 1.10.1 - 2026-09-17

- Added a Parasite-only fake spectator interaction layer while the server uses the real vanilla `/spectate` camera: the local player's own hotbar is rendered, number keys and mouse wheel select its slots, and the ready breakout item can send a normal right-click use packet from Spectator mode.
- The override activates only when slot nine contains the server-issued Parasite breakout item or its cooldown form; ordinary spectators and Carrier-host dormancy retain vanilla spectator controls.
- Client and server must both update to 1.10.1. Network protocol remains 19; the mandatory exact-version handshake prevents older clients from joining without this interaction support.

## 1.10.0 - 2026-09-16

- Added an AI analysis progress HUD for local transcription, uncertain-name checking, voting, report generation, and delivery.
- Added a server-authoritative participant vote screen for mapping uncertain ASR player-name mentions, including bounded payloads, one vote per participant, timeout handling, abstention, and strict-majority resolution.
- Client and server must both update to 1.10.0. Network protocol is now 19 because the AI progress and bidirectional name-vote payloads are required.

## 1.9.1 - 2026-09-16

- Added Parasite disguise identity support so stolen victim appearance and nametag data are rendered consistently with the existing disguise pipeline; the hidden in-host camera body no longer exposes a nametag outside meetings.
- Added Carrier and Parasite entries to the bundled role reference page.
- Client and server must both update to 1.9.1. Network protocol remains 18; no packet format changed.

## 1.9.0 - 2026-09-16

- Added FullBlood-DLC-only client-view aim claims for moving targets, with server-side validation against recent authoritative hitboxes, configured skill range, solid-block occlusion, target ordering, packet age, replay sequence, and teleport-sized displacement.
- Non-FullBlood games continue to use the existing pure-vanilla `looking_at` predicate. FullBlood claims that are missing or rejected also fall back to the vanilla predicate, so packet loss cannot suppress a valid server-side hit.
- Client and server must both update to 1.9.0. Network protocol is now 18 because the authenticated client-to-server aim-claim payload is required.

## 1.8.5 - 2026-09-16

- Added bounded, server-authoritative data-driven nametag attachments loaded from `config/goosetools/nametag_attachments.json`; future icons can define resource-pack textures, viewer/target tag rules, dimensions, colours, disguise behavior, and FullBlood gating without another GooseTools code change.
- Added `/goosetools nametags reload`, with last-known-good fallback when a configuration is invalid, so attachment rules can be refreshed safely while the server is running.
- Detective inspections in FullBlood mode now play the enchantment-table sound and privately show the inspected target's snapshot result as `angelring` or `demonring` until the next meeting; non-FullBlood games retain the original translated chat result.
- Client and server must both update to 1.8.5 and keep the Whoiskiller resource pack installed. Network protocol is now 17 because nametag snapshots carry a bounded list of data-driven texture attachments.

## 1.8.4 - 2026-09-15

- Replaced camera-relative world projection for the opt-in predictive blackout wall guide with stable screen-space contours, eliminating movement shake while preserving the first reachable same-Y collision boundary.
- Made recommended-shader resolution independent per component so installed Sodium, Iris and Euphoria Patcher files are never downloaded again merely because Complementary is missing.
- When the required mods are already loaded, GooseTools now downloads only a missing verified Complementary archive, asks the running Euphoria Patcher to generate its patched pack, applies the POPULAR profile, and enables Iris immediately without a game restart.
- Client and server must both update to 1.8.4. Network protocol remains 16; no packet format changed.

## 1.8.3 - 2026-09-15

- Restored the original depth-tested, depth-writing blackout mask so nearby terrain remains visible and glass outside the limited-vision radius no longer renders through the blackout.
- Kept the opt-in predictive wall guide visible by projecting its outlines just inside the restored mask without changing the established one-block clear radius, two-block fade radius, or same-Y detection rules.
- Client and server must both update to 1.8.3. Network protocol remains 16; no packet format changed.

## 1.8.2 - 2026-09-15

- Fixed the function-panel blackout-assist action being replaced by the dialog destination (usually the pause screen), and hid the command warning icon only on the trusted GooseTools function-panel dialog.
- Added HMCL, PCL and Minecraft Launcher identification to recommended-shader installation and a 120-second restart ticket that allows the GooseTools-launched client while closing only a duplicate launcher-started client. Sensitive launch arguments are removed from the handoff manifest before the old client exits.
- Switched the blackout mask to Minecraft's Iris-compatible see-through text pipeline so it preserves the world depth buffer and avoids the shader-pack `No active program` OpenGL error path.
- Forced Xaero Minimap coordinate, overworld-coordinate and chunk-coordinate displays off while connected to a Goose/Whoiskiller server without changing their settings elsewhere.
- Client and server must both update to 1.8.2. Network protocol remains 16; no packet format changed.

## 1.8.1 - 2026-09-15

- Changed the opt-in blackout wall guide to predict the first reachable same-Y collision boundary up to 12 blocks away. Blackout mask geometry now preserves world depth, so guides remain readable through darkness without revealing walls behind solid foreground geometry.
- Made the recommended shader installer work when Windows or HMCL withholds `ProcessHandle` arguments by reconstructing the current Fabric launch from JVM-owned data. If relaunch still cannot be recovered, verified files install after the match and the player is instructed to reopen through HMCL.
- Replaced GooseTools-owned dialog and chat command buttons with trusted client-local custom actions, removing Minecraft's command-execution warning when opening the blackout settings from the function panel and preventing duplicate setup feedback.
- Client and server must both update to 1.8.1. Network protocol remains 16; no packet format changed.

## 1.8.0 - 2026-09-15

- Added an opt-in, same-Y blackout wall guide that highlights only collision boundaries reachable from the player, avoiding wall-behind-wall information leaks; it is disabled by default and available from the function panel.
- Added one-time-per-blackout recommendations with clickable shader, wall-guide, and permanent-dismiss actions. Complementary Reimagined with Euphoria's own POPULAR profile is preferred for brightness and visual quality.
- Added a consent-gated Modrinth installer for stable Minecraft 26.1.2 Fabric releases of Sodium, Iris, Euphoria Patcher, and Complementary Reimagined, including fixed project allowlisting, SHA-512 and archive identity checks, strict path/size limits, exact Iris-Sodium dependency resolution, backups, automatic post-lobby restart, and early-crash rollback.
- Client and server must both update to 1.8.0. Network protocol is now 16 for authoritative blackout eligibility and client-settings opening.

## 1.7.0 - 2026-09-13

- Added authenticated, compressed, chunked AI report delivery with a UUID-specific server cache that never sends another player's personal report.
- Added an auto-opening native report screen with global, highlight, and personal tabs plus `/aireport` for reopening the latest report.
- Client and server must both update to 1.7.0. Network protocol is now 15 because private AI report delivery is required.

## 1.6.7 - 2026-09-12

- Astral players can no longer see other players' GooseTools nametags, room-order badges, or private nametag attachments while projected.
- Labels remain concealed during the brief return/reveal transition and restore automatically afterward; the Astral player's own second-/third-person label remains visible.
- Client and server must both update to 1.6.7. The nametag payload is unchanged, so the required network protocol remains 14.

## 1.6.6 - 2026-09-12

- Active Morphling and Identity Thief disguises now present the stolen player's complete nametag identity: name, wardrobe colour, room-order badge, Lover heart/pink name, Witch Doctor target marker/highlight, Gravy bounty, Clown balloon, and Pigeon infection markers.
- Observer-private attributes are evaluated against the stolen identity while visibility and lifecycle remain attached to the living disguised player; collected-but-unused Morphling DNA never changes the nametag.
- The most recent stolen visual identity remains cached if its source disconnects and restores immediately when `stealId` ends.
- Client and server must both update to 1.6.6. The nametag payload now carries a distinct effective identity UUID, so the required network protocol is 14.

## 1.6.5 - 2026-09-12

- Infected players now display a private `pigeon` infection badge next to their GooseTools nametag visible only to the Pigeon.
- The infection badge remains visible while the Pigeon is spectating if the target player is still infected, and clears alongside the target's infection state.
- Client and server must both update to 1.6.5. Network protocol remains 13; the exact-version handshake rejects older clients.

## 1.6.4 - 2026-09-12

- Lucid Dreamers no longer see a simultaneously dreaming Raven's real nametag, room-order badge, or nametag attachments through the Raven's dream disguise.
- Hidden dream identities remain managed so vanilla nametags cannot leak as a fallback; Raven-to-Lucid, self third-person, and post-dream labels are unchanged.
- Client and server must both update to 1.6.4. Network protocol remains 13; the exact-version handshake rejects older clients.

## 1.6.3 - 2026-09-12

- Voluntary lobby spectators now release their room-order serial immediately and receive a new serial at the end of the current lobby order only after leaving spectator mode.
- Dead spectators keep their match serial, while overflow spectators remain unnumbered.
- Client and server must both update to 1.6.3. Network protocol remains 13; the exact-version handshake rejects older clients.

## 1.6.2 - 2026-09-12

- Corrected the exact nametag and serial-badge colours for Sunny Yellow (`#FAEF56`), Sweet Orange (`#EB7E53`), and Knowledge Purple (`#8400FF`); all wardrobe presets now match the authoritative wardrobe catalog.
- The local player's GooseTools nametag and room-order badge now render in both rear and front third-person camera modes while remaining hidden in first person.
- Client and server must both update to 1.6.2. Network protocol remains 13; the exact-version handshake rejects older clients.

## 1.6.1 - 2026-09-12

- Fixed nametags and serial badges falling back to white by resolving the server-authoritative `ggdYuik` wardrobe selection, including exact custom `ggdYuikColor` values, before using equipped armour as a fallback.
- Fixed the room-order number being hidden behind the serial badge by drawing crisp 3x5 digits in the badge's own foreground render batch.
- Moved all attachments to the left side of the nametag, with the room-order badge always nearest the player name.
- Nametags now ignore only wall blocks validated and hidden by Birdwatcher wall transparency, while ordinary walls continue to occlude them.
- Client and server must both update to 1.6.1. Network protocol remains 13; the exact-version handshake rejects older clients.

## 1.6.0 - 2026-09-12

- Added depth-tested, limited-vision-aware player nametags with exact wardrobe HEX colours and reusable coloured room-order badges.
- Added a server-authoritative 1-20 room order that follows join order, reserves disconnected slots during a match, compacts on return to the lobby, and forces overflow players into spectator mode.
- Moved Witch Doctor targets, Gravy bounties, Clown balloons, and Lover marks into the client nametag attachment channel, including security-camera actors and player-profile stand-ins.
- Meeting seats now follow circular room order beginning after the actual caller or reporter instead of being randomized.
- Client and server must both update to 1.6.0. Network protocol is now 13 because filtered nametag snapshots are required.

## 1.5.7 - 2026-09-12

- Fixed swallowed players incorrectly losing sight of the Pelican and nearby living players while spectating the Pelican.
- Limited-vision entity culling now follows the active camera entity instead of the swallowed player's parked body, preserving the Pelican's normal vision range without granting full-map sight.
- Client and server must both update to 1.5.7. Network protocol remains 12; the exact-version handshake rejects older clients.

## 1.5.6 - 2026-09-11

- Fixed player armour and other late entity-render layers remaining visible in the fully black area outside limited Birdwatcher vision.
- Players, mannequins, and armour stands are now rejected as complete render units only after their full horizontal bounds leave the two-block near circle, the 50-degree outer fan, or the distance fade; entities inside the observation area remain visible through validated transparent walls.
- Client and server must both update to 1.5.6. Network protocol remains 12; the exact-version handshake rejects older clients.

## 1.5.5 - 2026-09-11

- Extended limited Birdwatcher wall transparency to the complete two-block circular near-vision area instead of restricting all wall scans to the forward fan.
- Added an independent 360-degree near-wall probe that validates up to eight wall layers and the open space behind them; a wall entering the two-block circle now hides through its complete validated thickness while lateral expansion remains clipped to that circle or the existing forward fan.
- Floors, ceilings, the outer side/rear fade band, the 40-to-50-degree soft fan, and Witch Doctor curse targeting rules are unchanged.
- Client and server must both update to 1.5.5. Network protocol remains 12; the exact-version handshake rejects older clients.

## 1.5.4 - 2026-09-10

- Replaced the limited-vision Birdwatcher sector with a straight-sided fan: the central 40 degrees remain clear, both sides fade smoothly to black at 50 degrees, and the far boundary now uses the same four-block fade span as ordinary limited vision.
- Increased supported Birdwatcher wall transparency from three to eight actual wall-normal block layers while continuing to exclude floors and ceilings.
- Witch Doctor curse progress through Birdwatcher vision now requires the three-dimensional crosshair ray to hit the target's real bounding box and every intervening solid run to qualify as an actually transparent vertical wall no more than eight blocks thick.
- Ordinary Witch Doctor sight and security-camera curse checks are unchanged.
- Client and server must both update to 1.5.4. Network protocol remains 12; the exact-version handshake rejects older clients.

## 1.5.3 - 2026-09-10

- Fixed the private Witch Doctor `curse_eye` marker rendering vertically inverted in both the normal world and security-camera feeds.
- Split current-frame monitor focus from the existing three-second streaming subscription: camera terrain and actor caches remain warm for fast re-entry, while curse progress stops immediately when no successfully rendered monitor is in the Witch Doctor's view.
- Required the curse target to be visible to a feed that is both actively rendered in the Witch Doctor's current view and retained by the validated camera subscription.
- Added a short fail-safe expiry for current monitor focus so a stalled or minimized client cannot continue curse progress.
- Client and server must both update to 1.5.3. Network protocol is now 12 because current monitor focus uses a separate authenticated packet.

## 1.5.2 - 2026-09-10

- Added a private `curse_eye` marker above the Witch Doctor's current target in both the normal world and security-camera feeds; only that Witch Doctor receives the target state.
- Made directional target highlighting render as a purple outline inside security-camera feeds as well as in the normal world.
- Fixed security-camera curse progress for visibly rendered targets by ignoring transparent non-occluding blocks, skipping camera-housing blocks, and sampling more points across the target's body.
- Increased every visible security-camera feed to an independent 60 FPS render cadence, with 20 TPS actor snapshots interpolated client-side.
- Stabilized camera-rendered player capes by advancing their client avatar and cloak state from the authoritative actor snapshots.
- A curse target swallowed by a Pelican is now immediately replaced with another eligible target.
- Client and server must both update to 1.5.2. Network protocol is now 11 because private Witch Doctor target synchronization is required.

## 1.5.1 - 2026-09-10

- Matched Witch Doctor curse sight range to the final rendered view, including the additional 2x Birdwatcher range applied after Witch Doctor's own 2x vision bonus.
- Matched security-camera curse checks to the camera's actual 70-degree widescreen frustum and accepted visible eye, torso, or lower-body samples instead of requiring the target's eyes alone.
- Added per-scan wall-block detection: block types with at least four samples and a 60% successful facade rate extend transparency vertically through matching side-wall blocks without horizontally flooding floors or ceilings.
- Fixed Witch Doctor Birdwatch cooldown stacks showing 99 at every remaining duration.
- Client and server must both update to 1.5.1. Network protocol remains 10; the exact-version handshake rejects older clients.

## 1.5.0 - 2026-09-10

- Increased Birdwatcher observation range from 1.5x to 2x the configured vision range.
- Added Witch Doctor authorization for Birdwatcher vision and doubled Witch Doctor limited-vision range.
- Added server-authoritative Witch Doctor sight checks for direct view, Birdwatcher view, and actively watched security-camera feeds.
- Kept administrative commands available to Minecraft 26.1.2 console and function-compilation contexts while continuing to require Game Master permission from real players.
- Client and server must both update to 1.5.0. Network protocol remains 10; the existing exact-version handshake rejects older clients.
