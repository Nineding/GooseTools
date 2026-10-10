"""Encode REAPER WAV renders for Minecraft (requires NumPy and soundfile)."""
from pathlib import Path
import json
import numpy as np
import soundfile as sf

root = Path(__file__).resolve().parent
assets = root.parents[2] / 'src/main/resources/assets/goosetools'
output = assets / 'sounds/game'
output.mkdir(parents=True, exist_ok=True)
events = {}
for item in json.loads((root / 'manifest.json').read_text()):
    name = item['name']
    data, sr = sf.read(root / 'exports' / (name + '.wav'))
    assert sr == 48000 and data.ndim == 1 and np.isfinite(data).all()
    assert abs(len(data) / sr - item['duration']) < .001 and np.max(np.abs(data)) < .95
    target = output / (name + '.ogg')
    sf.write(target, data, sr, format='OGG', subtype='VORBIS')
    decoded, rate = sf.read(target)
    assert rate == sr and len(decoded) == len(data) and np.max(np.abs(decoded)) < .98
    assert np.max(np.abs(decoded[-48:])) < .006 and abs(decoded[-1]) < .003
    events['game.' + name] = {'sounds': [{'name': 'goosetools:game/' + name, 'stream': False}]}
(assets / 'sounds.json').write_text(json.dumps(events, indent=2) + '\n', encoding='utf-8')
print('Encoded and verified', len(events), 'mono 48 kHz Ogg Vorbis sound events.')
