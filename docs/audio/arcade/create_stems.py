"""Create original oscillator/noise layers; final mixes are rendered by REAPER.

48 kHz mono, deterministic seed, no third-party recordings or sample libraries.
Run this, then import_render.lua via the existing local REAPER bridge.
"""
from pathlib import Path
import json
import wave
import numpy as np

ROOT = Path(__file__).resolve().parent
SR = 48000
rng = np.random.default_rng(20261010)

def tone(length, f0, f1=None, decay=.07, gain=1, style='soft'):
    t = np.arange(round(length * SR)) / SR
    f1 = f0 if f1 is None else f1
    phase = 2 * np.pi * (f0 * t + (f1-f0) * t*t / (2*length))
    y = np.sin(phase)
    if style == 'chip':
        y += .22*np.sin(3*phase) + .07*np.sin(5*phase)
    else:
        y += .1*np.sin(2*phase)
    envelope = (1-np.exp(-t/.0015))*np.exp(-t/decay)
    envelope *= np.clip((length-t)/.012,0,1)
    return y*envelope*gain

def noise(length, decay, gain, soft=False):
    t = np.arange(round(length*SR))/SR
    n = rng.normal(0,1,len(t))
    n = np.convolve(n, np.ones(16)/16, mode='same') if soft else n-np.roll(n,1)
    return n*(1-np.exp(-t/.001))*np.exp(-t/decay)*np.clip((length-t)/.015,0,1)*gain

def notes(length, seq):
    y = np.zeros(round(length*SR))
    for at, freq, gain in seq:
        start = round(at*SR)
        y[start:] += tone(length-at, freq, decay=.09, gain=gain, style='chip')
    return y

def wav(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), 'wb') as out:
        out.setnchannels(1); out.setsampwidth(2); out.setframerate(SR)
        out.writeframes(np.rint(data*32767).astype('<i2').tobytes())

