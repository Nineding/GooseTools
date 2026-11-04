package com.goosethings.tools.client.fairplay;

import java.util.List;
import java.util.Map;

/** Exact Fabric IDs, independent of file names and user-controlled feature toggles. */
public final class BlockedModPolicy {
    private static final List<String> BLOCKED_IDS = List.of(
            "tweakeroo", "freecam", "meteor-client", "wurst");

    private BlockedModPolicy() {}

    public static List<BlockedMod> detect(Map<String, String> loadedMods) {
        return BLOCKED_IDS.stream()
                .filter(loadedMods::containsKey)
                .map(id -> new BlockedMod(id, loadedMods.get(id)))
                .toList();
    }

    public record BlockedMod(String id, String name) {}
}
