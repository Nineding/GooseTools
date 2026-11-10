package sakuraprobe;

import net.fabricmc.api.DedicatedServerModInitializer;

public final class SakuraDedicated implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        new SakuraServer().onInitialize();
    }
}
