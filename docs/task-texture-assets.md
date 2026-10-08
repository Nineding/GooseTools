# Task trial texture sources

The timing dial, card reader, bin and knob sprites were generated with Codex's built-in image-generation tool on 2026-10-08. That tool exposes no model selector; no specific GPT Image model is asserted for this generation. The prompt is saved in `task-texture-prompt.txt`.

The generated transparent atlas was split into four equal quadrants. Each quadrant was processed through the existing local ComfyUI `PerfectPixel` node: `LoadImage -> PerfectPixel -> SaveImage`, with `sampling = Majority Cluster`, `export_scale = 1`, `backend = OpenCV Backend`. The API execution completed successfully; the four node outputs measured 72x69, 39x41, 68x68 and 76x76 pixels respectively.

PerfectPixel's IMAGE result contains RGB only. The original alpha mask was resized with nearest sampling, thresholded to binary alpha, and applied to the output. Transparent margins were trimmed. The palette was reduced to 24 colors without dithering. Sprite export dimensions are dial 128x128, reader 128x40, bin 64x96 and knob 64x64. Only the reader's empty middle casing/slot was widened; its corner proportions were preserved. Minecraft texture metadata explicitly disables blur.

`key_card.png` is copied from the project's existing `resourcepacks/whois/assets/minecraft/textures/item/white_key_card.png`. It is bundled so the task does not require the external resource pack. Garbage uses native Minecraft ItemStack GUI rendering: glass bottle, paper, rotten flesh, bone, poisonous potato and stick; active resource packs therefore apply to these items.

All task textures are installed with the mod. ComfyUI and an image-generation service are development tools only, with no runtime dependency or downloads. Targets, needles, status lights, wires, text and progress are rendered dynamically; static art contains no task outcome.
