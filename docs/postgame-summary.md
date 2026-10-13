# Postgame summary (Alpha0.63)

FullBloodDLC=1 opens a single summary as soon as the faction result's one-second fade reaches black. Lobby cleanup proceeds behind the screen. A SMVP takes precedence over all MVPs; otherwise the highest-index MVP is selected, breaking ties by room seat. With no award, the highest-index participant is selected using the same tie-break. The entire room sees the same spotlight. Observers receive no personal rewards.

The left stage uses the frozen result roster's true skin, skin overlays and clothing, including dead award winners. The right side uses only the recipient's already-paid reward snapshot. Level animation supports several levels, capped XP bank rollover and exact final values. The server remains the sole reward authority: opening, skipping, closing, reconnecting or duplicating packets never grants XP or currency.

Choose a persistent wave or dance from the client settings' Victory celebration button or `/goosetools-actions` while in the lobby. The server saves the self-only choice with `ggdVictoryDance`. The summary first skips on Escape/Enter/Space or the button, then closes on the next action. Closing resumes deferred trust promotion. A disconnected active session clears its presentation guard and resumes promotion on rejoin.

## Animation isolation and clipping

Only the summary's fresh AvatarRenderState carries a celebration marker. Normal world, inventory and meeting renders are unmodified. Skin sleeves are limb children; armor consumes the same pose. The wave opens the right arm with positive Z rotation. The dance keeps both arms outside the chest with small alternating steps. The torso stays aligned with its shoulder pivots, eliminating the previous detached shoulder tilt and inward arm sweep. Both actions ease in over 0.45 seconds and use elapsed time independently of frame rate.

Capes remain visible. The preview follows the camera without ticking cloak physics, so the extracted `capeFlap`, `capeLean` and `capeLean2` are explicitly zeroed for this screen. Otherwise stale cloak coordinates mimic a large teleport and lift the cape.

The goose-head medal and subtitle are removed. `summary_symbols` uses exact copies of the existing Whoiskiller MVP/SMVP, grade and goose-coin textures, at their original Minecraft resource paths so the active pack can override them. The generated atlas is used only for the level badge and sparkle.

## Datapack integration

Apply the companion `datapack/postgame-summary.patch` to the matching Whoiskiller datapack source; it is a scoped patch against the main world's pre-update files, not a replacement world. Both local main and development copies were updated separately, preserving differences in their trust systems.

Then apply `datapack/postgame-summary-alpha63.patch`: DLC returns after the one-second fade instead of the legacy five-second countdown. Other modes retain their existing countdown. Use `git apply --ignore-space-change` when the target datapack uses Windows CRLF line endings.

| Phase | Hook |
| --- | --- |
| Before authoritative XP grant | `goosetools summary before @a[tag=players]` in `ggd:index/calculate_all` |
| After grade/currency/XP calculation | `goosetools summary capture` in `ggd:gameendevents/summary/start` |
| Normal result return | `goosetools summary ready` before `mainsys:stopgame` |
| Every stop path | `goosetools summary stop` immediately opens an armed normal handoff; other stops cancel it |
| Lobby cleanup complete | Legacy `goosetools summary open` is idempotent; it cannot restart the early-opened screen |
| Starting another match | `goosetools summary clear` before resetting round scores |
| Deferred trust dispatch | Exclude `tag=ggdSummaryActive` until close, timeout or reconnect |

The new `lobby:level/summary_consume_xp` function consumes only the legacy presentation flags and synchronizes the vanilla XP bar. It preserves achievement/reward chat and performs no reward grants. Only a server display packet may replace the blackout; an early packet waits until the fade is fully black, then starts the summary immediately. Forced stop and non-DLC cancel the queue. Server sessions expire after 120 seconds; client round IDs reject duplicate displays.

New additive channels: `summary_s2c_v1` and `summary_close_v1`. Protocol remains 33; existing exact-version handshake requires Alpha0.63 on both sides. GooseThings is unchanged.

## Verification

- Full Gradle build: 4,452 tests, zero failures/errors; unit coverage includes award priority/ties, multi-level XP, capped bank rollover and outward arm trajectories.
- Isolated real Minecraft client/server test: actual packets, snapshot capture, black-screen handoff, preserved clothing for dead SMVP, wave/dance, observers, zero XP, skip/close, duplicate messages, forced cancellation and non-DLC. GUI scales 2/3; SkinLayers3D 1.11.3 loaded. 48 GPU screenshots, including 20 sequential frames per action, reviewed for skin/chestplate alignment.
- Alpha0.63's isolated client test additionally sends the display packet early: the result fades all the way to black and the summary appears within the following tick, without a world frame. The late cleanup open cannot restart it. A visible cape fixture retains its texture with all three motion values zeroed.
- Dedicated server reached `Done` and shut down normally with Alpha0.63/protocol 33. No client-class loading error. Existing Windows performance-counter diagnostics remain unrelated.
- Changed datapack macro lines and JSON/translations validated. Full gameplay in the user's live map has not been replayed; verification used an isolated world.

Original artwork and the successful local ComfyUI PerfectPixel workflow are under `art/summary/`. The runtime atlas retains transparency and a discrete pixel grid.
