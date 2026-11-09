# Board games — Alpha0.5

The native home combines Gomoku, Xiangqi and chess. Select a game, then click an available player's head, or choose computer difficulty and side. Invitation chat links accept and open the game or decline; links expire after 30 seconds. `/boardgames`, `/gomoku`, the configurable F9 key and the inventory/Esc button return to the same game.

The paired Whoiskiller pack adds an item display and interaction target at overworld block **13 65 42**. The display cannot be picked up. GooseTools supplies the spectator gamepad in zero-based slot 1, or slot 2 during meetings. It relocates an occupied slot into an empty inventory slot instead of deleting its contents; a full inventory cannot be displaced. The pack protects the launcher from ghost cleanup and reserves its slot when moving the navigation compass. The existing spectator hotbar facade displays the local inventory without changing spectator mode or the followed player.

## Rules and lifetime

- Every turn has 60 real-time seconds. Hiding or replacing the screen never pauses the server deadline. The hidden-game HUD shows the game, turn and remaining time; a clickable chat link returns to play.
- Gomoku keeps the existing 15×15, five-or-more, no-forbidden-moves rules.
- Xiangqi implements palace/river restrictions, blocked horses/elephants, cannon screens, facing generals and legal king safety. No legal move loses. Stable piece identities track perpetual chase through movement, including protected-rook and exchange exceptions. Repetition classification waits for actual responses, verifies four occurrences for each side and compares check/chase/idle severity. The natural limit is 100 non-capture half-moves, counting at most ten checks by each side. A capture resets that counter; a forward pawn move only resets repetition history.
- Chess implements castling rights and attacked transit squares, immediate en passant including discovered-check rejection, and explicit queen/rook/bishop/knight promotion. Checkmate wins; stalemate and common insufficient-material positions draw. Threefold repetition and 50 moves without pawn movement/capture can be claimed, either in the current position or by selecting an intended legal move. Fivefold repetition and 75 moves draw automatically, with checkmate taking precedence.
- This is a casual 60-second game: resignation, disconnect and timeout use the shared forfeit rule. Lobby preparation, results, revival or leaving the current Goose Goose Duck session cancel without awarding a winner. Meetings and cross-dimension meeting transfers preserve the game and clock. Server shutdown cancels; no games persist to disk.
- Initial PvP sides are random; both players must agree to rematch, then sides swap. A player holds one seat across all game kinds. Lobby and same-session eliminated/spectator groups cannot cross-invite. Being swallowed does not qualify as dead.

## Implementation

`ChessPosition` and `ChessAi` are pure Java. The existing `GomokuRoom` and `GomokuMatch` retain seat, invitation, deadline and revision ownership for all games. New `board_action_v1` and `board_view_v1` channels carry bounded board and legal-move snapshots. Existing arcade and `gomoku_*_v1` formats and protocol 50 are unchanged; exact client/server mod-version matching remains mandatory.

The two-thread AI executor has a bounded queue. Easy/normal use two-/three-ply search, while hard iteratively deepens to six plies with a 400 ms budget; simpler levels use 150 ms. Search keeps a legal fallback, honors interruption and publishes only to the server thread after rechecking game ID, revision, outcome and deadline. Clients cannot submit board contents, adjudications or clocks.

Textures are supplied by the companion resource pack under the intentionally existing `item/chinese_cheese`, `item/international_cheese` and `item/gamepad` paths. No new generated bitmap is used. The existing wooden board is reused, chess squares and Xiangqi lines are native GUI primitives, and the river labels are centered independently in each half. English fallback text and `zh_cn`/`en_us` translations accompany the controls.

## Verification

`gradlew test build` covers rule fixtures, opening move counts (chess 20/400/8902; Xiangqi 44/1920), special moves, checkmate/stalemate, repetition/claims, perpetual check/chase, natural limits, invalid/replayed/stale input, exclusive seats, rematches and AI legality/budgets. Existing Gomoku tests are retained.

`runGomokuServerTest` and two `runGomokuClientTest -PgomokuRole=A|B` processes exercise a real dedicated server and real clients. `-PboardRegressionOnly=true` skips the already-covered Gomoku prefix while preserving the new-game tests and final lifecycle scenarios. Local test resource packs contain only the supplied chess/gamepad art; the test datapack uses the actual lobby entry functions. No regression classes or fixtures are included in the runtime JAR.

The automated scenarios use controlled tags, scoreboards and dimensions; they do not replace a manual acceptance run of a complete production map round with all role mods. Rule fixtures do not claim exhaustive coverage of all 110 WXF diagrams or every FIDE blocked-position dead draw.

Rules references: [FIDE Laws of Chess](https://handbook.fide.com/chapter/e012023), [WXF 2018 rules](https://www.wxf-xiangqi.org/images/wxf-rules/2018_World_XiangQi_Rules_English2018.pdf), and the response-based chase algorithm in [Tan & Watkinson Medina, 2024](https://arxiv.org/html/2412.17334v1). The implementation is original Java; no external engine executable or source code is bundled.

Local validation on 2026-11-09: **4,621 JUnit tests, zero failures/errors/skips**; Alpha0.5 build succeeded. Dedicated-server and both client Gradle runs completed successfully. The full scenario prefix verified Gomoku and lifecycle behavior; the final board-only run verified both new games, all AI levels, paired boards, chat acceptance, actual world right-click, spectator gamepad slots, hidden HUD and lifecycle tail. Eighteen captures were produced in the final run, including the corrected river labels. Windows performance-counter warnings were environmental; no board-game runtime exceptions occurred.
