package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.animation.CuteDanceAnimation;
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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
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
    private static final FontDescription SYMBOLS=new FontDescription.Resource(Identifier.fromNamespaceAndPath("goosetools","summary_symbols"));
    private static final LinkedHashSet<UUID> SEEN=new LinkedHashSet<>();
    private static UUID waiting;
    private static SummaryPayload readyToDisplay;
    public static boolean waiting() {return waiting!=null;}
    private final SummaryPayload payload;
    private final long started=System.nanoTime();
    private MeetingAlertRemotePlayer model;
    private boolean skipped,acknowledged;
    private int soundedLevel;
    private float levelFlash;
    private boolean rosterPage;
    private double rosterScroll, nextXpSound;
    private SimpleSoundInstance xpSound, levelSound;
    private Button advanceButton, backButton;
    public PostgameSummaryScreen(SummaryPayload payload) {
        super(tr("title","Match summary")); this.payload=payload; soundedLevel=payload.startLevel();
    }
    public SummaryPayload payload() {return payload;}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SummaryPayload.TYPE,(p,c)->c.client().execute(()->apply(p)));
        ClientPlayNetworking.registerGlobalReceiver(SummaryControlPayload.TYPE,(p,c)->c.client().execute(()->{
            if(p.round().equals(waiting)) {waiting=null;readyToDisplay=null;PresentationBlackoutScreen.apply(false);}
            if(c.client().gui.screen() instanceof PostgameSummaryScreen s && s.payload.round().equals(p.round())) {
                s.acknowledged=true;c.client().gui.setScreen(null);
            }
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((h,mc)->{SEEN.clear();waiting=null;readyToDisplay=null;});
    }
    public static void apply(SummaryPayload p) {
        var mc=Minecraft.getInstance(); if(mc.level==null||mc.player==null||SEEN.contains(p.round())) return;
        if(!p.open()) {waiting=p.round();return;}
        if(mc.gui.screen() instanceof PresentationBlackoutScreen cover&&cover.ending()&&!cover.fadeComplete()) {
            waiting=p.round();readyToDisplay=p;return;
        }
        show(p);
    }
    static boolean revealReady() {
        if(readyToDisplay==null) return false;
        show(readyToDisplay);return true;
    }
    private static void show(SummaryPayload p) {
        var mc=Minecraft.getInstance();
        waiting=null;readyToDisplay=null;SEEN.add(p.round());
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
    @Override protected void init() {
        // Real widgets own their visible bounds, focus, narration and click handling.
        advanceButton=addRenderableWidget(new FooterButton(382,214,tr("next","Next page"),b->{
            if(rosterPage) onClose(); else setRosterPage(true);
        }));
        backButton=addRenderableWidget(new FooterButton(42,110,tr("previous","Previous page"),b->setRosterPage(false)));
        updateButtons();
        setInitialFocus(advanceButton);
    }
    private void setRosterPage(boolean value) {
        skipped=true; rosterPage=value; stopGrowthSounds();
        soundedLevel=payload.endLevel(); updateButtons(); setFocused(advanceButton);
    }
    private void updateButtons() {
        advanceButton.setMessage(rosterPage?tr("close","Close"):tr("next","Next page"));
        backButton.visible=rosterPage;
    }
    @Override public void tick() {
        if(minecraft.player==null||minecraft.level==null) {minecraft.gui.setScreen(null);return;}
        double fraction=progress();
        var state=SummaryProgress.at(payload,fraction);
        if(!skipped&&!rosterPage&&payload.participant()&&payload.gainedXp()>0) {
            if(state.level()>soundedLevel) {
                soundedLevel=state.level();levelFlash=1;
                if(levelSound!=null) minecraft.getSoundManager().stop(levelSound);
                levelSound=SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP,1.2F,.4F);
                minecraft.getSoundManager().play(levelSound);
                nextXpSound=seconds()+.24;
            } else if(fraction>0&&fraction<1&&seconds()>=nextXpSound) {
                if(xpSound!=null) minecraft.getSoundManager().stop(xpSound);
                xpSound=SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP,.85F+(float)fraction*.55F,.16F);
                minecraft.getSoundManager().play(xpSound);
                nextXpSound=seconds()+.18;
            }
        }
        levelFlash=Math.max(0,levelFlash-.065F);
    }
    private void stopGrowthSounds() {
        if(xpSound!=null) {minecraft.getSoundManager().stop(xpSound);xpSound=null;}
        if(levelSound!=null) {minecraft.getSoundManager().stop(levelSound);levelSound=null;}
    }
    @Override public void onClose() {
        stopGrowthSounds(); acknowledge(); minecraft.gui.setScreen(null);
    }
    private void acknowledge() {
        if(!acknowledged&&minecraft.getConnection()!=null&&ClientPlayNetworking.canSend(SummaryControlPayload.TYPE))
            ClientPlayNetworking.send(new SummaryControlPayload(payload.round()));
        acknowledged=true;
    }
    @Override public void removed() {stopGrowthSounds();acknowledge();model=null;}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        boolean previous=rosterPage,handled=super.mouseClicked(event,twice);
        // The container assigns focus after invoking the button callback. Do not leave it
        // on the now-hidden Previous button after returning to personal growth.
        if(previous!=rosterPage) setFocused(advanceButton);
        return handled;
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(rosterPage) {rosterScroll=Math.clamp(rosterScroll-vertical*26,0,maxRosterScroll());return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    private double maxRosterScroll() {
        long groups=payload.scoreboard().players().stream().map(SummaryRoster.Entry::faction).distinct().count();
        return Math.max(0,payload.scoreboard().players().size()*26+groups*20-188);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial) {
        double time=seconds(); float s=scale(),x0=ox(),y0=oy();
        g.fillGradient(0,0,width,height,0xFF111C30,0xFF050A14);
        g.pose().pushMatrix();g.pose().translate(x0,y0);g.pose().scale(s,s);
        if(rosterPage) {
            drawRoster(g); g.pose().popMatrix();
            super.extractRenderState(g,mouseX,mouseY,partial); return;
        }
        // Restrained stage beams and inset panels leave the model as the focal point.
        for(int i=0;i<24;i++) {
            int w=34+i*8,top=72+i*7;
            g.fill(171-w/2,top,171+w/2,top+8,0x036ABDD0);
        }
        g.fill(318,70,610,314,0x78101A2B);g.outline(318,70,292,244,0x304C6477);
        g.fill(318,70,610,72,0xFFDFC180);
        label(g,title,40,27,1.65F,0xFFF7E8BD);
        g.horizontalLine(42,597,65,0x405C718B);
        for(int i=0;i<16;i++) {
            double phase=time*.12+i*.618;
            int px=62+(int)((i*43.7)%223),py=84+(int)((phase%1)*190);
            int alpha=(int)(80*(.5+.5*Math.sin(time*2+i)));
            g.fill(px,py,px+1,py+2,alpha<<24|0xEDD995);
        }
        if(payload.award()>0) centered(g,symbol(payload.award()==2?'\uE063':'\uE064'),171,106,3F,0xFFFFFFFF);
        else centered(g,tr("highest_index","Highest index this round"),171,118,1.1F,0xFFFFD47C);
        // Floor under the animated feet.
        for(int i=0;i<7;i++) g.fill(105+i*4,269+i,238-i*4,271+i,0x0ADCE8A9);
        g.pose().popMatrix();
        drawPlayer(g,time,s,x0,y0);
        g.pose().pushMatrix();g.pose().translate(x0,y0);g.pose().scale(s,s);
        var hero=payload.appearance().roster().getFirst();
        MeetingNameTagHud.drawFrozen(g,minecraft,hero.name(),hero.rgb(),hero.serial(),hero.lover(),171,283,1.05F,242);
        var role=RoleIntroCatalog.byTag(hero.role());
        centered(g,Component.translatableWithFallback(role.key(),role.fallback()),171,299,.85F,0xFF000000|role.color());
        centered(g,tr("index","Index %s  ·  %s",payload.spotlightIndex(),gradeSymbol(payload.spotlightIndex())),171,316,.95F,0xFF91A7BD);
        drawGrowth(g);
        g.pose().popMatrix();
        super.extractRenderState(g,mouseX,mouseY,partial);
        float opacity=(float)Math.max(0,1-time/.5);
        if(opacity>0) {g.nextStratum();g.fill(0,0,width,height,Math.round(opacity*255)<<24);}
    }
    private void drawRoster(GuiGraphicsExtractor g) {
        label(g,tr("roster","All players' index"),42,27,1.65F,0xFFF7E8BD);
        label(g,tr("page","%s / %s",2,2),555,33,.9F,0xFF91A7BD);
        g.horizontalLine(42,597,65,0x405C718B);
        g.fill(42,75,598,294,0x78101A2B);
        label(g,tr("player","Player"),78,82,.85F,0xFF91A7BD);
        label(g,tr("role","Role"),299,82,.85F,0xFF91A7BD);
        label(g,tr("index_header","Goose Duck index"),430,82,.85F,0xFF91A7BD);
        label(g,tr("grade","Grade"),523,82,.85F,0xFF91A7BD);
        g.enableScissor(44,102,589,290);
        int y=102-(int)rosterScroll,previous=-1;
        for(var player:payload.scoreboard().players()) {
            if(player.faction()!=previous) {
                previous=player.faction();
                int color=switch(previous) {case 0->0xFF7BEAA6;case 1->0xFFFF848A;default->0xFFF3D878;};
                String key=switch(previous) {case 0->"geese";case 1->"ducks";default->"birds";};
                String fallback=switch(previous) {case 0->"Geese";case 1->"Ducks";default->"Birds";};
                g.fill(47,y,587,y+18,0x55192B3F);g.fill(47,y,49,y+18,color);
                label(g,tr(key,fallback),56,y+5,.9F,color);y+=20;
            }
            if(y+26>102&&y<290) {
                var skin=MeetingAlertTrueSkinCache.get(player.id());
                if(skin==null) {
                    var info=minecraft.getConnection()==null?null:minecraft.getConnection().getPlayerInfo(player.id());
                    skin=info==null?DefaultPlayerSkin.get(player.id()):info.getSkin();
                }
                PlayerFaceExtractor.extractRenderState(g,skin,56,y+4,16);
                Component name=Component.literal(player.name());
                label(g,name,80,y+8,Math.min(.95F,204F/Math.max(1,font.width(name))),0xFF000000|(player.lover()?0xFFB6D7:player.rgb()));
                var role=RoleIntroCatalog.byTag(player.role());
                Component roleName=Component.translatableWithFallback(role.key(),role.fallback());
                label(g,roleName,299,y+8,Math.min(.85F,122F/Math.max(1,font.width(roleName))),0xFF000000|role.color());
                label(g,Component.literal(Integer.toString(player.index())),442,y+8,.95F,0xFF79E4C1);
                centered(g,gradeSymbol(player.index()),537,y+8,1.35F,0xFFFFFFFF);
                if(player.award()>0) centered(g,symbol(player.award()==2?'\uE063':'\uE064'),575,y+9,.8F,0xFFFFFFFF);
                g.horizontalLine(55,584,y+25,0x18394E65);
            }
            y+=26;
        }
        g.disableScissor();
        double max=maxRosterScroll();
        if(max>0) {
            int thumb=Math.max(18,(int)(188*188/(max+188)));
            int top=102+(int)(rosterScroll/max*(188-thumb));
            g.fill(592,102,595,290,0xFF1C3043);g.fill(592,top,595,top+thumb,0xFF8FB7CA);
        }
        label(g,tr("tasks","Tasks completed this round: %s",payload.scoreboard().tasksDone()),43,304,.9F,0xFFB0C0CE);
        if(max>0) label(g,tr("scroll","Scroll to view all players"),434,304,.8F,0xFF91A7BD);
    }
    private static Component gradeSymbol(int index) {
        return symbol(switch(SummaryPolicy.grade(index)) {
            case "F" -> '\uE06A'; case "D" -> '\uE06B'; case "C" -> '\uE06C'; case "N" -> '\uE06D';
            case "A" -> '\uE06E'; case "S" -> '\uE06F'; case "S+" -> '\uE061'; default -> '\uE062';
        });
    }
    private final class FooterButton extends Button {
        FooterButton(int x,int w,Component message,OnPress press) {
            super(Math.round(ox()+x*scale()),Math.round(oy()+325*scale()),Math.round(w*scale()),
                    Math.round(22*scale()),message,press,DEFAULT_NARRATION);
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial) {
            g.fill(getX(),getY(),getRight(),getBottom(),isHoveredOrFocused()?0xFF36546C:0xFF233B50);
            g.outline(getX(),getY(),getWidth(),getHeight(),isHoveredOrFocused()?0xFFE9D9B0:0xFF587488);
            centered(g,getMessage(),getX()+getWidth()/2F,getY()+7*scale(),.9F*scale(),0xFFE8F0F8);
        }
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
        row(g,t,1,251,tr("death","Death deduction"),Component.literal("−"+payload.deathPercent()+"%"),payload.deathPercent()>0?0xFFDB999F:0xFF71869B);
        row(g,t,1.2,272,tr("total","XP earned"),Component.literal("+"+payload.gainedXp()+" XP"),0xFF8FF3D5);
        label(g,symbol('\uE082'),337,291,1.8F,0xFFFFFFFF);
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
        var state=previewState(seconds);
        var dance=payload.action()==2?CuteDanceAnimation.at(seconds):null;
        float offsetX=dance==null?0:dance.x()*55/16F,offsetY=dance==null?-(float)com.goosethings.tools.client.animation.CelebrationPose.at(payload.action(),seconds).lift()*55:dance.y()*55/16F;
        float pixels=55*s,top=y+125*s,bottom=y+278*s,floor=y+(271+offsetY)*s;
        // Keep the viewport fixed while the planted foot determines the model's root height.
        // Using the old padded top in only the bounds raised the feet eight GUI pixels.
        g.entity(state,pixels,new Vector3f(0,(floor-(top+bottom)*.5F)/pixels,0),new Quaternionf().rotateZ((float)Math.PI),new Quaternionf(),
                Math.round(x+(80+offsetX)*s),Math.round(top),Math.round(x+(262+offsetX)*s),Math.round(bottom));
    }
    net.minecraft.client.renderer.entity.state.EntityRenderState previewState(double seconds) {
        var state=minecraft.getEntityRenderDispatcher().getRenderer(model).createRenderState(model,1);
        state.nameTag=null;state.outlineColor=0;state.shadowPieces.clear();state.isInvisible=false;
        state.lightCoords=0x00F000F0;
        if(state instanceof AvatarRenderState living) {
            living.bodyRot=180;living.yRot=0;living.xRot=0;living.walkAnimationSpeed=0;living.isInvisibleToPlayer=false;living.isSpectator=false;
            // The preview follows the camera without world ticks. Its stale cloak coordinates
            // otherwise look like a huge teleport and force the cape into its flying pose.
            living.capeFlap=0;living.capeLean=0;living.capeLean2=0;
            ((CelebrationState)living).goosetools$celebration(payload.action(),(float)seconds);
        }
        return state;
    }
    private static Component symbol(char glyph) {
        return Component.literal(Character.toString(glyph)).withStyle(s->s.withFont(SYMBOLS).withColor(0xffffff).withItalic(false).withoutShadow());
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
