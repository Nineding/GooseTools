package com.goosethings.tools.client.dream;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.server.MinecraftServer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Dedicated server and real TCP connection, sharing only test assertions with the client JVM. */
public final class DreamTcpFixture {
    private static final boolean ENABLED=Boolean.getBoolean("goosetools.testTcp");
    private static volatile MinecraftServer dedicated;
    private static boolean connecting, verified;
    private static long started;
    private DreamTcpFixture() {}

    public static boolean open(Minecraft client) throws Exception {
        if(!ENABLED)return false;
        started=System.nanoTime();
        Path directory=client.gameDirectory.toPath().toAbsolutePath().normalize();
        if(!directory.equals(Path.of("").toAbsolutePath().normalize()))throw new IllegalStateException("Unexpected test working directory");
        Files.writeString(directory.resolve("eula.txt"),"eula=true\n");
        Files.writeString(directory.resolve("server.properties"),"online-mode=false\nserver-ip=127.0.0.1\nserver-port=25598\n"
                +"white-list=false\nallow-flight=true\nlevel-type=minecraft:flat\ngenerate-structures=false\nspawn-protection=0\nview-distance=4\nsimulation-distance=5\nmax-tick-time=-1\n");
        ServerLifecycleEvents.SERVER_STARTED.register(server->{if(server.isDedicatedServer())dedicated=server;});
        Thread launcher=new Thread(()->net.minecraft.server.Main.main(new String[]{"--nogui","--universe",directory.resolve("tcp-worlds").toString(),
                "--world","projection-"+System.currentTimeMillis(),"--port","25598"}),"Projection TCP server launcher");
        launcher.setDaemon(true);launcher.start();return true;
    }

    public static boolean awaitConnection(Minecraft client) {
        if(!ENABLED)return false;
        if(client.level==null && System.nanoTime()-started>120_000_000_000L)
            throw new IllegalStateException("Dedicated TCP fixture did not connect within two minutes");
        if(dedicated==null)return true;
        if(!connecting) {
            connecting=true;
            ConnectScreen.startConnecting(new TitleScreen(),client,new ServerAddress("127.0.0.1",25598),
                    new ServerData("Projection TCP test","127.0.0.1:25598",ServerData.Type.OTHER),false,null);
        }
        if(client.player!=null && !verified) {
            if(!dedicated.isDedicatedServer() || client.getSingleplayerServer()!=null
                    || client.player.connection.getConnection().isMemoryConnection())
                throw new AssertionError("Fixture must use a dedicated server and real TCP, not an integrated/memory connection");
            verified=true;
            GooseTools.LOGGER.info("Projection regression connected to native dedicated server through real TCP");
        }
        return client.level==null;
    }

    public static MinecraftServer server(Minecraft client) {return ENABLED?dedicated:client.getSingleplayerServer();}
    public static void stop() {if(dedicated!=null)dedicated.halt(true);}
}
