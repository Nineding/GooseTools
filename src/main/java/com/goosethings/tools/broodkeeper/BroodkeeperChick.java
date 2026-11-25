package com.goosethings.tools.broodkeeper;

import java.util.function.ObjIntConsumer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueOutput;

public final class BroodkeeperChick extends Chicken {
    public static final ResourceKey<EntityType<?>> KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse("goosetools:broodkeeper_chick"));
    private static final EntityDataAccessor<Integer> CARRIER =
            SynchedEntityData.defineId(BroodkeeperChick.class, EntityDataSerializers.INT);
    private static EntityType<BroodkeeperChick> type;

    public static EntityType<BroodkeeperChick> type() {
        return type;
    }

    public static void register() {
        type = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
                EntityType.Builder.of(BroodkeeperChick::new, MobCategory.MISC)
                        .sized(0.5f, 0.47f).clientTrackingRange(12).updateInterval(2).build(KEY));
        FabricDefaultAttributeRegistry.register(type, Chicken.createAttributes());
        FabricLoader.getInstance().getObjectShare().put("goosetools:broodkeeper_carrier",
                (ObjIntConsumer<Entity>) (entity, carrier) -> {
                    if (entity instanceof BroodkeeperChick chick) chick.entityData.set(CARRIER, carrier);
                });
    }

    private BroodkeeperChick(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
        setSilent(true);
        setPersistenceRequired();
    }

    public int carrierId() {
        return entityData.get(CARRIER);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CARRIER, -1);
    }

    @Override protected void registerGoals() {}
    @Override public void aiStep() { eggTime = Integer.MAX_VALUE; super.aiStep(); }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return carrierId() == -1 && !isInvisible(); }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean saveAsPassenger(ValueOutput output) { return false; }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) { return InteractionResult.PASS; }
}
