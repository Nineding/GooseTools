# Postgame summary (Alpha0.65)

FullBloodDLC=1 opens a two-page summary as soon as the faction result's one-second fade reaches black. Lobby cleanup proceeds behind the screen. A SMVP takes precedence over all MVPs; otherwise the highest-index MVP is selected, breaking ties by room seat. With no award, the highest-index participant is selected using the same tie-break. The entire room sees the same spotlight. Observers receive no personal rewards.

The left stage uses the frozen result roster's true skin, skin overlays and clothing, including dead award winners. The right side uses only the recipient's already-paid reward snapshot. Level animation supports several levels, capped XP bank rollover and exact final values. The server remains the sole reward authority: opening, skipping, closing, reconnecting or duplicating packets never grants XP or currency.

Choose a persistent wave or dance from the client settings' Victory celebration button or `/goosetools-actions` while in the lobby. The server saves the self-only choice with `ggdVictoryDance`. The first page has a native Next page button; it completes the personal animation and opens the roster in the same screen/session. The second page has Previous page and Close buttons. Escape closes immediately, and Tab/Enter/Space use native button navigation. Closing resumes deferred trust promotion; page changes do not acknowledge or release the session. A disconnected active session clears its presentation guard and resumes promotion on rejoin.

## Roster and sound

The second page groups players by their saved true faction and room seat, with skin heads, roles, signed indices and the existing resource-pack grade and award glyphs. A clipped scroll region supports 21 players while keeping navigation and the personal task count visible. Long names and role labels fit within fixed table columns. Minecraft 26.3 uses SDL mouse/button constants: the old left-button `0` and Enter `257` checks never matched normal input. Footer buttons now use registered widgets whose actual screen-space bounds also drive rendering, so GUI scaling and resize preserve hit testing and native keyboard navigation.

XP growth plays a quiet vanilla experience-orb sound every 0.18 seconds with gradually rising pitch. Level-up uses the existing vanilla upgrade sound. Observers and zero-XP results are silent; opening the roster, closing or receiving cancellation stops this screen’s sound instances. Returning to the first page shows final values without replaying XP or sound.

## Animation isolation and clipping

Only the summary's fresh AvatarRenderState carries a celebration marker. Normal world, inventory and meeting renders are unmodified. The wave keeps its outward shoulder sweep. The cute dance uses 399 authored quaternion poses across 20 seconds, with a 0.45-second entry fade and a short blended loop seam. Playback is based on elapsed time with quaternion SLERP, independent of frame rate.

The dance retargets a locally supplied reference excerpt (approximately 97–117 seconds) to Minecraft proportions. Head/torso, upper/lower arms and thighs/calves have independent rotations; shoulders follow the torso around the hip pivot. Hand targets leave space around the large block head and chest. Both foot boxes are evaluated after interpolation to keep the lowest supporting foot on the stage even during entry/loop blends.

`DancePartMixin` activates only on marked celebration limbs. Existing cube faces are cached in one-pixel strips, retaining their UVs; a smooth two-pixel skinning band joins each elbow/knee. Sleeves, trousers and humanoid armor receive the same bend. During the dance, articulated limb overlays use vanilla skin geometry instead of SkinLayers3D's unsliced voxel mesh; head/torso voxel layers remain available. Each ordinary humanoid setup clears the bend flags before checking its marker. The preview viewport has fixed padded bounds; root displacement uses the actual bounds' center so top padding cannot raise the feet.

`animations/cute_dance.json` stores ten global quaternions (body, head, right upper/lower arm, left upper/lower arm, right upper/lower leg, left upper/lower leg), root X and authored root Y per frame. Runtime recomputes root Y from the interpolated geometry. The offline retarget helper is `scripts/retarget_cute_dance.py`; neither pose inference, its model, reference video nor soundtrack is shipped or loaded by Minecraft.

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

The extended summary uses `summary_s2c_v2`, with the existing `summary_close_v1` acknowledgement. Protocol is 34; both sides require the same mod version (currently Alpha0.65). The full immutable roster includes all saved participants (including offline players), true faction, score, role, name and award, plus the recipient’s task count. Capture precedes lobby cleanup. The roster is bounded to 21 unique UUIDs and scores remain signed. GooseThings and the Alpha0.63 datapack hooks are unchanged.

## Verification

Alpha0.65 adds four focused tests for unit rotations, long-running playback, cyclic continuity, independent samples, limb skinning continuity, and actual bent foot corners throughout entry/loop blends. The isolated dance client captured 88 GPU screenshots across a complete loop with armor/cape, then bare slim skin, and exercised Next/Previous/Close. The full Gradle suite passed 4,458 tests with zero failures/errors. The dedicated server reached Done with Alpha0.65/protocol 34 and shut down normally. The existing Alpha0.64 checks below remain the baseline for unchanged result/XP behavior. A final root-height interpolation correction is verified by the foot-corner test rather than starting another client.

- Alpha0.64 full Gradle unit suite: 4,454 tests, zero failures/errors, covering award priority/ties, multi-level and capped XP, pose isolation, roster immutability, duplicate identities, signed scores and room limits.
- Isolated Minecraft client/server regression: actual v2 packets, early black-screen handoff, clothing/cape preservation, wave/dance, observer/zero XP, normal/forced-stop/non-DLC cleanup and duplicate acknowledgement. 60 GPU screenshots; GUI scales 2/3 and resize; SkinLayers3D 1.11.3 loaded.
- Clicks pass through the real Minecraft MouseHandler using current SDL constants, covering Next, Previous, Close and keyboard confirmation. Returning from Previous restores focus to the visible Next button. Neither page changes nor duplicate packets grant rewards or prematurely release deferred promotion.
- Four-player and full 21-player rosters include every faction and offline participants; changing scores and clearing identity storage after capture does not change the received results. Top/bottom screenshots confirm clipping, scroll bounds and fixed footer controls.
- SoundManager listeners observe actual vanilla XP sound events, including a small gain without leveling. Observers and zero-XP summaries remain silent; early page changes stop growth sounds and returning does not replay them.
- Dedicated server reached `Done` with Alpha0.64/protocol 34 and shut down normally. No client-class loading error. Verification uses an isolated world, not a replay of the user's live match.

Original artwork and the successful local ComfyUI PerfectPixel workflow are under `art/summary/`. The runtime atlas retains transparency and a discrete pixel grid.
