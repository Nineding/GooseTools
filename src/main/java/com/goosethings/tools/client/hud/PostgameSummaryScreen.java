package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.animation.CelebrationPose;
import com.goosethings.tools.client.animation.CelebrationState;
import com.goosethings.tools.client.input.ProtectedInputScreen;
import com.goosethings.tools.intro.RoleIntroCatalog;
import com.goosethings.tools.intro.RoleIntroScene;
import com.goosethings.tools.result.*;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.UUID;
import java.util.LinkedHashSet;

/** One frozen award, one recipient's growth; the screen never changes the world player. */
public final class PostgameSummaryScreen extends Screen implements ProtectedInputScreen {
    private static final Identifier ATLAS=Identifier.fromNamespaceAndPath("goosetools","textures/gui/summary/atlas.png");
    private static final LinkedHashSet<UUID> SEEN=new LinkedHashSet<>();
    private static UUID waiting;
    public static boolean waiting() {return waiting!=null;}
    private final SummaryPayload payload;
    private final long started=System.nanoTime();
    private MeetingAlertRemotePlayer model;
    private boolean skipped,acknowledged;
    private int soundedLevel;
    private float levelFlash;
    public PostgameSummaryScreen(SummaryPayload payload) {
        super(tr("title","Match summary")); this.payload=payload; soundedLevel=payload.startLevel();
    }
    public SummaryPayload payload() {return payload;}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SummaryPayload.TYPE,(p,c)->c.client().execute(()->apply(p)));
        ClientPlayNetworking.registerGlobalReceiver(SummaryControlPayload.TYPE,(p,c)->c.client().execute(()->{
            if(p.round().equals(waiting)) {waiting=null;PresentationBlackoutScreen.apply(false);}
            if(c.client().gui.screen() instanceof PostgameSummaryScreen s && s.payload.round().equals(p.round())) {
                s.acknowledged=true;c.client().gui.setScreen(null);
            }
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((h,mc)->{SEEN.clear();waiting=null;});
    }
    public static void apply(SummaryPayload p) {
        var mc=Minecraft.getInstance(); if(mc.level==null||mc.player==null||SEEN.contains(p.round())) return;
        if(!p.open()) {waiting=p.round();return;}
        waiting=null;SEEN.add(p.round());
        if(SEEN.size()>32) SEEN.remove(SEEN.getFirst());
        mc.gui.setScreen(new PostgameSummaryScreen(p));
    }
    private double seconds() {return (System.nanoTime()-started)/1e9;}
    private double progress() {return skipped?1:Math.clamp((seconds()-.85)/4.9,0,1);}
    private float scale() {return Math.min(width/640F,height/360F);}
    private float ox() {return (width-640*scale())*.5F;}
    private float oy() {return (height-360*scale())*.5F;}
    private static Component tr(String key,String fallback,Object...args) {return Component.translatableWithFallback("summary.goosetools."+key,fallback,args);}
    @Override public boolean isPauseScreen() {return false;}
    @Override public void tick() {
        if(minecraft.player==null||minecraft.level==null) {minecraft.gui.setScreen(null);return;}
        var state=SummaryProgress.at(payload,progress());
        if(state.level()>soundedLevel) {
            soundedLevel=state.level();levelFlash=1;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP,1.2F,.45F));
        }
        levelFlash=Math.max(0,levelFlash-.065F);
    }
    @Override public void onClose() {
        if(progress()<1) {skipped=true;return;}
        acknowledge(); minecraft.gui.setScreen(null);
    }
    private void acknowledge() {
        if(!acknowledged&&minecraft.getConnection()!=null&&ClientPlayNetworking.canSend(SummaryControlPayload.TYPE))
            ClientPlayNetworking.send(new SummaryControlPayload(payload.round()));
        acknowledged=true;
    }
    @Override public void removed() {acknowledge();model=null;}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice) {
        float s=scale();double x=(e.x()-ox())/s,y=(e.y()-oy())/s;
        if(e.button()==0&&x>=382&&x<=596&&y>=325&&y<=347) {onClose();return true;}
        return super.mouseClicked(e,twice);
    }
    @Override public boolean keyPressed(KeyEvent e) {
        if(e.key()==257||e.key()==32) {onClose();return true;}
        return super.keyPressed(e);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial) {
        double time=seconds(); float s=scale(),x0=ox(),y0=oy();
        g.fillGradient(0,0,width,height,0xFF111C30,0xFF050A14);
        g.pose().pushMatrix();g.pose().translate(x0,y0);g.pose().scale(s,s);
        // Restrained stage beams and inset panels leave the model as the focal point.
        for(int i=0;i<24;i++) {
            int w=34+i*8,top=72+i*7;
            g.fill(171-w/2,top,171+w/2,top+8,0x036ABDD0);
        }
        g.fill(318,70,610,314,0x78101A2B);g.outline(318,70,292,244,0x304C6477);
        g.fill(318,70,610,72,0xFFDFC180);
        label(g,title,40,27,1.65F,0xFFF7E8BD);
        label(g,tr("subtitle","A moment for the best. Rewards for your journey."),42,51,.85F,0xFF8FA6BE);
        g.horizontalLine(42,597,65,0x405C718B);
        for(int i=0;i<16;i++) {
            double phase=time*.12+i*.618;
            int px=62+(int)((i*43.7)%223),py=84+(int)((phase%1)*190);
            int alpha=(int)(80*(.5+.5*Math.sin(time*2+i)));
            g.fill(px,py,px+1,py+2,alpha<<24|0xEDD995);
        }
        icon(g,0,137,76,68);
        centered(g,payload.award()==2?Component.literal("SMVP"):payload.award()==1?Component.literal("MVP"):tr("review","Round review"),171,144,1.35F,0xFFFFD47C);
        // Floor under the animated feet.
        for(int i=0;i<7;i++) g.fill(105+i*4,269+i,238-i*4,271+i,0x0ADCE8A9);
        g.pose().popMatrix();
        drawPlayer(g,time,s,x0,y0);
        g.pose().pushMatrix();g.pose().translate(x0,y0);g.pose().scale(s,s);
        var hero=payload.appearance().roster().getFirst();
        MeetingNameTagHud.drawFrozen(g,minecraft,hero.name(),hero.rgb(),hero.serial(),hero.lover(),171,283,1.05F,242);
        var role=RoleIntroCatalog.byTag(hero.role());
        centered(g,Component.translatableWithFallback(role.key(),role.fallback()),171,299,.85F,0xFF000000|role.color());
        centered(g,tr("index","Index %s  ·  %s",payload.spotlightIndex(),SummaryPolicy.grade(payload.spotlightIndex())),171,316,.82F,0xFF91A7BD);
        drawGrowth(g);
        boolean hover=mouseX>=x0+382*s&&mouseX<=x0+596*s&&mouseY>=y0+325*s&&mouseY<=y0+347*s;
        g.fill(382,325,596,347,hover?0xFF36546C:0xFF233B50);g.outline(382,325,214,22,0xFF587488);
        centered(g,progress()<1?tr("skip","Skip animation"):tr("return","Return to lobby"),489,332,.9F,0xFFE8F0F8);
        g.pose().popMatrix();
        float opacity=(float)Math.max(0,1-time/.5);
        if(opacity>0) {g.nextStratum();g.fill(0,0,width,height,Math.round(opacity*255)<<24);}
    }
    private void drawGrowth(GuiGraphicsExtractor g) {
        label(g,tr("growth","Your growth"),336,82,.95F,0xFFD3E4EF);
        if(!payload.participant()) {
            icon(g,2,337,110,58);
            label(g,tr("observer","Spectating this round"),405,122,1.0F,0xFFB9CDDD);
            label(g,tr("no_rewards","No personal rewards this round"),337,192,.9F,0xFF91A7BD);return;
        }
        var p=SummaryProgress.at(payload,progress());
        float badge=48+levelFlash*7;
        icon(g,2,335-(badge-48)/2,101-(badge-48)/2,badge);
        label(g,tr("level","Lv. %s",p.level()),399,106,1.8F,0xFFE9F9FF);
        label(g,tr("earned","+%s XP",p.received()),400,133,1.05F,0xFF79E4D5);
        g.fill(336,155,594,163,0xFF060C16);
        int fill=(int)(256*Math.clamp(p.xp()/p.needed(),0,1));
        if(fill>0) {g.fillGradient(337,156,337+fill,162,0xFF86F2E7,0xFF34A8C5);g.fill(336+fill,155,338+fill,163,0xFFDCFFFF);}
        Component detail=p.level()>=payload.cap()&&payload.bankGain()>0
                ?tr("bank","Level cap reached · %s XP stored",payload.bankGain())
                :tr("progress","%s / %s XP",(int)Math.floor(p.xp()),p.needed());
        label(g,detail,337,170,.78F,0xFF8FB7CA);
        g.horizontalLine(336,594,189,0x34455F76);
        label(g,tr("rewards","Round rewards"),337,198,.9F,0xFFE9D9B0);
        double t=skipped?99:seconds();
        row(g,t,.6,217,tr("base","Base XP"),Component.literal("+"+payload.baseXp()),0xFFB4C9DA);
        row(g,t,.8,234,tr("bonus","Award bonus"),Component.literal("+"+payload.bonusPercent()+"%"),0xFFEACD86);
        row(g,t,1,251,tr("death","Death adjustment"),Component.literal("−"+payload.deathPercent()+"%"),payload.deathPercent()>0?0xFFDB999F:0xFF71869B);
        row(g,t,1.2,272,tr("total","XP earned"),Component.literal("+"+payload.gainedXp()+" XP"),0xFF8FF3D5);
        icon(g,1,334,288,21);
        row(g,t,1.4,295,tr("coins","Goose coins"),Component.literal("+"+payload.coins()),0xFFFFD37C,359);
        if(levelFlash>0) icon(g,3,553,104,27+levelFlash*13);
    }
    private void row(GuiGraphicsExtractor g,double t,double delay,int y,Component key,Component value,int color) {row(g,t,delay,y,key,value,color,337);}
    private void row(GuiGraphicsExtractor g,double t,double delay,int y,Component key,Component value,int color,int left) {
        float f=(float)Math.clamp((t-delay)/.35,0,1);if(f<=0)return;
        int a=Math.max(4,Math.round(f*255))<<24,dy=(int)((1-f)*5);
        label(g,key,left,y+dy,.85F,a|0xB0C0CE);
        label(g,value,594-font.width(value)*.9F,y+dy,.9F,a|(color&0xffffff));
    }
    private void drawPlayer(GuiGraphicsExtractor g,double seconds,float s,float x,float y) {
        if(minecraft.level==null) return;
        var hero=payload.appearance().roster().getFirst();
        if(model==null) {
            model=new MeetingAlertRemotePlayer(minecraft.level,new GameProfile(RoleIntroScene.modelId(hero.id()),hero.name()),()->{
                var skin=MeetingAlertTrueSkinCache.get(hero.id());if(skin!=null)return skin;
                var info=minecraft.getConnection()==null?null:minecraft.getConnection().getPlayerInfo(hero.id());
                return info==null?DefaultPlayerSkin.get(hero.id()):info.getSkin();
            });
            model.setId(0x6A140001);model.showAllSkinLayers();model.setInvisible(false);
            for(var slot:EquipmentSlot.values()) model.setItemSlot(slot,slot==EquipmentSlot.MAINHAND||slot==EquipmentSlot.OFFHAND?ItemStack.EMPTY:hero.equipment().get(slot.ordinal()).copy());
        }
        MeetingAlertTrueSkinCache.capture(minecraft);
        var camera=minecraft.getCameraEntity();if(camera!=null) model.setPos(camera.position());
        var state=minecraft.getEntityRenderDispatcher().getRenderer(model).createRenderState(model,1);
        state.nameTag=null;state.outlineColor=0;state.shadowPieces.clear();state.isInvisible=false;
        state.lightCoords=0x00F000F0;
        if(state instanceof AvatarRenderState living) {
            living.bodyRot=180;living.yRot=0;living.xRot=0;living.walkAnimationSpeed=0;living.isInvisibleToPlayer=false;living.isSpectator=false;
            ((CelebrationState)living).goosetools$celebration(payload.action(),(float)seconds);
        }
        var pose=CelebrationPose.at(payload.action(),seconds);
        float pixels=55*s,top=y+153*s,floor=y+(271-pose.lift()*55)*s;
        g.entity(state,pixels,new Vector3f(0,(floor-top)*.5F/pixels,0),new Quaternionf().rotateZ((float)Math.PI),new Quaternionf(),
                Math.round(x+80*s),Math.round(top-16*s),Math.round(x+262*s),Math.round(floor));
    }
    private void icon(GuiGraphicsExtractor g,int cell,float x,float y,float size) {
        float u=(cell%2)*.5F,v=(cell/2)*.5F;
        g.blit(ATLAS,Math.round(x),Math.round(y),Math.round(x+size),Math.round(y+size),u,u+.5F,v,v+.5F);
    }
    private void label(GuiGraphicsExtractor g,Component text,float x,float y,float scale,int color) {
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);g.text(font,text,0,0,color,false);g.pose().popMatrix();
    }
    private void centered(GuiGraphicsExtractor g,Component text,float x,float y,float scale,int color) {label(g,text,x-font.width(text)*scale/2,y,scale,color);}
}
