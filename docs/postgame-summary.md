# Postgame summary (Alpha0.62)

FullBloodDLC=1 opens a single summary after the faction result presentation and normal lobby cleanup. A SMVP takes precedence over all MVPs; otherwise the highest-index MVP is selected, breaking ties by room seat. With no award the participant sees their own round review. Observers see a public participant and no personal rewards.

The left stage uses the frozen result roster's true skin, skin overlays and clothing, including dead award winners. The right side uses only the recipient's already-paid reward snapshot. Level animation supports several levels, capped XP bank rollover and exact final values. The server remains the sole reward authority: opening, skipping, closing, reconnecting or duplicating packets never grants XP or currency.

Choose a persistent wave or dance from the client settings' Victory celebration button or `/goosetools-actions` while in the lobby. The server saves the self-only choice with `ggdVictoryDance`. The summary first skips on Escape/Enter/Space or the button, then closes on the next action. Closing resumes deferred trust promotion. A disconnected active session clears its presentation guard and resumes promotion on rejoin.

## Animation isolation and clipping

Only the summary's fresh AvatarRenderState carries a celebration marker. Normal world, inventory and meeting renders are unmodified. Skin sleeves are limb children; armor consumes the same pose. The wave opens the right arm with positive Z rotation. The dance keeps both arms outside the chest with small alternating steps. The torso stays aligned with its shoulder pivots, eliminating the previous detached shoulder tilt and inward arm sweep. Both actions ease in over 0.45 seconds and use elapsed time independently of frame rate.

## Datapack integration

Apply the companion `datapack/postgame-summary.patch` to the matching Whoiskiller datapack source; it is a scoped patch against the main world's pre-update files, not a replacement world. Both local main and development copies were updated separately, preserving differences in their trust systems.

| Phase | Hook |
| --- | --- |
| Before authoritative XP grant | `goosetools summary before @a[tag=players]` in `ggd:index/calculate_all` |
| After grade/currency/XP calculation | `goosetools summary capture` in `ggd:gameendevents/summary/start` |
| Normal result return | `goosetools summary ready` before `mainsys:stopgame` |
| Every stop path | `goosetools summary stop` preserves only an armed normal handoff |
| Lobby cleanup complete | `goosetools summary open` before `lobby:level/schedule_postgame_display` |
| Starting another match | `goosetools summary clear` before resetting round scores |
| Deferred trust dispatch | Exclude `tag=ggdSummaryActive` until close, timeout or reconnect |

The new `lobby:level/summary_consume_xp` function consumes only the legacy presentation flags and synchronizes the vanilla XP bar. It preserves achievement/reward chat and performs no reward grants. Queued snapshots keep the existing blackout through cleanup; only the display packet replaces it with the summary. Forced stop and non-DLC cancel the queue. Server sessions expire after 120 seconds; client round IDs reject duplicate displays.

New additive channels: `summary_s2c_v1` and `summary_close_v1`. Protocol remains 33; existing exact-version handshake requires Alpha0.62 on both sides. GooseThings is unchanged.

## Verification

- Full Gradle build: 4,452 tests, zero failures/errors; unit coverage includes award priority/ties, multi-level XP, capped bank rollover and outward arm trajectories.
- Isolated real Minecraft client/server test: actual packets, snapshot capture, black-screen handoff, preserved clothing for dead SMVP, wave/dance, observers, zero XP, skip/close, duplicate messages, forced cancellation and non-DLC. GUI scales 2/3; SkinLayers3D 1.11.3 loaded. 48 GPU screenshots, including 20 sequential frames per action, reviewed for skin/chestplate alignment.
- Dedicated server reached `Done` and shut down normally with Alpha0.62/protocol 33. No client-class loading error. Existing Windows performance-counter diagnostics remain unrelated.
- Changed datapack macro lines and JSON/translations validated. Full gameplay in the user's live map has not been replayed; verification used an isolated world.

Original artwork and the successful local ComfyUI PerfectPixel workflow are under `art/summary/`. The runtime atlas retains transparency and a discrete pixel grid.
