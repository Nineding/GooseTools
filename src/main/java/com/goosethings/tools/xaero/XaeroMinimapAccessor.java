package com.goosethings.tools.xaero;

import xaero.common.minimap.render.MinimapRenderer;
import xaero.minimap.XaeroMinimap;

final class XaeroMinimapAccessor {
    private final MinimapRenderer renderer;

    private XaeroMinimapAccessor(MinimapRenderer renderer) {
        this.renderer = renderer;
    }

    static XaeroMinimapAccessor get() {
        if (XaeroMinimap.instance == null || XaeroMinimap.instance.getMinimap() == null) {
            return null;
        }
        var minimap = XaeroMinimap.instance.getMinimap();
        MinimapRenderer renderer = minimap.usingFBO()
                ? minimap.getMinimapFBORenderer()
                : minimap.getMinimapSafeModeRenderer();
        return new XaeroMinimapAccessor(renderer);
    }

    double zoom() {
        return renderer.getZoom();
    }
}
