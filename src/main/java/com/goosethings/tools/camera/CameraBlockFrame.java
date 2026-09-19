package com.goosethings.tools.camera;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/** Fixed-size, bounded remote block cache. No remote data enters the player's chunk cache. */
public record CameraBlockFrame(int x, int y, int z, int[] states, int[] lights, int[] biomes) {
    public CameraBlockFrame {
        if (states.length != CameraLimits.CELLS || lights.length != CameraLimits.CELLS
                || biomes.length != CameraLimits.CELLS) throw new IllegalArgumentException("Invalid scene size");
        CameraLimits.position(x, y, z);
    }
    public static int index(int x, int y, int z) {
        return (y * CameraLimits.SIZE_Z + z) * CameraLimits.SIZE_X + x;
    }
    public byte[] compress() throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(new DeflaterOutputStream(bytes))) {
            out.writeInt(x); out.writeInt(y); out.writeInt(z);
            for (int value : states) out.writeInt(value);
            for (int value : lights) out.writeInt(value);
            for (int value : biomes) out.writeInt(value);
        }
        if (bytes.size() > CameraLimits.MAX_COMPRESSED) throw new IOException("Scene exceeds transmission limit");
        return bytes.toByteArray();
    }
    public static CameraBlockFrame decompress(byte[] data) throws IOException {
        if (data.length > CameraLimits.MAX_COMPRESSED) throw new IOException("Scene exceeds transmission limit");
        try (var in = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))) {
            int x = in.readInt(), y = in.readInt(), z = in.readInt();
            int[] states = new int[CameraLimits.CELLS], lights = new int[states.length], tints = new int[states.length];
            for (int i = 0; i < states.length; i++) {
                states[i] = in.readInt();
                if (states[i] < 0) throw new IOException("Invalid block state");
            }
            for (int i = 0; i < lights.length; i++) lights[i] = in.readInt();
            for (int i = 0; i < tints.length; i++) tints[i] = in.readInt();
            if (in.read() != -1) throw new IOException("Trailing scene data");
            return new CameraBlockFrame(x, y, z, states, lights, tints);
        }
    }
}
