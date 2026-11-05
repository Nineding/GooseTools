package com.goosethings.tools.client.meetingvoice;

/** Only uses dimensions of attachments already present on the visible nameplate. */
final class BubbleAnchor {
    static double top(Object entity, boolean goose) throws ReflectiveOperationException {
        double top = 6;
        if (goose) {
            Object entry = Reflect.call(Reflect.type("com.goosethings.tools.client.nametag.NameTagClientState"), "entry", entity);
            if (entry != null) for (Object icon : (Iterable<?>)Reflect.call(entry, "attachments")) {
                double height = ((Number)Reflect.call(icon, "height")).doubleValue();
                if (Double.isFinite(height)) top = Math.max(top, height / 2);
            }
        }
        return top;
    }
}
