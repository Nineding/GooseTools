package sakuraprobe;

import com.goosethings.tools.client.punchy.mixin.ItemRenderLayerAccess;
import com.goosethings.tools.client.punchy.mixin.ItemRenderStateAccess;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class SakuraClient implements ClientModInitializer {
    private final Path root = Path.of(System.getProperty("sakura.probe"));
    private final String role = System.getProperty("sakura.role");
    private final String address = "127.0.0.1:" + System.getProperty("sakura.port");
    private boolean connecting;
    private boolean done;
    private boolean punchy;
    private int stage;
    private int ticks;
    private long deadline = System.nanoTime() + 220_000_000_000L;
    private Object punchyMachine;
    private Object movesController;
    private Method movesIsSword;
    private Method movesGroup;
    private int weapon;
    private int combo;
    private int lastTrigger;
    private final String[] ironClips = new String[2];
    private final int[] slots = {1, 2, 0};

    @Override
    public void onInitializeClient() {
        punchy = FabricLoader.getInstance().isModLoaded("punchy");
        ClientTickEvents.START_CLIENT_TICK.register(mc->{if(stage==11||stage==13)mc.options.keyUse.setDown(true);});
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if(done){if(Files.exists(root.resolve(("A".equals(role)?"B":"A")+"-result")))mc.stop();return;}
        if (!mc.isGameLoadFinished()) return;
        try {
            require(System.nanoTime() < deadline, "timeout stage " + stage + ", screen " + mc.gui.screen());
            require(punchy == "B".equals(role), "wrong Punchy fixture");
            require(FabricLoader.getInstance().isModLoaded("punchy_moves") == punchy, "wrong Moves fixture");
            if (!connecting) {
                if (!Files.exists(root.resolve("server-ready"))) return;
                connecting = true;
                mc.options.pauseOnLostFocus = false;
                ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address),
                        new ServerData("Sakura validation", address, ServerData.Type.OTHER), false, null);
                return;
            }
            if(mc.gui.screen()!=null && mc.gui.screen().getClass().getName().contains("punchymoves.client.tutorial")) {mc.gui.screen().onClose();if(stage>=10){stage=10;ticks=0;}return;}
            var player = mc.player;
            if (player == null || mc.level == null || !player.connection.hasClientLoaded()) return;
            if (stage >= 10) { iaido(mc); return; }
            if (stage == 0) {
                var remote = mc.level.players().stream().filter(p -> p != player).findFirst().orElse(null);
                if (remote == null || player.getMainHandItem().getCount() != 6
                        || !SakuraStacks.model(remote.getMainHandItem()).equals("sakura_sword")) return;
                for (var camera : new CameraType[]{CameraType.THIRD_PERSON_BACK, CameraType.THIRD_PERSON_FRONT}) {
                    mc.options.setCameraType(camera);
                    require(visible(mc, player), "own sword hidden in " + camera);
                    require(!visible(mc, remote), "remote sword visible in " + camera);
                }
                mc.setCameraEntity(remote);
                require(!visible(mc, remote), "camera target inherited owner's sword");
                require(!visible(mc, player), "owner sword leaked into detached camera");
                mc.setCameraEntity(player);
                var profile = player.connection.getPlayerInfo(player.getUUID());
                var gameMode = field(PlayerInfo.class, "gameMode");
                var mode = profile.getGameMode();
                gameMode.set(profile, GameType.SPECTATOR);
                require(player.isSpectator() && !visible(mc, player), "spectator saw the private sword");
                gameMode.set(profile, mode);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                require(!visible(mc, player), "third-person layer visible in first person");
                Files.writeString(root.resolve(role + "-view"), "PASS front/back self, remote and followed entity");
                stage = 1;
                return;
            }
            if (stage == 1 && Boolean.getBoolean("sakura.quick")) {stage=10;ticks=0;return;}
            if (stage == 1) {
                if (!Files.exists(root.resolve("A-view")) || !Files.exists(root.resolve("B-view"))) return;
                if (height(player.firstPersonHandsAndItems()) < 0.95F) return;
                if (punchy) {
                    punchyMachine = field(Class.forName("punchy.client.state.HandEquipStateMachine"), "instance").get(null);
                    if (punchyMachine == null || punchyBusy() || visiblePunchyItem().getCount() != 6) return;
                }
                player.getInventory().setItem(0, SakuraStacks.sword(0, 5, false));
                stage = 2;
                return;
            }
            if (stage == 2 || stage == 3) {
                int count = stage == 2 ? 5 : 4;
                String model = stage == 2 ? "sakura_sword" : "sakura_sword_charging";
                var hands = player.firstPersonHandsAndItems();
                require(height(hands) >= 0.95F, "vanilla lowered hand during " + model);
                var shown = (ItemStack) field(FirstPersonHandsAndItems.class, "mainHandItem").get(hands);
                require(shown.getCount() == count && model.equals(SakuraStacks.model(shown)), "vanilla kept stale stack");
                if (punchy) {
                    require(!punchyBusy(), "Punchy started equip for " + model);
                    shown = visiblePunchyItem();
                    require(shown.getCount() == count && model.equals(SakuraStacks.model(shown)), "Punchy kept stale stack");
                }
                if (stage++ == 2) {
                    player.getInventory().setItem(0, SakuraStacks.sword(0, 4, true));
                    return;
                }
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                require(visible(mc, player), "charged sword hidden to owner");
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                player.getInventory().setSelectedSlot(2);
                return;
            }
            if (stage == 4) {
                if (++ticks == 1)
                    require(height(player.firstPersonHandsAndItems()) < 0.95F, "vanilla skipped real slot change");
                if (!punchy || punchyBusy()) {
                    stage = 5;
                    ticks = 0;
                    return;
                }
                require(ticks < 15, "Punchy skipped real slot change");
                return;
            }
            if (!punchy) {
                stage = 10; ticks = 0;
                return;
            }
            if (stage == 5) {
                var remote = mc.level.players().stream().filter(p -> p != player).findFirst().orElse(null);
                if (remote == null || !SakuraStacks.model(remote.getMainHandItem()).equals("sakura_sword")) return;
                var engine = Class.forName("punchymoves.client.PlayerAnimationEngine");
                Object state = ((Map<?, ?>) field(engine, "PLAYERS").get(null)).get(player);
                if (state == null) return;
                movesController = field(state.getClass(), "controller").get(state);
                if (movesController == null) return;
                movesIsSword = movesController.getClass().getDeclaredMethod("isSword", ItemStack.class);
                movesIsSword.setAccessible(true);
                movesGroup = Class.forName("punchymoves.client.animation.ItemAnimationBindings")
                        .getDeclaredMethod("groupFor", ItemStack.class);
                movesGroup.setAccessible(true);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                require(isMovesSword(player.getMainHandItem()), "held Sakura did not enter the native sword group");
                require(!isMovesSword(remote.getMainHandItem()), "remote Sakura entered the private sword group");
                require(!isMovesSword(new ItemStack(Items.CARROT_ON_A_STICK)), "generic skill item classified as sword");
                mc.setCameraEntity(remote);
                require(!isMovesSword(player.getMainHandItem()), "detached camera inherited sword classification");
                mc.setCameraEntity(player);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                require(!isMovesSword(player.getMainHandItem()), "Moves classification leaked to first person");
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                player.setXRot(-67);
                player.getInventory().setSelectedSlot(slots[weapon]);
                stage = 6;
                return;
            }
            if (stage == 6) {
                if (++ticks < 32) return;
                if (weapon == 2) {
                    player.getInventory().setItem(0, SakuraStacks.sword(0, 4, true));
                }
                require(isMovesSword(player.getMainHandItem()), "weapon did not resolve as a native sword");
                click(mc);
                return;
            }
            if (stage == 7) {
                if (++ticks == 2) mc.options.keyAttack.setDown(false);
                if (comboTick() == lastTrigger) {
                    require(ticks < 18, "native sword combo never started for weapon " + weapon + " step " + combo);
                    return;
                }
                String clip = swordClip();
                require(clip.equals("sword_attack_" + (combo + 1)), "wrong native clip " + clip);
                if (weapon == 0) ironClips[combo] = clip;
                else require(clip.equals(ironClips[combo]), "Sakura clip differs from iron sword");
                if (combo == 0) {
                    combo = 1;
                    lastTrigger = comboTick();
                    stage = 8;
                    return;
                }
                mc.options.keyAttack.setDown(false);
                combo = 0;
                if (++weapon == slots.length) {
                    stage = 10; ticks = 0;
                    return;
                }
                player.getInventory().setSelectedSlot(slots[weapon]);
                stage = 6;
                ticks = 0;
                return;
            }
            if (stage == 8) {
                mc.options.keyAttack.setDown(false);
                if (player.tickCount - lastTrigger >= 13) click(mc);
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish(mc, "FAIL " + failure);
        }
    }


    private int round;
    private int captures;
    private void capture(Minecraft mc,String name) {
        net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){
            image.writeToFile(root.resolve(role+"-"+name+".png"));captures++;
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private void iaido(Minecraft mc) throws Exception {
        var p=mc.player; ticks++;
        if(stage==10) {
            if(ticks==1) {
                p.getInventory().setSelectedSlot(0);p.getInventory().setItem(0,SakuraStacks.sword(0,4,false));
                p.setXRot(0);p.setYRot(0);mc.options.keyUse.setDown(false);
                mc.options.setCameraType(round==0?CameraType.FIRST_PERSON:CameraType.THIRD_PERSON_FRONT);
            }
            if(ticks<30)return;
            require(com.goosethings.tools.client.swordman.SakuraIaidoRenderer.ready(),"iaido assets failed to load");
            capture(mc,"idle-"+round);
            p.getInventory().setItem(0,SakuraStacks.sword(0,3,true));mc.options.keyUse.setDown(true);
            stage=11;ticks=0;return;
        }
        if(stage==11) {
            require(ticks<3||com.goosethings.tools.client.swordman.SakuraIaidoClient.active(),"charge did not start: eligible="+com.goosethings.tools.client.swordman.SakuraIaidoClient.eligible()+" held="+mc.options.keyUse.isDown()+" model="+SakuraStacks.model(p.getMainHandItem())+" screen="+mc.gui.screen());
            if(ticks==8)capture(mc,"insert-"+round);
            if(ticks==25) {
                capture(mc,"hold-"+round);
                var f=com.goosethings.tools.client.swordman.SakuraIaidoClient.frame(0);
                require(f.blade().position().equals(f.sheath().position()),"blade not seated in saya");
                var remote=mc.level.players().stream().filter(x->x!=p).findFirst().orElseThrow();
                @SuppressWarnings("rawtypes") var renderer=(net.minecraft.client.renderer.entity.EntityRenderer)mc.getEntityRenderDispatcher().getRenderer(remote);
                var rs=(net.minecraft.client.renderer.entity.state.AvatarRenderState)renderer.createRenderState(remote,1);
                require(!com.goosethings.tools.client.swordman.SakuraIaidoClient.owns(rs),"remote inherited iaido");
                var ownRenderer=(net.minecraft.client.renderer.entity.EntityRenderer)mc.getEntityRenderDispatcher().getRenderer(p);
                var own=(net.minecraft.client.renderer.entity.state.AvatarRenderState)ownRenderer.createRenderState(p,1);
                require(((com.goosethings.tools.client.swordman.SakuraVisualState)own).goosetools$iaido()==null,"corpse snapshot leaked visual pose");
                p.getInventory().setItem(0,SakuraStacks.sword(0,2,true));
            }
            if(ticks<30)return;
            mc.options.keyUse.setDown(false);
            Files.writeString(root.resolve(role+"-dash-request-"+round),"dash");
            stage=12;ticks=0;return;
        }
        if(stage==12) {
            float time=com.goosethings.tools.client.swordman.SakuraIaidoClient.frame(0).slash();
            if(time>=0 && time<.12)capture(mc,"draw-"+round+"-"+ticks);
            if(time>=.15 && time<.4)capture(mc,"cut-"+round+"-"+ticks);
            if(ticks<38)return;
            require(!com.goosethings.tools.client.swordman.SakuraIaidoClient.active(),"slash failed to recover");
            p.getInventory().setItem(0,SakuraStacks.sword(0,2,false));
            if(++round<2){stage=10;ticks=0;return;}

            p.getInventory().setItem(0,SakuraStacks.sword(0,2,true));mc.options.keyUse.setDown(true);
            stage=13;ticks=0;return;
        }
        if(stage==13) {
            if(ticks<3)return;
            require(com.goosethings.tools.client.swordman.SakuraIaidoClient.active(),"second charge did not start");
            p.getInventory().setSelectedSlot(1);mc.options.keyUse.setDown(false);stage=14;ticks=0;return;
        }
        if(stage==14) {
            if(ticks<3||captures<8)return;
            require(!com.goosethings.tools.client.swordman.SakuraIaidoClient.active(),"slot switch did not clear iaido");
            finish(mc,"PASS native iron/Sakura swings and equip guards; server-approved iaido in both views; local-only pose and snapshot isolation; slot cancellation; "+captures+" GPU screenshots");
        }
    }

    private boolean isMovesSword(ItemStack stack) throws ReflectiveOperationException {
        boolean recognized = (boolean) movesIsSword.invoke(null, stack);
        require(recognized == "sword".equals(movesGroup.invoke(null, stack)), "native sword predicates disagree");
        return recognized;
    }

    private void click(Minecraft mc) throws ReflectiveOperationException {
        lastTrigger = comboTick();
        mc.options.keyAttack.setDown(true);
        var engine = Class.forName("punchymoves.client.PlayerAnimationEngine");
        var trigger = field(engine, "primaryActionTrigger");
        int before = trigger.getInt(null);
        var hit = mc.hitResult;
        mc.hitResult = BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO);
        try {
            var attack = Minecraft.class.getDeclaredMethod("startAttack");
            attack.setAccessible(true);
            field(Minecraft.class, "missTime").setInt(mc, 0);
            attack.invoke(mc);
        } finally {
            mc.hitResult = hit;
        }
        require(trigger.getInt(null) == before + 1, "real Minecraft attack did not reach Moves");
        stage = 7;
        ticks = 0;
    }

    private int comboTick() throws ReflectiveOperationException {
        var state = field(movesController.getClass(), "attackCombo").get(movesController);
        return field(state.getClass(), "lastTriggerTick").getInt(state);
    }

    private String swordClip() throws ReflectiveOperationException {
        var layer = field(movesController.getClass(), "swordAttack").get(movesController);
        var clip = field(layer.getClass(), "clip").get(layer);
        require(clip != null, "native sword layer has no clip");
        return (String) clip.getClass().getMethod("name").invoke(clip);
    }

    private static boolean visible(Minecraft mc, LivingEntity holder) {
        var armed = new ArmedEntityRenderState();
        ArmedEntityRenderState.extractArmedEntityRenderState(holder, armed, mc.getItemModelResolver(), 1);
        var state = armed.getMainHandItemState();
        ItemRenderStateAccess access = (ItemRenderStateAccess) state;
        require(access.goosetools$getActiveLayerCount() > 0, "skill sword model has no layers");
        for (int i = 0; i < access.goosetools$getActiveLayerCount(); i++) {
            var scale = ((ItemRenderLayerAccess) access.goosetools$getLayers()[i]).goosetools$getItemTransform().scale();
            if (scale.x() != 0 && scale.y() != 0) return true;
        }
        return false;
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static float height(FirstPersonHandsAndItems hands) throws ReflectiveOperationException {
        return field(FirstPersonHandsAndItems.class, "mainHandHeight").getFloat(hands);
    }

    private boolean punchyBusy() throws ReflectiveOperationException {
        return (boolean) punchyMachine.getClass().getMethod("isEquipInProgress").invoke(null);
    }

    private ItemStack visiblePunchyItem() throws ReflectiveOperationException {
        return (ItemStack) field(punchyMachine.getClass(), "renderedMain").get(punchyMachine);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void finish(Minecraft mc, String result) {
        done = true;
        try {
            Files.writeString(root.resolve(role + "-result"), result);
        } catch (Exception failure) {
            failure.printStackTrace();
        }
    }
}
