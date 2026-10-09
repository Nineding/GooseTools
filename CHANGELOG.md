# Changelog

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
