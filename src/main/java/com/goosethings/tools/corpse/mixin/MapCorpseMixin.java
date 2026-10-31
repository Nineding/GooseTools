package com.goosethings.tools.corpse.mixin;

import com.goosethings.tools.corpse.MapCorpse;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mannequin.class)
public abstract class MapCorpseMixin implements MapCorpse {
    @Unique private static final EntityDataAccessor<Boolean> goosetools$MAP_CORPSE =
            SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.BOOLEAN);

    @Inject(method="defineSynchedData", at=@At("TAIL"))
    private void goosetools$defineCorpse(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(goosetools$MAP_CORPSE, false);
    }

    @Override public boolean goosetools$isMapCorpse() {
        return ((Mannequin)(Object)this).getEntityData().get(goosetools$MAP_CORPSE);
    }

    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    private void goosetools$readCorpse(ValueInput input, CallbackInfo ci) {
        // Tags also migrate corpses saved before the dedicated flag was introduced.
        Mannequin self=(Mannequin)(Object)this;
        boolean corpse=input.getBooleanOr("ggd_map_corpse", false)
                || self.entityTags().contains("deadbody") && self.entityTags().contains("ggd.mapCorpse");
        self.getEntityData().set(goosetools$MAP_CORPSE, corpse);
    }

    @Inject(method="addAdditionalSaveData", at=@At("TAIL"))
    private void goosetools$saveCorpse(ValueOutput output, CallbackInfo ci) {
        if(goosetools$isMapCorpse()) output.putBoolean("ggd_map_corpse", true);
    }
}
