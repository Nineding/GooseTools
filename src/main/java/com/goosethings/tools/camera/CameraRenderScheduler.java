package com.goosethings.tools.camera;

import java.util.List;
import java.util.Optional;

/** Fairly staggers expensive off-screen feed updates across main-world frames. */
public final class CameraRenderScheduler {
    private int cursor;

    public Optional<String> next(List<String> readyFeeds) {
        if (readyFeeds.isEmpty()) {
            cursor = 0;
            return Optional.empty();
        }
        int index = Math.floorMod(cursor, readyFeeds.size());
        cursor = (index + 1) % readyFeeds.size();
        return Optional.of(readyFeeds.get(index));
    }

    public void reset() {
        cursor = 0;
    }
}
