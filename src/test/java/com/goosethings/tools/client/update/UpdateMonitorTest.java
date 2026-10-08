package com.goosethings.tools.client.update;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CancellationException;
import static org.junit.jupiter.api.Assertions.*;

class UpdateMonitorTest {
    @Test void reportsMeasuredProgressAndUnknownSize() {
        var monitor = new UpdateMonitor(); assertEquals(-1, monitor.snapshot().percent());
        monitor.update(UpdateMonitor.Phase.DOWNLOADING, "1.14.0+Alpha0.25", 372, 1000);
        assertEquals(37, monitor.snapshot().percent());
        monitor.update(UpdateMonitor.Phase.VERIFYING, "1.14.0+Alpha0.25", 1000, 1000);
        assertEquals(100, monitor.snapshot().percent());
    }
    @Test void cancellationCannotBeOverwrittenByTransferCallbacks() {
        var monitor = new UpdateMonitor(); monitor.cancel();
        assertThrows(CancellationException.class, () -> monitor.update(UpdateMonitor.Phase.CURRENT, "", 0, 0));
        assertEquals(UpdateMonitor.Phase.CANCELLED, monitor.snapshot().phase());
    }
    @Test void fileReplacementCannotBeCancelledHalfwayThrough() {
        var monitor = new UpdateMonitor(); monitor.update(UpdateMonitor.Phase.INSTALLING, "", 1, 1); monitor.cancel();
        assertEquals(UpdateMonitor.Phase.INSTALLING, monitor.snapshot().phase()); monitor.checkCancelled();
    }
}
