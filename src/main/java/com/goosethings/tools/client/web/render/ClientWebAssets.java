package com.goosethings.tools.client.web.render;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.web.dom.WebDocument;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Resolves resource-pack textures and creates bounded dynamic textures for server-sent images. */
public final class ClientWebAssets {
    private final Map<String, TextureRef> textures = new LinkedHashMap<>();

    public TextureRef resolve(WebDocument document, String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        if (source.startsWith("mc:")) {
            Identifier identifier = Identifier.tryParse(source.substring("mc:".length()));
            return identifier == null ? null : new TextureRef(identifier, false);
        }

        String path = document.resolveResource(source);
        if (path.isBlank()) {
            return null;
        }
        String cacheKey = document.bundle().hash() + ':' + path;
        TextureRef existing = textures.get(cacheKey);
        if (existing != null) {
            return existing;
        }
        byte[] bytes = document.bundle().files().get(path);
        if (bytes == null || !(path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg"))) {
            return null;
        }
        try {
            NativeImage image = NativeImage.read(bytes);
            Identifier identifier = Identifier.fromNamespaceAndPath(
                    GooseTools.MOD_ID,
                    "web/" + Integer.toUnsignedString(cacheKey.hashCode(), 16));
            DynamicTexture texture = new DynamicTexture(() -> "GooseTools WebUI " + path, image);
            Minecraft.getInstance().getTextureManager().register(identifier, texture);
            TextureRef created = new TextureRef(identifier, true);
            textures.put(cacheKey, created);
            return created;
        } catch (IOException | RuntimeException exception) {
            GooseTools.LOGGER.warn("Unable to decode GooseTools web image {}: {}", path, exception.getMessage());
            return null;
        }
    }

    public void clear() {
        for (TextureRef texture : textures.values()) {
            if (texture.dynamic()) {
                Minecraft.getInstance().getTextureManager().release(texture.identifier());
            }
        }
        textures.clear();
    }

    public record TextureRef(Identifier identifier, boolean dynamic) {
    }
}
