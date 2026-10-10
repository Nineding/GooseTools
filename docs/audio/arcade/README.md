# Arcade sound project

18 original short sounds for GooseTools, authored from deterministic oscillator
and noise layers, mixed and rendered through REAPER 7.82. No external recordings
or sample libraries are used. These assets are covered by the repository license.

Open `GooseTools_Arcade_SFX.rpp` in REAPER. It has relative media paths, separate
layer tracks and named regions. Each region uses the master mix at 48 kHz mono.
Peaks are between -15 and -7 dBFS, before the game's 0.85 UI playback gain.
Use Minecraft's master and UI sound volume sliders to adjust playback.

The order is click, start, pause, resume, flap, score, eat, pong hit, hammer hit,
miss, reveal, flag, slide, merge, crash, win, lane change and brake.

To regenerate the source layers, run `python create_stems.py` (requires NumPy).
Run the resulting `import_render.lua` as a REAPER ReaScript: it creates a new
project tab, imports the layers, renders 18 WAVs and a preview into `exports/`,
and saves the project. It leaves the previous project tab intact. Export files
must not already exist, so review REAPER's overwrite prompt on a repeat run.

Run `python encode_ogg.py` (requires NumPy and soundfile) to encode and verify
the rendered WAVs and regenerate `sounds.json` in the repository's resource tree.
The shipped `assets/goosetools/sounds/game/*.ogg` files are Ogg Vorbis encodings
of those REAPER renders. Keep them mono at 48 kHz with the same names and lengths.
`GameSoundCues` selects sounds from public authoritative snapshots. Flap audio
is predicted locally; its server echo is skipped. No audio packet is added.
