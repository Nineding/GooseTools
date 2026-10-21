package com.goosethings.tools.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MimeControlPayloadTest {
    @Test void followModeAndControllerRoundTripForStartAndRestore() {
        UUID controller = UUID.randomUUID();
        for (var state : java.util.List.of(
                new GooseToolsPayloads.MimeControlS2C(true, controller, true),
                new GooseToolsPayloads.MimeControlS2C(true, controller, false),
                new GooseToolsPayloads.MimeControlS2C(false, controller, false))) {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                GooseToolsPayloads.MimeControlS2C.CODEC.encode(buffer, state);
                assertEquals(state, GooseToolsPayloads.MimeControlS2C.CODEC.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {buffer.release();}
        }
    }
}
