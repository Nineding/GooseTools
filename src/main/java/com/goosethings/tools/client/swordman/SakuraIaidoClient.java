package com.goosethings.tools.client.swordman;

import com.goosethings.tools.client.punchy.PunchySkillSword;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/** The owner-only effect never sends poses, equipment, particles or new attack inputs. */
public final class SakuraIaidoClient {
    private static final SakuraIaidoTimeline CLOCK=new SakuraIaidoTimeline();
    private static int ticks, slot=-1;
    private static Object level;
    private SakuraIaidoClient() { }
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            ticks++;
            if(mc.level!=level || mc.player==null || mc.player.getInventory().getSelectedSlot()!=slot) {
                CLOCK.clear();level=mc.level;slot=mc.player==null?-1:mc.player.getInventory().getSelectedSlot();
            }
            boolean valid=eligible();
            boolean charging=valid && "sakura_sword_charging".equals(mc.player.getMainHandItem().get(DataComponents.CUSTOM_MODEL_DATA).getString(0));
            CLOCK.tick(ticks/20F,valid,charging,mc.options.keyUse.isDown());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->clear());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.fromNamespaceAndPath("goosetools","sakura_iaido"),
                (ResourceManagerReloadListener) manager->{SakuraIaidoRenderer.reload(manager);clear();});
    }
    private static void clear() { CLOCK.clear();slot=-1;level=null; }
    public static boolean eligible() {
        var mc=Minecraft.getInstance();var p=mc.player;
        return p!=null && mc.level!=null && mc.getCameraEntity()==p && p.isAlive() && !p.isSpectator()
                && !p.isInvisible() && !p.isSleeping() && !p.isPassenger() && !p.isFallFlying()
                && mc.gui.screen()==null && PunchySkillSword.isSakura(p.getMainHandItem());
    }
    public static boolean active() { return eligible() && CLOCK.active(); }
    public static void approvedDash() { CLOCK.approvedDash(ticks/20F,eligible()); }
    public static SakuraIaidoPose.Frame frame(float partial) {
        return active()?SakuraIaidoPose.sample(CLOCK.phase(),CLOCK.elapsed((ticks+partial)/20F)):SakuraIaidoPose.idle();
    }
    public static boolean owns(net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
        var mc=Minecraft.getInstance();
        return eligible() && state.id==mc.player.getId() && !mc.options.getCameraType().isFirstPerson();
    }
}
