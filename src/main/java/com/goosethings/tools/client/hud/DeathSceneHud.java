package com.goosethings.tools.client.hud;

import com.goosethings.tools.death.*;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A short victim-only vignette. Never takes screen focus, moves the camera, or pauses simulation. */
public final class DeathSceneHud {
    private static DeathScenePayload active;
    private static MeetingAlertRemotePlayer victim;
    private static long started, lastEvent = Long.MIN_VALUE;
    private static final Map<Item, ItemStack> PROPS = new HashMap<>();
    private DeathSceneHud() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DeathScenePayload.TYPE, (p,c) -> c.client().execute(() -> apply(p)));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c) -> { clear(); lastEvent = Long.MIN_VALUE; });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("goosetools", "death_scene"), (g,d) -> render(g));
    }
    private static void apply(DeathScenePayload payload) {
        if (payload.event() <= lastEvent) return;
        lastEvent = payload.event();
        clear();
        var mc = Minecraft.getInstance();
        if (payload.victim() == null || mc.player == null || mc.level == null || !payload.victim().id().equals(mc.player.getUUID())) return;
        active = payload; started = System.nanoTime();
        var appearance = payload.victim();
        victim = new MeetingAlertRemotePlayer(mc.level,
                new GameProfile(UUID.nameUUIDFromBytes(("death-scene:" + appearance.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8)), appearance.name()),
                () -> {
                    var cached = MeetingAlertTrueSkinCache.get(appearance.id());
                    var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(appearance.id());
                    return cached != null ? cached : info != null ? info.getSkin() : DefaultPlayerSkin.get(appearance.id());
                });
        victim.showAllSkinLayers(); victim.setId(0x6A130001);
        for (var slot : EquipmentSlot.values()) victim.setItemSlot(slot, appearance.equipment().get(slot.ordinal()).copy());
        victim.refreshDimensions();
    }
    private static void clear() { active = null; victim = null; }
    private static void render(GuiGraphicsExtractor g) {
        if (active == null) return;
        var mc = Minecraft.getInstance();
        double t = (System.nanoTime() - started) / 1_000_000_000.0;
        if (mc.level == null || mc.player == null || t >= DeathSceneKind.DURATION
                || mc.gui.screen() instanceof PostgameSummaryScreen || mc.gui.screen() instanceof RoleIntroScreen) { clear(); return; }
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        float s = Math.min(w * .92F / 600, h * .74F / 280);
        int left = Math.round(w * .5F - 300 * s), top = Math.round(h * .5F - 140 * s);
        float motion = mc.options.screenEffectScale().get().floatValue();
        double alpha = DeathSceneKind.opacity(t), hit = DeathSceneKind.ease((t - DeathSceneKind.IMPACT) / .55);
        int accent = active.kind().color;
        g.fill(0, 0, w, h, color(0x05080D, alpha * .58));
        box(g,left,top,s,-3,-3,603,283,color(accent,alpha*.32));
        box(g,left,top,s,0,0,600,280,color(0x10151F,alpha));
        box(g,left,top,s,0,0,600,3,color(accent,alpha));
        box(g,left,top,s,0,45,600,46,color(0xFFFFFF,alpha*.1));
        box(g,left,top,s,0,224,600,280,color(0x090E17,alpha));
        for (int i=0;i<16;i++) box(g,left,top,s,38+i*35,205,52+i*35,206,color(accent,alpha*.14));
        text(g,mc,Component.translatable("death.goosetools.title"),left+26*s,top+19*s,s,color(0xF0F1F5,alpha),false);
        text(g,mc,Component.translatable("death.goosetools.private"),left+510*s,top+21*s,.7F*s,color(0xADB7C9,alpha),true);
        if (t < DeathSceneKind.DURATION - DeathSceneKind.EXIT) {
            float fall = motion <= .01F ? 0 : (float)hit;
            float vx=350, vy=205, rotation=fall*78;
            switch(active.kind()) {
                case VOID, FALL, EXILE -> { vy += fall*50; rotation=fall*130; }
                case DROWN -> { vy += fall*22; rotation=fall*18; }
                case TRUCK -> { vx += fall*115; rotation=fall*105; }
                case SHERIFF -> { vx=385; rotation=fall*70; }
                default -> {}
            }
            if (motion <= .01F || t >= .12) model(g,mc,left+vx*s,top+vy*s,52*s,rotation);
            props(g,left,top,s,t,hit,motion,accent);
            double flash = motion > .01F ? Math.max(0,1-Math.abs(t-DeathSceneKind.IMPACT)/.09)*.08 : 0;
            if (flash>0) box(g,left,top,s,4,49,596,220,color(accent,flash));
        }
        String key="death.goosetools.kind."+active.kind().id();
        text(g,mc,Component.translatable(key),left+300*s,top+237*s,1.15F*s,color(accent,alpha),true);
        text(g,mc,Component.translatable("death.goosetools.continue"),left+300*s,top+257*s,.72F*s,color(0xADB7C9,alpha),true);
        box(g,left,top,s,0,278,(float)(600*DeathSceneKind.clamp(t/DeathSceneKind.DURATION)),280,color(accent,alpha));
    }
    private static void props(GuiGraphicsExtractor g,int x,int y,float s,double t,double hit,float motion,int accent) {
        double approach=DeathSceneKind.ease((t-.2)/.65);
        float a=motion<=.01F?1:(float)approach, f=motion<=.01F?0:(float)hit;
        switch(active.kind()) {
            case KNIFE -> { silhouette(g,x,y,s,185,203,0); prop(g,Items.IRON_SWORD,x+(220+90*a)*s,y+126*s,3*s,-75+120*a); }
            case SPECIAL -> { prop(g,Items.ENCHANTED_BOOK,x+220*s,y+140*s,3*s,-15*a); orbit(g,Items.AMETHYST_SHARD,x,y,s,t,motion); }
            case EXPLOSION -> { prop(g,Items.TNT,x+280*s,y+174*s,3*s,0); sparks(g,x,y,s,f,accent); }
            case SNIPER, MEETING_SHOT -> {
                prop(g,Items.CROSSBOW,x+195*s,y+145*s,4*s,-15);
                if(t>=DeathSceneKind.IMPACT && t<1.25) prop(g,Items.ARROW,x+(245+120*f)*s,y+126*s,2*s,-45);
                if(active.kind()==DeathSceneKind.MEETING_SHOT) {box(g,x,y,s,385,188,435,195,0xFF5E332D);box(g,x,y,s,392,195,398,217,0xFF806858);}
                sparks(g,x,y,s,f,accent);
            }
            case SHERIFF -> {
                silhouette(g,x,y,s,185+f*22,203,f*65);
                prop(g,Items.GOLD_NUGGET,x+190*s,y+123*s,2*s,0);
                prop(g,Items.CROSSBOW,x+256*s,y+150*s,3*s,25*f); sparks(g,x,y,s,f,accent);
            }
            case LOVER -> {
                silhouette(g,x,y,s,205,203,f*65);
                for(int i=0;i<5;i++)prop(g,Items.REDSTONE,x+(242+i*18+(i-2)*f*9)*s,y+(135+f*25)*s,.75F*s,i*12*f);
                prop(g,Items.POPPY,x+(280-f*20)*s,y+(111+f*35)*s,2*s,-40*f);
            }
            case PARASITE -> {
                prop(g,Items.FERMENTED_SPIDER_EYE,x+235*s,y+140*s,3*s,30*f);
                for(int i=0;i<4;i++)prop(g,Items.IRON_CHAIN,x+(270+i*20)*s,y+(136+((i%2)*2-1)*f*33)*s,1.2F*s,90+f*i*24);
            }
            case TRUCK -> {
                prop(g,Items.MINECART,x+(75+a*220+f*120)*s,y+180*s,6*s,0);
                prop(g,Items.IRON_BLOCK,x+(75+a*220+f*120)*s,y+139*s,3*s,0); sparks(g,x,y,s,f,accent);
            }
            case FALL, VOID, EXILE -> {
                box(g,x,y,s,170,202,310,211,0xFF555F70);
                if(active.kind()==DeathSceneKind.VOID) prop(g,Items.ENDER_PEARL,x+232*s,y+148*s,3*s,0);
                else prop(g,Items.GRAVEL,x+220*s,y+(147+f*35)*s,3*s,20*f);
            }
            case DROWN -> {
                for(int i=0;i<8;i++){float by=198-(float)((t*30+i*17)%100)*motion;prop(g,Items.PRISMARINE_CRYSTALS,x+(260+i*18)*s,y+by*s,.65F*s,0);}
                box(g,x,y,s,170,178,455,215,0x504EBDD2);prop(g,Items.WATER_BUCKET,x+205*s,y+150*s,3*s,0);
            }
            case CRUSH -> prop(g,Items.ANVIL,x+350*s,y+(70+a*80)*s,5*s,0);
            case LAVA -> {prop(g,Items.LAVA_BUCKET,x+220*s,y+145*s,3*s,35*a); sparks(g,x,y,s,f,accent);}
            case DEVOUR -> {prop(g,Items.BUNDLE,x+275*s,y+170*s,(4+2*f)*s,0);prop(g,Items.FEATHER,x+230*s,y+(120-f*20)*s,2*s,30*f);}
            case UNKNOWN -> prop(g,Items.WITHER_ROSE,x+240*s,y+155*s,3*s,0);
        }
    }
    private static void model(GuiGraphicsExtractor g,Minecraft mc,float cx,float floor,float scale,float tilt) {
        if(victim==null)return;
        if(mc.getCameraEntity()!=null)victim.setPos(mc.getCameraEntity().position());
        var state=mc.getEntityRenderDispatcher().getRenderer(victim).createRenderState(victim,1);
        state.shadowPieces.clear();state.nameTag=null;state.outlineColor=0;
        if(state instanceof LivingEntityRenderState living){living.bodyRot=180;living.yRot=-12;living.xRot=0;living.walkAnimationSpeed=0;}
        float angle=(float)Math.toRadians(tilt),sin=(float)Math.sin(angle),cos=(float)Math.cos(angle);
        // Rotate around the torso, and fit both upright and fallen poses inside the render target.
        float cy=floor-scale*(.9F*Math.abs(cos)+.4F*Math.abs(sin));
        g.entity(state,Math.max(1,Math.round(scale)),new Vector3f(-.9F*sin,.9F*cos,0),new Quaternionf().rotateZ((float)Math.PI+angle),new Quaternionf(),
                Math.round(cx-scale*1.2F),Math.round(cy-scale*1.2F),Math.round(cx+scale*1.2F),Math.round(cy+scale*1.2F));
    }
    private static void silhouette(GuiGraphicsExtractor g,int x,int y,float s,float cx,float floor,float tilt){
        g.pose().pushMatrix();g.pose().translate(x+cx*s,y+(floor-50)*s);g.pose().rotate((float)Math.toRadians(tilt));g.pose().scale(s,s);
        g.fill(-14,-52,14,-24,0xFF41495A);g.fill(-18,-23,18,24,0xFF293243);g.fill(-16,24,-3,50,0xFF232B3A);g.fill(3,24,16,50,0xFF232B3A);
        g.fill(-25,-20,-18,15,0xFF394354);g.fill(18,-20,25,15,0xFF394354);g.pose().popMatrix();
    }
    private static void orbit(GuiGraphicsExtractor g,Item item,int x,int y,float s,double t,float motion){
        for(int i=0;i<5;i++){double a=i*Math.PI*2/5+t*2*motion;prop(g,item,x+(float)(342+Math.cos(a)*54)*s,y+(float)(145+Math.sin(a)*38)*s,1.2F*s,(float)Math.toDegrees(a));}
    }
    private static void sparks(GuiGraphicsExtractor g,int x,int y,float s,float f,int accent){
        if(f<=0||f>=1)return;
        for(int i=0;i<8;i++){double a=i*Math.PI/4;float px=350+(float)Math.cos(a)*f*65,py=130+(float)Math.sin(a)*f*42;box(g,x,y,s,px,py,px+3,py+3,color(accent,1-f));}
    }
    private static void prop(GuiGraphicsExtractor g,Item item,float x,float y,float scale,float angle){
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().rotate((float)Math.toRadians(angle));g.pose().scale(scale,scale);
        g.item(PROPS.computeIfAbsent(item,ItemStack::new),-8,-8,0xD34D);g.pose().popMatrix();
    }
    private static void box(GuiGraphicsExtractor g,int x,int y,float s,float x1,float y1,float x2,float y2,int c){g.fill(Math.round(x+x1*s),Math.round(y+y1*s),Math.round(x+x2*s),Math.round(y+y2*s),c);}
    private static int color(int rgb,double alpha){return ((int)(255*DeathSceneKind.clamp(alpha))<<24)|(rgb&0xFFFFFF);}
    private static void text(GuiGraphicsExtractor g,Minecraft mc,Component text,float x,float y,float s,int color,boolean center){
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(s,s);g.text(mc.font,text,center?-mc.font.width(text)/2:0,0,color,false);g.pose().popMatrix();
    }
}
