package com.goosethings.tools.client.ambience;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.ambience.*;
import com.goosethings.tools.client.regions.RegionClient;
import com.goosethings.tools.client.regions.RegionAudioProfiles;
import com.goosethings.tools.client.presence.GamePresenceClient;
import com.goosethings.tools.client.hud.*;
import com.goosethings.tools.client.dream.DreamAvatarClient;

import com.goosethings.tools.regions.*;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.*;

/** Client-only room ambience, driven by existing public presence snapshots and the actual listener. */
public final class ChapelAmbienceClient {
    private record Scene(String id,String key,RegionDefinition.Audio audio){
        String sound(){return audio.sound();} float volume(){return audio.volume();} boolean continuous(){return audio.continuous();}
    }
    private record Waiting(Scene scene,int due){}
    private static final Map<String,Waiting> waiting=new HashMap<>();
    private static final java.util.Random repeatRandom=new java.util.Random();
    private static final Map<String,Loop> owned=new HashMap<>();
    private static final Map<SoundInstance,Integer> music=new IdentityHashMap<>();
    private static final Set<String> missing=new HashSet<>();
    private static final SoundEventListener LISTENER=(sound,event,range)->Minecraft.getInstance().execute(()->heard(sound));
    private static SoundManager manager;private static Object level;
    private static String selected;private static int ticks,quietUntil;
    private ChapelAmbienceClient() {}

    public static void register(){
        ClientTickEvents.END_CLIENT_TICK.register(ChapelAmbienceClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->clear());
    }

    private static void heard(SoundInstance sound){
        if(sound instanceof Loop||Minecraft.getInstance().options.getSoundSourceVolume(sound.getSource())<=0)return;
        var id=sound.getIdentifier();
        if(AmbiencePriority.background(id.getNamespace(),id.getPath(),sound.getSource()==SoundSource.MUSIC))music.put(sound,ticks);
        if(AmbiencePriority.taskNote(id.getNamespace(),id.getPath()))quietUntil=ticks+80;
    }
    private static boolean sessionActive(Minecraft client){
        if(client.player==null||client.level==null)return false;
        if(client.level.dimension().identifier().getNamespace().equals("ggd_training"))return true;
        return switch(GamePresenceClient.phaseId()){case "playing","spectating","meeting"->true;default->false;};
    }
    private static boolean eligible(Minecraft client){
        if(!sessionActive(client))return false;
        if(client.gui.screen() instanceof RoleIntroScreen||client.gui.screen() instanceof PostgameSummaryScreen||client.gui.screen() instanceof PresentationBlackoutScreen||MeetingAlertHud.blocksMovement())return false;
        return !GamePresenceClient.phaseId().equals("meeting")||DreamAvatarClient.active();
    }
    private static void tick(Minecraft client){
        ticks++;
        if(manager!=client.getSoundManager()){
            if(manager!=null){clear();manager.removeListener(LISTENER);}
            manager=client.getSoundManager();manager.addListener(LISTENER);
        }
        if(level!=client.level){clear();level=client.level;}
        if(client.player==null||client.level==null)return;
        // Retain newly queued music for a short grace period before its audio channel becomes active.
        music.entrySet().removeIf(e->client.options.getSoundSourceVolume(e.getKey().getSource())<=0 || ticks-e.getValue()>12&&!manager.isActive(e.getKey()));
        boolean allowed=eligible(client),priority=!music.isEmpty()||!allowed;
        var camera=client.gameRenderer.mainCamera();
        var position=camera.position();
        var scene=allowed&&camera.isInitialized()?selectScene(selected,client.level.dimension().identifier().toString(),position.x,position.y,position.z):null;
        selected=scene==null?null:scene.id();
        var wanted=priority?null:scene;
        if(client.options.getSoundSourceVolume(SoundSource.AMBIENT)<=0||client.options.getSoundSourceVolume(SoundSource.MASTER)<=0)wanted=null;
        final Scene audible=wanted;
        // EOF is different from a deliberate fade-out: schedule once, after the complete clip.
        for(var iterator=owned.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();Loop loop=entry.getValue();
            boolean ended=loop.isStopped()||ticks-loop.started>30&&!manager.isActive(loop);
            if(ended){
                manager.stop(loop);iterator.remove();
                if(loop.audio.delayed()&&!loop.faded&&sessionActive(client)&&(loop.continuous||audible!=null&&entry.getKey().equals(audible.key())))
                    waiting.put(entry.getKey(),new Waiting(loop.description,ticks+loop.audio.nextDelayTicks(repeatRandom)));
            }
        }
        waiting.entrySet().removeIf(e->!sessionActive(client)||!e.getValue().scene().continuous()&&(audible==null||!e.getKey().equals(audible.key()))
            ||audible!=null&&e.getKey().equals(audible.key())&&!e.getValue().scene().audio().samePlayback(audible.audio()));
        for(var iterator=waiting.entrySet().iterator();iterator.hasNext();){var e=iterator.next();
            if(ticks<e.getValue().due())continue;
            Scene next=audible!=null&&e.getKey().equals(audible.key())?audible:e.getValue().scene();
            play(next,audible!=null&&e.getKey().equals(audible.key()));iterator.remove();
        }
        if(wanted!=null&&wanted.volume()>0){
            Loop loop=owned.get(wanted.key());
            if(loop!=null&&!loop.audio.samePlayback(wanted.audio())){manager.stop(loop);owned.remove(wanted.key());loop=null;}
            if(loop==null&&!waiting.containsKey(wanted.key()))loop=play(wanted,true);
            if(loop!=null){loop.description=wanted;loop.fade(wanted.volume()*(ticks<quietUntil?0.18F:1),ticks<quietUntil?4:10);}
        }
        for(var iterator=owned.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();Loop loop=entry.getValue();
            loop.retain=loop.continuous&&sessionActive(client);
            if(wanted==null||!entry.getKey().equals(wanted.key()))loop.fade(0,priority?4:14);
            if(loop.isStopped()){manager.stop(loop);iterator.remove();}
        }
    }
    private static Loop play(Scene scene,boolean audible){
        var id=Identifier.parse(scene.sound());
        if(manager.getSoundEvent(id)==null){if(missing.add(scene.sound()))GooseTools.LOGGER.warn("Missing region ambience sound {}; install the matching resource pack",id);return null;}
        var loop=new Loop(scene,ticks);loop.fade(audible?scene.volume()*(ticks<quietUntil?0.18F:1):0,ticks<quietUntil?4:10);
        owned.put(scene.key(),loop);manager.play(loop);return loop;
    }

