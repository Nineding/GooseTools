package com.goosethings.tools.client.dream;

import com.goosethings.tools.dream.DreamAvatarPackets;
import com.goosethings.tools.dream.DreamFlightMotion;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;

/** Replays only unacknowledged controls over server motion; scene interpolation is for observers. */
final class DreamFlightPrediction {
    private final float scale;
    private final ArrayDeque<DreamAvatarPackets.Input> pending = new ArrayDeque<>();
    private DreamFlightMotion.State predicted;
    private Vec3 correction = Vec3.ZERO;
    private long tick = -1, epoch = -1;
    private boolean flying;

    DreamFlightPrediction() { this(1.0F); }
    DreamFlightPrediction(float scale) { this.scale = scale; }

    boolean active() { return predicted != null && flying; }

    void accept(DreamAvatarPackets.Flight state, DreamRemotePlayer body) {
        if (state.tick() <= tick) return;
        tick = state.tick();
        while (!pending.isEmpty() && pending.peekFirst().sequence() <= state.sequence()) pending.removeFirst();
        var next = new DreamFlightMotion.State(state.position(), state.velocity());
        if (state.flying()) for (var input : pending) next = DreamFlightMotion.step(next, input, scale);
        boolean snap = predicted == null || epoch != state.epoch() || flying != state.flying()
                || body.position().distanceToSqr(next.position()) > 16;
        correction = snap ? Vec3.ZERO : body.position().subtract(next.position());
        predicted = next;
        epoch = state.epoch();
        flying = state.flying();
        if (active()) {
            body.getInterpolation().cancel();
            body.setPos(predicted.position().add(correction));
            if (snap) body.setOldPosAndRot();
        }
    }

    void tick(DreamAvatarPackets.Input input, DreamRemotePlayer body) {
        // Bound both memory and speculative travel if the connection stalls for seconds.
        if (pending.size() >= 64) return;
        pending.addLast(input);
        if (!active()) return;
        predicted = DreamFlightMotion.step(predicted, input, scale);
        correction = correction.scale(0.65);
        body.setPos(predicted.position().add(correction));
    }
}
