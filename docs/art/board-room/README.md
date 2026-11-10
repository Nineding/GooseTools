# Board room UI — PixelLab / PerfectPixel

The user approved the warm wood / brass / teal design on 2026-11-10. `lobby-original.png` and `match-original.png` are PixelLab MCP `create_ui_asset` results; the asset IDs and source URLs are in `provenance.json`. Existing chess pieces remain supplied by the companion resource pack.

The included ComfyUI API workflows expand the native one-pixel art by exactly 4 with `nearest-exact`, then run PerfectPixel (`Majority Cluster`, `OpenCV Backend`, `export_scale=1`). This avoids the detector's minimum four-pixel cell size erasing fine borders. Detection recovers the original 512×336 grid. Alpha comes from the original LoadImage mask and is restored through JoinImageWithAlpha. Both execution histories report success. The initially over-coarse results are not shipped.

Runtime copies of `*-pixel.png` live at `assets/goosetools/textures/gui/board_room/{lobby,match}.png`. `BoardGameSkin` samples regions directly, using nine-slice borders for panels and buttons; it never stretches the complete design into the game. Text, avatars, chess pieces, grids, countdowns and highlights are rendered separately. The board retains its established logical coordinates and click transform.

Atlas rectangles (x, y, width, height in pixels):

| Element | Atlas | Rectangle | Source border |
| --- | --- | --- | --- |
| Window frame (no center) | match | 23, 18, 467, 300 | 12 |
| Header | match | 36, 30, 441, 26 | 5 |
| Wooden board | match | 38, 63, 246, 242 | 12 |
| Information panel / HUD | match | 299, 64, 177, 241 | 12 |
| Player and AI rail | lobby | 38, 39, 439, 70 | 9 |
| Normal button | lobby | 383, 240, 73, 22 | 5 |
| Selected / primary button | lobby | 305, 240, 71, 22 | 5 |
| Game card | lobby | 38, 115, 140, 101 | Full region |

The HTML design previews are not claimed as Minecraft validation. Actual screenshots and test results are recorded in `docs/board-games.md` after the native client regression.
