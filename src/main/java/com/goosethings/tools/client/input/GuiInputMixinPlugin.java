package com.goosethings.tools.client.input;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public final class GuiInputMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String p){}
    @Override public String getRefMapperConfig(){return null;}
    @Override public boolean shouldApplyMixin(String target,String mixin){return !mixin.endsWith("InvMoveGuiGuardMixin")||FabricLoader.getInstance().isModLoaded("invmove");}
    @Override public void acceptTargets(Set<String> a,Set<String> b){}
    @Override public List<String> getMixins(){return null;}
    @Override public void preApply(String a,ClassNode b,String c,IMixinInfo d){}
    @Override public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}
