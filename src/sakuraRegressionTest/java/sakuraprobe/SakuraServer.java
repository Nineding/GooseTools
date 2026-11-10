package sakuraprobe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SakuraServer implements ModInitializer {
    private final Path root = Path.of(System.getProperty("sakura.probe"));
    private final java.util.Set<String> sent=new java.util.HashSet<>();
    private boolean equipped;
    private boolean finished;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> write("server-ready", "dedicated server booted"));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (finished) return;
            var players = server.getPlayerList().getPlayers();
            if (!equipped && players.size() == 2) {
                equipped = true;
                for (var player : players) {
                    player.getInventory().setItem(0, SakuraStacks.sword(0, 6, false));
                    player.getInventory().setItem(1, new ItemStack(Items.IRON_SWORD));
                    player.getInventory().setItem(2, SakuraStacks.sword(2, 6, false));
                }
            }
            for(var player:players) for(int round=0;round<2;round++) {
                String key=player.getGameProfile().name().substring(6)+"-dash-request-"+round;
                if(Files.exists(root.resolve(key)) && sent.add(key)) {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                        new com.goosethings.tools.swordman.SwordmanPackets.Motion(player.getX(),player.getY(),player.getZ(),true));
                }
            }
            if (Files.exists(root.resolve("A-result")) && Files.exists(root.resolve("B-result"))) {
                finished = true;
                write("server-result", equipped ? "PASS two clients connected and received real sword stacks" : "FAIL players did not join");
                server.halt(false);
            }
        });
    }

    private void write(String file, String message) {
        try {
            Files.writeString(root.resolve(file), message);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
