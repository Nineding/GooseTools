# Goosechapel Fishing Master art

PixelLab Pro Flash generated the panel, three fish, launcher, crosshair, net, close button, two counter states and hotbar icon. `pixellab-jobs.json` records their job IDs and `*-original.png` contains the original sprites.

`workflow.json` contains the ComfyUI graphs. Each graph scales the native pixel grid by four using nearest-exact, recovers the grid with PerfectPixel, then restores alpha from the original sprite. `history.json` records the successful executions. The panel recovered at 208 × 160 and all sprites recovered at their original sizes.

The runtime panel and sprites live under `assets/goosetools/textures/gui/tasks/fishing/`. The companion Whoiskiller resource pack uses `trigger-final.png` for `assets/minecraft/textures/item/fishing_task.png`.
