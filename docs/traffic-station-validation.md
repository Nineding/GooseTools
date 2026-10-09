# Alpha0.33 validation (Minecraft 26.3, Java 25)

- 363 unit tests pass. New coverage includes actual-position traffic collisions,
  continuous steering, brake timeout/release, pause activity exclusion, bounded
  long-road generation for all three difficulties, electrical units, capacity
  selection, under/over compensation, each synchronization interlock, uninterrupted
  hold times, disconnected/overloaded/unbalanced phase distribution, bounded clocks,
  input rejection and snapshot ownership. 1,000 random stations complete all four stages.
- Real client station runs pass both with and without the installed InvMove 0.9.6
  and Cloth Config 26.3.158. The tests open all 17 task/game screens and verify
  vanilla input and InvMove's late update cannot move the player. Ordinary inventory
  movement restores, and a disabled InvMove setting remains disabled.
- The station is solved through commands, actual packets, numeric character input,
  Ctrl+A, Enter, arrow/Shift adjustments, switches and load-card dragging. Incorrect
  calculation, phase sequence and missing loads are rejected; handbook, replay,
  completion and Chinese GUI scales 1/2/3 are checked with GPU screenshots.
- All seven games pass client regression: visible-board play, scoring, pause,
  collision/loss, Pong classic/endless, mine difficulties/flag cycle/win/next board,
  2048 victory/4096/loss, task replacement and ESC/admin/meeting/death cleanup.
  A further traffic run checks held braking, release and final Chinese labels/arrows.
- All original nine tasks complete through real mouse/Space input at GUI scales
  1/2/3, including memory mistake/replay, replacement and ESC/admin/meeting/death cleanup.
- The dedicated server starts, reports the traffic and powerstation commands,
  saves and exits normally. Client-only mixins do not load on the server.
- Runtime artifact validation checks version/protocol, all four traffic sprites and
  absence of regression code. Both language catalogs preserve all 424 previous
  keys and append 77 entries. Existing task and arcade payload layouts are unchanged.

Client screenshots and machine-readable results live in the isolated build run
directories documented in the regression source-set READMEs. These checks exercise
the actual Minecraft client/server; public-server latency and human game feel can
be assessed during player trials.
