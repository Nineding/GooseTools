# Postgame summary (Alpha0.64)

FullBloodDLC=1 opens a two-page summary as soon as the faction result's one-second fade reaches black. Lobby cleanup proceeds behind the screen. A SMVP takes precedence over all MVPs; otherwise the highest-index MVP is selected, breaking ties by room seat. With no award, the highest-index participant is selected using the same tie-break. The entire room sees the same spotlight. Observers receive no personal rewards.

The left stage uses the frozen result roster's true skin, skin overlays and clothing, including dead award winners. The right side uses only the recipient's already-paid reward snapshot. Level animation supports several levels, capped XP bank rollover and exact final values. The server remains the sole reward authority: opening, skipping, closing, reconnecting or duplicating packets never grants XP or currency.

Choose a persistent wave or dance from the client settings' Victory celebration button or `/goosetools-actions` while in the lobby. The server saves the self-only choice with `ggdVictoryDance`. The first page has a native Next page button; it completes the personal animation and opens the roster in the same screen/session. The second page has Previous page and Close buttons. Escape closes immediately, and Tab/Enter/Space use native button navigation. Closing resumes deferred trust promotion; page changes do not acknowledge or release the session. A disconnected active session clears its presentation guard and resumes promotion on rejoin.

## Roster and sound

The second page groups players by their saved true faction and room seat, with skin heads, roles, signed indices and the existing resource-pack grade and award glyphs. A clipped scroll region supports 21 players while keeping navigation and the personal task count visible. Long names and role labels fit within fixed table columns. Minecraft 26.3 uses SDL mouse/button constants: the old left-button `0` and Enter `257` checks never matched normal input. Footer buttons now use registered widgets whose actual screen-space bounds also drive rendering, so GUI scaling and resize preserve hit testing and native keyboard navigation.

XP growth plays a quiet vanilla experience-orb sound every 0.18 seconds with gradually rising pitch. Level-up uses the existing vanilla upgrade sound. Observers and zero-XP results are silent; opening the roster, closing or receiving cancellation stops this screen’s sound instances. Returning to the first page shows final values without replaying XP or sound.

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

The extended summary uses `summary_s2c_v2`, with the existing `summary_close_v1` acknowledgement. Protocol is 34; both sides require Alpha0.64. The full immutable roster includes all saved participants (including offline players), true faction, score, role, name and award, plus the recipient’s task count. Capture precedes lobby cleanup. The roster is bounded to 21 unique UUIDs and scores remain signed. GooseThings and the Alpha0.63 datapack hooks are unchanged.

## Verification

- Alpha0.64 full Gradle unit suite: 4,454 tests, zero failures/errors, covering award priority/ties, multi-level and capped XP, pose isolation, roster immutability, duplicate identities, signed scores and room limits.
- Isolated Minecraft client/server regression: actual v2 packets, early black-screen handoff, clothing/cape preservation, wave/dance, observer/zero XP, normal/forced-stop/non-DLC cleanup and duplicate acknowledgement. 60 GPU screenshots; GUI scales 2/3 and resize; SkinLayers3D 1.11.3 loaded.
- Clicks pass through the real Minecraft MouseHandler using current SDL constants, covering Next, Previous, Close and keyboard confirmation. Returning from Previous restores focus to the visible Next button. Neither page changes nor duplicate packets grant rewards or prematurely release deferred promotion.
- Four-player and full 21-player rosters include every faction and offline participants; changing scores and clearing identity storage after capture does not change the received results. Top/bottom screenshots confirm clipping, scroll bounds and fixed footer controls.
- SoundManager listeners observe actual vanilla XP sound events, including a small gain without leveling. Observers and zero-XP summaries remain silent; early page changes stop growth sounds and returning does not replay them.
- Dedicated server reached `Done` with Alpha0.64/protocol 34 and shut down normally. No client-class loading error. Verification uses an isolated world, not a replay of the user's live match.

Original artwork and the successful local ComfyUI PerfectPixel workflow are under `art/summary/`. The runtime atlas retains transparency and a discrete pixel grid.
