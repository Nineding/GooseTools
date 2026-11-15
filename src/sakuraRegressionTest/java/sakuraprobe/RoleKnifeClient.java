package sakuraprobe;

import com.goosethings.tools.client.punchy.PunchySkillSword;
import com.goosethings.tools.client.punchy.mixin.ItemRenderLayerAccess;
import com.goosethings.tools.client.punchy.mixin.ItemRenderStateAccess;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Runs against the published JAR and real optional mods, never inside the release artifact. */
public final class RoleKnifeClient implements ClientModInitializer {
    private static final Item COOLDOWN = BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:yellow_stained_glass_pane"));
    private final Path root = Path.of(System.getProperty("sakura.probe"));
    private final String role = System.getProperty("sakura.role");
    private final boolean punchy = FabricLoader.getInstance().isModLoaded("punchy");
    private final long deadline = System.nanoTime() + 380_000_000_000L;
    private List<String> knives;
    private final List<String> weapons = new ArrayList<>();
    private int stage, ticks, weapon, view, combo, lastTrigger, checks;
    private boolean connected, done;
    private Object controller;
    private Method movesSword, movesGroup, punchyKind;
    private Object fallbackKind;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }

    private void tick(Minecraft mc) {
        if (done) {
            if (Files.exists(root.resolve(role.equals("A") ? "B-result" : "A-result"))) mc.stop();
            return;
        }
        if (!mc.isGameLoadFinished()) return;
        try {
            check(System.nanoTime() < deadline, "timeout at stage " + stage + " weapon " + weapon);
            if (!connected) {
                if (!Files.exists(root.resolve("server-ready"))) return;
                connected = true;
                mc.options.pauseOnLostFocus = false;
                String address = "127.0.0.1:" + System.getProperty("sakura.port");
                ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address),
                        new ServerData("Role knife proof", address, ServerData.Type.OTHER), false, null);
                return;
            }
            if (mc.gui.screen() != null && mc.gui.screen().getClass().getName().contains("punchymoves.client.tutorial")) {
                mc.gui.screen().onClose();
                return;
            }
            if (mc.player == null || mc.level == null || !mc.player.connection.hasClientLoaded()) return;
            if (stage == 0) {
                if (mc.level.players().size() < 2) return;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                if (++ticks < 30) return;
                if (punchy) {
                    var engine = Class.forName("punchymoves.client.PlayerAnimationEngine");
                    var state = ((Map<?, ?>) field(engine, "PLAYERS").get(null)).get(mc.player);
                    if (state == null) return;
                    controller = field(state.getClass(), "controller").get(state);
                    if (controller == null) return;
                    movesSword = controller.getClass().getDeclaredMethod("isSword", ItemStack.class);
                    movesSword.setAccessible(true);
                    movesGroup = Class.forName("punchymoves.client.animation.ItemAnimationBindings")
                            .getDeclaredMethod("groupFor", ItemStack.class);
                    movesGroup.setAccessible(true);
                    var kind = Class.forName("punchy.client.renderer.layer.VanillaFirstPersonItemLayer$ToolKind");
                    fallbackKind = kind.getEnumConstants()[0];
                    punchyKind = Class.forName("punchy.client.render.ToolKindResolver")
                            .getDeclaredMethod("resolveItemKind", ItemStack.class, kind);
                }
                verifyModels(mc);
                weapons.add("iron");
                weapons.addAll(knives);
                weapons.addAll(List.of("sakura_sword", "sakura_sword_charging", "blade_of_flames"));
                equip(mc);
                return;
            }
            if (stage == 1) {
                if (++ticks < 32) return;
                if (punchy && view == 1) {
                    check((boolean) movesSword.invoke(null, mc.player.getMainHandItem()), "Moves sword: " + current());
                    lastTrigger = comboTick();
                }
                attack(mc);
                stage = 2;
                ticks = 0;
                return;
            }
            if (stage == 2) {
                ++ticks;
                if (ticks == 2) mc.options.keyAttack.setDown(false);
                if (punchy && view == 1) {
                    if (comboTick() == lastTrigger) {
                        check(ticks < 18, "no native combo for " + current());
                        return;
                    }
                    var layer = field(controller.getClass(), "swordAttack").get(controller);
                    var clip = field(layer.getClass(), "clip").get(layer);
                    check(clip != null, "no sword clip for " + current());
                    check(clip.getClass().getMethod("name").invoke(clip).equals("sword_attack_" + (combo + 1)),
                            "wrong native clip for " + current() + "/" + combo);
                }
                if (ticks == 3 && combo == 0 && (weapon == 1 || weapon == 16 || weapon == 33)) {
                    String name = "role-knife-" + role + "-" + view + "-" + current() + ".png";
                    Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
                        try (image) { image.writeToFile(root.resolve(name)); }
                        catch (Exception error) { throw new RuntimeException(error); }
                    });
                }
                if (ticks < 6) return;
                if (punchy && view == 1 && combo == 0) {
                    combo = 1;
                    lastTrigger = comboTick();
                    stage = 3;
                    return;
                }
                if (++weapon == weapons.size()) {
                    weapon = 0;
                    if (++view == 2) {
                        finish(mc, "PASS " + checks + " checks; 33 role models x ready/cooldown/overridden base x both hands; "
                                + "private views; 37 weapons x first/third-person real attacks; "
                                + (punchy ? "74 native sword combo clips" : "optional mods absent"));
                        return;
                    }
                }
                equip(mc);
                return;
            }
            if (stage == 3 && mc.player.tickCount - lastTrigger >= 13) {
                attack(mc);
                stage = 2;
                ticks = 0;
            }
        } catch (Throwable error) {
            error.printStackTrace();
            finish(mc, "FAIL " + current() + ": " + error);
        }
    }

    private void verifyModels(Minecraft mc) throws Exception {
        Path definitions = Path.of(System.getProperty("roleknife.pack"), "assets/ggd/items/role_knife");
        try (var files = Files.list(definitions)) {
            knives = files.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().replace(".json", "")).sorted().toList();
        }
        check(knives.size() == 33, "unexpected resource-pack model count");
        var remote = mc.level.players().stream().filter(p -> p != mc.player).findFirst().orElseThrow();
        var remoteOriginal = remote.getMainHandItem();
        for (String name : knives) for (Item base : List.of(Items.CARROT_ON_A_STICK, COOLDOWN, Items.PAPER)) {
            var stack = knife(name, base);
            check(PunchySkillSword.matches(stack) && !PunchySkillSword.isSakura(stack), "classification: " + name);
            if (punchy) check(((Enum<?>) punchyKind.invoke(null, stack, fallbackKind)).name().equals("SWORD"), "Punchy kind: " + name);
            mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            mc.player.setItemInHand(InteractionHand.OFF_HAND, stack.copy());
            for (var context : List.of(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND)) {
                var render = new ItemStackRenderState();
                mc.getItemModelResolver().updateForLiving(render, stack, context, mc.player);
                check(!render.isEmpty(), "missing first-person model: " + name);
            }
            for (var camera : List.of(CameraType.THIRD_PERSON_BACK, CameraType.THIRD_PERSON_FRONT)) {
                mc.options.setCameraType(camera);
                var state = armed(mc, mc.player);
                check(visible(state.rightHandItemState) && visible(state.leftHandItemState), "owner grip: " + name);
                if (punchy) {
                    check((boolean) movesSword.invoke(null, stack), "Moves recognition: " + name);
                    check(movesGroup.invoke(null, stack).equals("sword"), "Moves binding: " + name);
                    check(!(boolean) movesSword.invoke(null, stack.copy()), "non-held stack inherited private group");
                }
            }
            remote.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
            check(!visible(armed(mc, remote).getMainHandItemState()), "remote knife visible: " + name);
            remote.setItemInHand(InteractionHand.MAIN_HAND, remoteOriginal);
            mc.setCameraEntity(remote);
            check(!visible(armed(mc, mc.player).getMainHandItemState()), "detached camera leak: " + name);
            mc.setCameraEntity(mc.player);
            var profile = mc.player.connection.getPlayerInfo(mc.player.getUUID());
            var mode = profile.getGameMode();
            var modeField = field(PlayerInfo.class, "gameMode");
            modeField.set(profile, GameType.SPECTATOR);
            check(!visible(armed(mc, mc.player).getMainHandItemState()), "spectator knife visible: " + name);
            if (punchy) check(!(boolean) movesSword.invoke(null, stack), "spectator inherited sword actions");
            modeField.set(profile, mode);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            if (punchy) check(!(boolean) movesSword.invoke(null, stack), "third-person Moves override leaked into first person");
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of("task"), List.of()));
            check(!PunchySkillSword.matches(stack), "non-knife discriminator accepted: " + name);
        }
        check(!PunchySkillSword.matches(new ItemStack(Items.CARROT_ON_A_STICK)), "generic skill item is a sword");
        check(!PunchySkillSword.matches(ItemStack.EMPTY) && !PunchySkillSword.matches(null), "empty item is a sword");
        Files.writeString(root.resolve(role + "-models"), "PASS " + checks + " resource/recognition/private-view checks");
    }

    private static ItemStack knife(String name, Item base) {
        var stack = new ItemStack(base);
        stack.set(DataComponents.ITEM_MODEL, Identifier.parse("ggd:role_knife/" + name));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of("15"), List.of()));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, base == Items.CARROT_ON_A_STICK);
        if (base == COOLDOWN) stack.setCount(37);
        return stack;
    }

    private void equip(Minecraft mc) {
        var name = current();
        ItemStack stack;
        if (name.equals("iron")) stack = new ItemStack(Items.IRON_SWORD);
        else if (knives.contains(name)) stack = knife(name, Items.CARROT_ON_A_STICK);
        else {
            stack = new ItemStack(Items.CARROT_ON_A_STICK);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(name), List.of()));
        }
        mc.player.getInventory().setSelectedSlot(0);
        mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        mc.player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        mc.options.setCameraType(view == 0 ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
        mc.player.setXRot(view == 0 ? 8 : -30);
        combo = 0;
        stage = 1;
        ticks = 0;
    }

    private void attack(Minecraft mc) throws Exception {
        mc.options.keyAttack.setDown(true);
        var hit = mc.hitResult;
        mc.hitResult = BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO);
        try {
            var method = Minecraft.class.getDeclaredMethod("startAttack");
            method.setAccessible(true);
            field(Minecraft.class, "missTime").setInt(mc, 0);
            method.invoke(mc);
        } finally { mc.hitResult = hit; }
    }

    private int comboTick() throws Exception {
        var state = field(controller.getClass(), "attackCombo").get(controller);
        return field(state.getClass(), "lastTriggerTick").getInt(state);
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        var result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static ArmedEntityRenderState armed(Minecraft mc, net.minecraft.world.entity.LivingEntity holder) {
        var state = new ArmedEntityRenderState();
        ArmedEntityRenderState.extractArmedEntityRenderState(holder, state, mc.getItemModelResolver(), 1);
        return state;
    }

    private static boolean visible(ItemStackRenderState state) {
        var access = (ItemRenderStateAccess) state;
        for (int i = 0; i < access.goosetools$getActiveLayerCount(); i++) {
            var scale = ((ItemRenderLayerAccess) access.goosetools$getLayers()[i]).goosetools$getItemTransform().scale();
            if (scale.x() != 0 && scale.y() != 0) return true;
        }
        return false;
    }

    private String current() { return weapons.isEmpty() ? "preflight" : weapons.get(weapon); }

    private void finish(Minecraft mc, String result) {
        done = true;
        mc.options.keyAttack.setDown(false);
        try { Files.writeString(root.resolve(role + "-result"), result); }
        catch (Exception error) { error.printStackTrace(); }
        if (result.startsWith("FAIL")) mc.stop();
    }
}