def main():
    sounds = []
    cursor = 0
    def add(name, layers, peak=-9):
        nonlocal cursor
        mix = sum(layers.values())
        gain = 10**(peak/20)/np.max(np.abs(mix))
        stems = []
        for layer, data in layers.items():
            path = ROOT/'stems'/name/(layer+'.wav')
            wav(path, data*gain)
            stems.append([layer,path.as_posix()])
        duration = len(mix)/SR
        sounds.append(dict(name=name,start=round(cursor,3),duration=duration,stems=stems,peak_dbfs=peak))
        cursor += duration+.35
    add('ui_click', {'body':tone(.09,950,700,decay=.015), 'tap':noise(.09,.004,.025)}, -12)
    add('start', {'melody':notes(.48,[(0,523,.8),(.08,659,.8),(.16,784,1)]), 'sparkle':tone(.48,1568,decay=.11,gain=.08)}, -8)
    add('pause', {'melody':notes(.22,[(0,660,.6),(.07,440,.7)])}, -12)
    add('resume', {'melody':notes(.22,[(0,440,.6),(.07,660,.7)])}, -12)
    add('flap', {'chirp':tone(.115,650,1100,decay=.035,gain=.7,style='chip'), 'air':noise(.115,.025,.12,True)}, -13)
    add('score', {'melody':notes(.28,[(0,1047,.8),(.055,1568,.65)])}, -10)
    add('eat', {'gulp':tone(.17,280,900,decay=.04,style='chip'), 'bite':noise(.17,.009,.04)}, -10)
    add('pong_hit', {'beep':tone(.065,740,decay=.012,style='chip')}, -13)
    add('whack_hit', {'thud':tone(.19,190,75,decay=.034), 'wood':tone(.19,1350,decay=.014,gain=.2), 'tap':noise(.19,.007,.06)}, -9)
    add('miss', {'melody':notes(.25,[(0,330,.7),(.07,247,.6)]), 'body':tone(.25,140,90,decay=.06,gain=.3)}, -11)
    add('reveal', {'tick':tone(.1,1500,950,decay=.015), 'tap':noise(.1,.005,.03)}, -14)
    add('flag', {'body':tone(.12,750,1250,decay=.025,style='chip'), 'snap':noise(.12,.006,.02)}, -13)
    add('slide', {'glide':tone(.14,320,500,decay=.05,gain=.3), 'air':noise(.14,.035,.18,True)}, -14)
    add('merge', {'pop':tone(.22,500,880,decay=.055,style='chip'), 'bell':tone(.22,1320,decay=.05,gain=.16)}, -10)
    add('crash', {'impact':tone(.5,155,38,decay=.09), 'dust':noise(.5,.055,.15,True), 'fall':tone(.5,480,100,decay=.1,gain=.3,style='chip')}, -8)
    add('win', {'melody':notes(.82,[(0,523,.7),(.09,659,.7),(.18,784,.7),(.31,1047,.9)]), 'harmony':notes(.82,[(.31,659,.2),(.31,784,.2)])}, -7)
    add('lane', {'whoosh':noise(.13,.036,.3,True), 'body':tone(.13,240,430,decay=.025,gain=.2)}, -15)
    add('brake', {'rubber':tone(.24,920,400,decay=.065,gain=.6), 'air':noise(.24,.05,.12,True)}, -15)
    (ROOT/'exports').mkdir(exist_ok=True)
    (ROOT/'manifest.json').write_text(json.dumps(sounds,indent=2)+'\n',encoding='utf-8')
    lua = ['-- New tab keeps the previous project intact.', 'reaper.Main_OnCommand(40859,0)',
           'assert(reaper.CountTracks(0)==0,"Expected new project tab")',
           'reaper.GetSetProjectInfo(0,"PROJECT_SRATE",48000,true)',
           'reaper.GetSetProjectInfo(0,"PROJECT_SRATE_USE",1,true)',
           'reaper.GetSetProjectInfo(0,"RENDER_SRATE",48000,true)',
           'reaper.GetSetProjectInfo(0,"RENDER_CHANNELS",1,true)',
           'reaper.GetSetProjectInfo(0,"RENDER_SETTINGS",0,true)',
           'reaper.GetSetProjectInfo(0,"RENDER_BOUNDSFLAG",0,true)',
           'reaper.GetSetProjectInfo(0,"RENDER_TAILFLAG",0,true)',
           'reaper.GetSetProjectInfo(0,"RENDER_NORMALIZE",0,true)',
           'reaper.GetSetProjectInfo_String(0,"RENDER_FORMAT","ZXZhdxgAAQ==",true)',
           f'reaper.GetSetProjectInfo_String(0,"RENDER_FILE",{json.dumps((ROOT/"exports").as_posix())},true)']
    for snd in sounds:
        for layer,path in snd['stems']:
            lua += ['reaper.InsertTrackAtIndex(reaper.CountTracks(0),false)',
                    'local track=reaper.GetTrack(0,reaper.CountTracks(0)-1)',
                    f'reaper.GetSetMediaTrackInfo_String(track,"P_NAME",{json.dumps(snd["name"]+" / "+layer)},true)',
                    'local item=reaper.AddMediaItemToTrack(track)',
                    'local take=reaper.AddTakeToMediaItem(item)',
                    f'local source=assert(reaper.PCM_Source_CreateFromFile({json.dumps(path)}))',
                    'reaper.SetMediaItemTake_Source(take,source)',
                    f'reaper.SetMediaItemInfo_Value(item,"D_POSITION",{snd["start"]})',
                    f'reaper.SetMediaItemInfo_Value(item,"D_LENGTH",{snd["duration"]})',
                    'reaper.SetMediaItemInfo_Value(item,"B_LOOPSRC",0)',
                    'reaper.SetMediaItemInfo_Value(item,"D_FADEINLEN",0)',
                    'reaper.SetMediaItemInfo_Value(item,"D_FADEOUTLEN",0)']
        lua += [f'reaper.AddProjectMarker2(0,true,{snd["start"]},{snd["start"]+snd["duration"]},{json.dumps(snd["name"])},-1,0)']
    lua += ['reaper.TrackList_AdjustWindows(false)','reaper.UpdateArrange()']
    for snd in sounds:
        lua += [f'reaper.GetSetProjectInfo(0,"RENDER_STARTPOS",{snd["start"]},true)',
                f'reaper.GetSetProjectInfo(0,"RENDER_ENDPOS",{snd["start"]+snd["duration"]},true)',
                f'reaper.GetSetProjectInfo_String(0,"RENDER_PATTERN",{json.dumps(snd["name"])},true)',
                'reaper.Main_OnCommand(42230,0)']
    lua += ['reaper.GetSetProjectInfo(0,"RENDER_STARTPOS",0,true)',
            f'reaper.GetSetProjectInfo(0,"RENDER_ENDPOS",{cursor},true)',
            'reaper.GetSetProjectInfo_String(0,"RENDER_PATTERN","preview",true)',
            'reaper.Main_OnCommand(42230,0)',
            f'reaper.Main_SaveProjectEx(0,{json.dumps((ROOT/"GooseTools_Arcade_SFX.rpp").as_posix())},8)',
            'return "Rendered 18 cues and preview through REAPER master; editable project saved."']
    (ROOT/'import_render.lua').write_text('\n'.join(lua)+'\n',encoding='utf-8')
    print('Prepared',len(sounds),'original cues for REAPER rendering.')

if __name__ == '__main__':
    main()
