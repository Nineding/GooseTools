# GUI task bindings

GooseTools 1.14.0+Alpha0.35 keeps independent command trials and adds an explicit
authoritative bridge for the Whoiskiller Eagleton Springs tasks. Existing game,
task and profession payloads and protocol 30 are unchanged.

The datapack owns assignment, accepted-task tags, task-point distance, arcade
bounds, normal task completion, navigation and cleanup. GooseTools owns the
actual GUI session, game simulation, valid play time, hits and puzzle outcomes.

## Commands and scoreboard contract

Create dummy objectives `ggdGuiState`, `ggdGuiTime`, `ggdGuiHits` before binding.
The player must have `players` and `inTaskGooseGui`, be alive and outside meetings
and the existing unavailable role states, and pass the exact-version handshake.

- `/goosetools gui bind <players> traffic`: immediately starts hard Traffic Dodge;
  succeeds after 30,000 ms active survival. Difficulty and replay are locked.
- `/goosetools gui bind <players> whack`: immediately starts hard Whac-A-Mole;
  succeeds on 20 valid hits before life exhaustion. Difficulty and replay are locked.
- `/goosetools gui bind <players> pipes|knobs|timing`: binds a fresh puzzle session.
- `/goosetools gui bind <players> arcade`: clears pre-acceptance games and starts
  a new station-only cumulative counter. Menus, pause and results do not count.
- `/goosetools gui clear <players>`: removes the binding first, closes both GUI
  kinds and resets published progress. Use for failure, cancellation and cleanup.
- `/goosetools gui release <players>`: removes the binding and resets progress
  without closing the current game. Use after Arcade Fan completion.

`ggdGuiState`: 0 inactive, 1 bound, 2 completed, 3 failed. `ggdGuiTime` is valid
milliseconds, capped at 30,000; `ggdGuiHits` is the Whac-A-Mole hit count, capped
at 20. Failure and success are terminal until rebinding. Closing after success
cannot convert success to failure. Independent player UUIDs and session IDs
prevent stale messages or another player's game from contributing.

The service does not grant task tags or increment normal task counters.
Consumers must validate their map and player rules before handling the published
state. The included datapack finishes through its existing `.finished` pipeline.

## Arcade blocks

Normal eligible map-11 players may right-click these exact overworld blocks:

| Position | Game |
| --- | --- |
| -1669 72 -500 | 2048 |
| -1669 72 -504 | Flappy Bird |
| -1670 72 -507 | Snake |
| -1674 72 -507 | Minesweeper |

The native server block-use callback verifies the target, reach, hand, map,
player eligibility and arcade bounds. Neighbouring blocks do not open a game;
repeated same-tick use does not double-open. Station-origin sessions alone count
for Arcade Fan. Ordinary admin game commands remain independent trials.

Xaero's closed arcade bounds are X [-1702,-1667], Y [71,82], Z [-510,-481]. Closing,
losing and switching games preserve the current cumulative attempt inside the
arcade. The datapack fails and clears it when the player leaves.

## Validation

`./gradlew test build` covers pure authoritative gameplay and existing rules.
`./gradlew runGuiMapRegressionTest` runs real integrated-server map functions,
native block-use packets, game/puzzle Screen inputs, screenshots, lifecycle and
cumulative-time checks. Its isolated test source set is not in the runtime JAR.
Controlled server-player fixtures additionally verify two-player UUID isolation,
stale session handling, task/game ID overlap, exact block mappings and all six
arcade boundary directions. These fixtures never alter production handshake rules.
