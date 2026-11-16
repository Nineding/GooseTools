package sakuraprobe;

import com.google.gson.JsonParser;
import com.goosethings.tools.client.item.ManagedHotbarRefresh;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

/** Tests the actual vanilla/Punchy hand state machines using the packaged mod. */
public final class SkillRefreshClient implements ClientModInitializer {
    private record Button(String item, String model, String definition, int slot) { }
    private final Path root = Path.of(System.getProperty("sakura.probe"));
    private final String role = System.getProperty("sakura.role");
    private final boolean punchy = FabricLoader.getInstance().isModLoaded("punchy");
    private final List<Button> buttons = new ArrayList<>();
    private final long deadline = System.nanoTime() + 340_000_000_000L;
    private Object machine;
    private int stage, ticks, index, seconds = 3, checks;
    private boolean connecting, done, vanillaSwitch, punchySwitch;

    @Override public void onInitializeClient() { ClientTickEvents.END_CLIENT_TICK.register(this::tick); }

    private void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private void tick(Minecraft mc) {
        if (done) {
            if (Files.exists(root.resolve(role.equals("A") ? "B-result" : "A-result"))) mc.stop();
            return;
        }
        if (!mc.isGameLoadFinished()) return;
        try {
            if (System.nanoTime() > deadline) throw new AssertionError("timeout at " + index + "/" + stage);
            if (!connecting) {
                if (!Files.exists(root.resolve("server-ready"))) return;
                connecting = true;
                String address = "127.0.0.1:" + System.getProperty("sakura.port");
                ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address),
                        new ServerData("Skill refresh proof", address, ServerData.Type.OTHER), false, null);
                return;
            }
            if (mc.gui.screen() != null && mc.gui.screen().getClass().getName().contains("punchymoves.client.tutorial")) {
                mc.gui.screen().onClose();
                return;
            }
            if (mc.player == null || !mc.player.connection.hasClientLoaded() || mc.level.players().size() < 2) return;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            if (stage == 0) {
                if (!Files.exists(root.resolve("tutorial-result"))) return;
                for (var value : JsonParser.parseString(Files.readString(root.resolve("skill-refresh-cases.json"))).getAsJsonArray()) {
                    var object = value.getAsJsonObject();
                    buttons.add(new Button(object.get("item").getAsString(), object.get("model").getAsString(),
                            object.get("definition").getAsString(), object.get("slot").getAsInt()));
                }
                check(buttons.size() >= 33, "incomplete source inventory coverage");
                equip(mc);
                return;
            }
            if (stage == 1) {
                if (++ticks < 32) return;
                if (punchy) {
                    machine = field(Class.forName("punchy.client.state.HandEquipStateMachine"), "instance").get(null);
                    check(machine != null && !busy(), "initial Punchy equip did not settle");
                }
                check(height(mc) >= .95F, "initial vanilla equip did not settle");
                var previous = mc.player.getMainHandItem();
                var next = stack(buttons.get(index), --seconds);
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, next);
                check(ManagedHotbarRefresh.matches(previous, mc.player.getMainHandItem(), mc.player),
                        "display refresh did not match unified rule: slot=" + mc.player.getInventory().getSelectedSlot()
                        + " ref=" + (next == mc.player.getMainHandItem())
                        + " model=" + Objects.equals(previous.get(DataComponents.ITEM_MODEL), next.get(DataComponents.ITEM_MODEL))
                        + " customModel=" + Objects.equals(previous.get(DataComponents.CUSTOM_MODEL_DATA), next.get(DataComponents.CUSTOM_MODEL_DATA))
                        + " previous=" + previous.get(DataComponents.CUSTOM_DATA) + " next=" + next.get(DataComponents.CUSTOM_DATA));
                stage = 2;
                return;
            }
            if (stage == 2) {
                var current = mc.player.getMainHandItem();
                check(current.getCount() == Math.max(1, seconds)
                                && current.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                                        .getStringOr("ggdHotbarState", "").equals("fixture_" + seconds),
                        "current held stack did not receive countdown " + seconds);
                var shown = (ItemStack) field(FirstPersonHandsAndItems.class, "mainHandItem").get(mc.player.firstPersonHandsAndItems());
                check(height(mc) >= .95F, "vanilla hand lowered at " + index + "/" + seconds
                        + " height=" + height(mc) + " swapScale=" + mc.player.getItemSwapScale(1)
                        + " busy=" + mc.player.isHandsBusy() + " match=" + ManagedHotbarRefresh.matches(shown, current, mc.player)
                        + " shown=" + shown.get(DataComponents.CUSTOM_DATA) + " current=" + current.get(DataComponents.CUSTOM_DATA));
                verifyDisplay(shown, current, "vanilla");
                if (punchy) {
                    check(!busy(), "Punchy replayed equip at " + index + "/" + seconds);
                    verifyDisplay((ItemStack) field(machine.getClass(), "renderedMain").get(machine), current, "Punchy");
                }
                if (seconds-- > 0) {
                    mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack(buttons.get(index), seconds));
                    return;
                }
                if (++index < buttons.size()) { equip(mc); return; }
                negativeCases(mc);
                var previous = mc.player.getMainHandItem();
                int nextSlot = (mc.player.getInventory().getSelectedSlot() + 1) % 9;
                var next = stack(new Button("minecraft:carrot_on_a_stick", "15", "", nextSlot), 7);
                mc.player.getInventory().setItem(nextSlot, next);
                mc.player.getInventory().setSelectedSlot(nextSlot);
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, next);
                check(!ManagedHotbarRefresh.matches(previous, next, mc.player), "real slot switch was suppressed");
                stage = 3;
                ticks = 0;
                return;
            }
            if (stage == 3) {
                ticks++;
                vanillaSwitch |= height(mc) < .95F;
                punchySwitch |= punchy && busy();
                if (ticks < 32) return;
                check(vanillaSwitch && (!punchy || punchySwitch), "normal slot equip animation disappeared");
                verifyDisplay((ItemStack) field(FirstPersonHandsAndItems.class, "mainHandItem")
                        .get(mc.player.firstPersonHandsAndItems()), mc.player.getMainHandItem(), "post-switch");
                finish(mc, "PASS " + checks + " checks; " + buttons.size()
                        + " source-derived button cases x 3 continuous refreshes; names/lore/count/glint/custom_data current; "
                        + "slot/identity/model/unmanaged exclusions; " + (punchy ? "Punchy + vanilla" : "vanilla without Punchy"));
            }
        } catch (Throwable error) {
            error.printStackTrace();
            finish(mc, "FAIL " + index + "/" + seconds + ": " + error);
        }
    }

    private void negativeCases(Minecraft mc) {
        int slot = mc.player.getInventory().getSelectedSlot();
        var previous = stack(new Button("minecraft:carrot_on_a_stick", "15", "ggd:role_knife/sheriff", slot), 3);
        var next = previous.copy();
        var data = next.get(DataComponents.CUSTOM_DATA).copyTag();
        data.putBoolean("otherAbility", true);
        next.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        mc.player.setItemInHand(InteractionHand.MAIN_HAND, next);
        check(!ManagedHotbarRefresh.matches(previous, next, mc.player), "changed ability identity was ignored");
        next = previous.copy();
        next.set(DataComponents.ITEM_MODEL, Identifier.parse("ggd:role_knife/duck"));
        mc.player.setItemInHand(InteractionHand.MAIN_HAND, next);
        check(!ManagedHotbarRefresh.matches(previous, next, mc.player), "changed item model was ignored");
        next = previous.copy();
        next.remove(DataComponents.CUSTOM_DATA);
        mc.player.setItemInHand(InteractionHand.MAIN_HAND, next);
        check(!ManagedHotbarRefresh.matches(previous, next, mc.player), "unmanaged item was accepted");
        check(!ManagedHotbarRefresh.matches(previous, next.copy(), mc.player), "non-held stack was accepted");
        check(!ManagedHotbarRefresh.matches(ItemStack.EMPTY, next, mc.player), "empty previous item was accepted");
    }

    private void equip(Minecraft mc) {
        var button = buttons.get(index);
        seconds = 3;
        mc.player.getInventory().setSelectedSlot(button.slot);
        mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack(button, seconds));
        ticks = 0;
        stage = 1;
    }

    private static ItemStack stack(Button button, int seconds) {
        var stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(button.item)), Math.max(1, seconds));
        stack.remove(DataComponents.MAX_DAMAGE);
        stack.set(DataComponents.MAX_STACK_SIZE, 99);
        if (!button.definition.isEmpty()) stack.set(DataComponents.ITEM_MODEL, Identifier.parse(button.definition));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(button.model), List.of()));
        var data = new CompoundTag();
        data.putBoolean("ggdManagedHotbar", true);
        data.putInt("ggdHotbarSlot", button.slot);
        data.putString("ggdHotbarState", "fixture_" + seconds);
        data.putString("chapelSewerState", "cd_" + seconds);
        data.putString("neptuneItemState", "cd_" + seconds);
        data.putBoolean("RefreshFixture", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        stack.set(DataComponents.ITEM_NAME, Component.translatableWithFallback("test.skill.cooldown", "Cooldown %s", seconds));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.translatableWithFallback("test.skill.charges", "Charges %s", seconds))));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, seconds % 2 == 0);
        return stack;
    }

    private void verifyDisplay(ItemStack shown, ItemStack current, String renderer) {
        check(shown.getCount() == current.getCount(), renderer + " stale count");
        for (var component : List.of(DataComponents.ITEM_NAME, DataComponents.LORE, DataComponents.ENCHANTMENT_GLINT_OVERRIDE,
                DataComponents.CUSTOM_DATA, DataComponents.ITEM_MODEL, DataComponents.CUSTOM_MODEL_DATA)) {
            check(Objects.equals(shown.get(component), current.get(component)), renderer + " stale " + component);
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        var result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static float height(Minecraft mc) throws Exception {
        return field(FirstPersonHandsAndItems.class, "mainHandHeight").getFloat(mc.player.firstPersonHandsAndItems());
    }

    private boolean busy() throws Exception { return (boolean) machine.getClass().getMethod("isEquipInProgress").invoke(null); }

    private void finish(Minecraft mc, String result) {
        done = true;
        try { Files.writeString(root.resolve(role + "-result"), result); }
        catch (Exception error) { error.printStackTrace(); }
        if (result.startsWith("FAIL")) mc.stop();
    }
}
