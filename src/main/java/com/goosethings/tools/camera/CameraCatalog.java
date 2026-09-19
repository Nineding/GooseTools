package com.goosethings.tools.camera;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashSet;
import java.util.List;

public record CameraCatalog(int format, List<CameraDefinition> cameras, List<ScreenDefinition> screens) {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public static CameraCatalog empty() { return new CameraCatalog(1, List.of(), List.of()); }
    public CameraCatalog {
        if (format != 1 || cameras == null || screens == null
                || cameras.size() > CameraLimits.MAX_CAMERAS || screens.size() > CameraLimits.MAX_SCREENS)
            throw new IllegalArgumentException("Invalid camera catalog");
        cameras = List.copyOf(cameras);
        screens = List.copyOf(screens);
        var ids = new HashSet<String>();
        for (var c : cameras) if (!ids.add(c.id())) throw new IllegalArgumentException("Duplicate camera ID");
        var screenIds = new HashSet<String>();
        for (var s : screens) {
            if (!ids.contains(s.cameraId())) throw new IllegalArgumentException("Unknown camera: " + s.cameraId());
            if (!screenIds.add(s.id())) throw new IllegalArgumentException("Duplicate screen ID");
        }
    }
    public CameraDefinition camera(String id) {
        return cameras.stream().filter(c -> c.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown camera: " + id));
    }
    public ScreenDefinition screen(String id) {
        return screens.stream().filter(s -> s.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown screen: " + id));
    }
}
