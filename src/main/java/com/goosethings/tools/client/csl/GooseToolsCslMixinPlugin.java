package com.goosethings.tools.client.csl;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Enables the skin-cache bridge only on clients that actually have CSL installed. */
public final class GooseToolsCslMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("goosetools/csl-bridge");

    private boolean enabled;

    @Override
    public void onLoad(String mixinPackage) {
        enabled = CslModDetector.isLoaded(FabricLoader.getInstance()::isModLoaded);
        if (enabled) {
            LOGGER.info("CustomSkinLoader detected; enabling the GooseTools skin bridge");
        } else {
            LOGGER.debug("CustomSkinLoader is not installed; the GooseTools skin bridge is disabled");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return enabled;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
