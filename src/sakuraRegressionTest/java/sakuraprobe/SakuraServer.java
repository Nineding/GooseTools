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
    private record Dash(net.minecraft.world.phys.Vec3 start,net.minecraft.world.phys.Vec3 direction,int began) { }
    private final java.util.Map<java.util.UUID,Dash> dashes=new java.util.HashMap<>();
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
                    double yaw=Math.toRadians(player.getYRot());
                    dashes.put(player.getUUID(),new Dash(player.position(),new net.minecraft.world.phys.Vec3(-Math.sin(yaw),0,Math.cos(yaw)),server.getTickCount()));
                }
            }
            for(var player:players) {
                var dash=dashes.get(player.getUUID());if(dash==null)continue;
                int elapsed=server.getTickCount()-dash.began();
                double t=Math.clamp(elapsed/8.0,0,1),progress=t*t*(3-2*t);
                var point=dash.start().add(dash.direction().scale(6*progress));
                player.setPos(point);
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                        new com.goosethings.tools.swordman.SwordmanPackets.Motion(point.x,point.y,point.z,elapsed<=8));
                if(elapsed>8) {
                    player.connection.teleport(point.x,point.y,point.z,player.getYRot(),player.getXRot());
                    dashes.remove(player.getUUID());
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
