# Changelog

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