    private static Scene selectScene(String previous,String dimension,double x,double y,double z){
        var regions=new LinkedHashMap<String,RegionDefinition>(RegionAudioProfiles.profiles());
        var world=RegionClient.catalog();if(world!=null){world.regions().forEach(r->regions.put(r.id(),r));world.deleted().forEach(regions::remove);}
        var candidates=regions.values().stream().filter(r->r.dimension().equals(dimension)||dimension.equals("ggd_training:"+r.map())).toList();
        var picked=RegionResolver.resolve(candidates,x,y,z,r->RegionAudioProfiles.insideMap(r.map(),x,z));
        if(picked==null)return null;
        var audio=RegionAudioProfiles.effective(picked);if(audio==null||audio.sound().isEmpty()||audio.volume()<=0)return null;
        return new Scene(picked.id(),AmbienceClock.key(picked.map(),picked.id(),audio),audio);
    }

    private static void clear(){
        if(manager!=null)for(var loop:owned.values())manager.stop(loop);
        owned.clear();waiting.clear();music.clear();missing.clear();selected=null;quietUntil=0;
    }
    static final class Loop extends AbstractTickableSoundInstance {
        final String scene;final int started;final boolean continuous;final RegionDefinition.Audio audio;Scene description;boolean retain,faded;final AmbienceEnvelope envelope=new AmbienceEnvelope();
        Loop(Scene description,int started){super(SoundEvent.createVariableRangeEvent(Identifier.parse(description.sound())),SoundSource.AMBIENT,RandomSource.create());
            this.description=description;this.audio=description.audio();this.scene=description.key();this.started=started;this.continuous=audio.continuous();this.retain=continuous;
            this.volume=0;this.pitch=1;this.looping=!audio.delayed();this.delay=0;this.relative=true;this.attenuation=SoundInstance.Attenuation.NONE;
        }
        void fade(float value,int ticks){envelope.target(value,ticks);}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){volume=envelope.tick();if(envelope.silent()&&!retain){faded=true;stop();}}
    }
}
