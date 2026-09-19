package com.goosethings.tools.camera;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CameraDataTest {
    @Test void eachFeedMaintainsAnIndependentSixtyFrameCadence() {
        var pacer = new CameraFramePacer();
        long start = 1_000_000L;
        int renders = pacer.shouldRender(start) ? 1 : 0;
        for (int frame = 1; frame <= 144; frame++) {
            long now = start + Math.round(frame * (1_000_000_000.0D / 144.0D));
            if (pacer.shouldRender(now)) renders++;
        }
        assertTrue(renders >= 60 && renders <= 61, "one second should produce 60 FPS plus the initial frame");
    }

    @Test void cameraQualityContractUsesFullHdAndExtendedForwardScene() {
        assertEquals(1920,CameraLimits.RENDER_WIDTH); assertEquals(1080,CameraLimits.RENDER_HEIGHT);
        assertEquals(70.0F,CameraLimits.VERTICAL_FOV_DEGREES);
        assertEquals(128,CameraLimits.SIZE_X); assertEquals(128,CameraLimits.SIZE_Z);
        assertEquals(32,CameraLimits.FORWARD_OFFSET); assertTrue(CameraLimits.FAR_PLANE>=128);
    }
    @Test void inclusiveBlocksCoverAllFourFacesAndReversedNegativeSelections() {
        var south = ScreenDefinition.corners("wall","hall","minecraft:the_end",-2405,72,91,-2401,74,91,"south");
        assertEquals(5,south.width()); assertEquals(3,south.height());
        assertEquals(-2402.5,south.x()); assertEquals(73.5,south.y()); assertEquals(92.003,south.z(),.000001);
        assertEquals(south,ScreenDefinition.corners("wall","hall","minecraft:the_end",-2401,74,91,-2405,72,91,"south"));
        var east=ScreenDefinition.corners("wall","hall","minecraft:the_end",-2407,72,97,-2407,74,93,"east");
        assertEquals(5,east.width()); assertEquals(3,east.height()); assertEquals(-2405.997,east.x(),.000001);
        assertEquals(95.5,east.z());
        var west=ScreenDefinition.corners("wall","hall","minecraft:the_end",-2407,72,97,-2407,74,93,"west");
        assertEquals(-2407.003,west.x(),.000001);
        var north=ScreenDefinition.corners("wall","hall","minecraft:the_end",-2405,72,91,-2401,74,91,"north");
        assertEquals(90.997,north.z(),.000001);
        var single=ScreenDefinition.corners("wall","hall","minecraft:overworld",-.2,70.8,-.1,-.9,70.1,-.9,"north");
        assertEquals(1,single.width()); assertEquals(1,single.height()); assertEquals(-.5,single.x());
        assertThrows(IllegalArgumentException.class,() -> ScreenDefinition.corners("wall","hall","minecraft:overworld",0,70,0,6,74,1,"north"));
    }
    @Test void movementHasContinuityButDoesNotAnimateDuplicatePacketsOrTeleports() {
        var motion = new CameraMotion(); motion.update(100,0,0); motion.update(102,.4,0);
        assertTrue(motion.speed()>.1); float phase = motion.position();
        motion.update(102,.4,0); assertEquals(phase,motion.position());
        motion.update(104,.8,0); assertTrue(motion.position()>phase);
        for(int tick=106;tick<130;tick+=2) motion.update(tick,.8,0);
        assertTrue(motion.speed()<.001);
        motion.update(130,512,0); assertEquals(0,motion.speed());
    }
    private final CameraDefinition camera = new CameraDefinition("hall","minecraft:overworld",-10.5,81.62,5,179,-45);
    private final ScreenDefinition screen = new ScreenDefinition("wall","hall","minecraft:overworld",0,80,0,"north",6,3.5f);
    @Test void persistedCatalogPreservesFixedEyeAndFractionalScreenSize() {
        var original = new CameraCatalog(1,List.of(camera),List.of(screen));
        var read = CameraCatalog.JSON.fromJson(CameraCatalog.JSON.toJson(original),CameraCatalog.class);
        assertEquals(original,read);
        assertEquals(camera,read.camera("hall"));
    }
    @Test void rejectsDanglingBindingsDuplicateIdsAndNonFiniteGeometry() {
        assertThrows(IllegalArgumentException.class,() -> new CameraCatalog(1,List.of(),List.of(screen)));
        assertThrows(IllegalArgumentException.class,() -> new CameraCatalog(1,List.of(camera,camera),List.of()));
        assertThrows(IllegalArgumentException.class,() -> screen.resize(Float.NaN,3));
        assertThrows(IllegalArgumentException.class,() -> screen.resize(0,3));
        assertThrows(IllegalArgumentException.class,() -> new CameraDefinition("../bad","minecraft:overworld",0,0,0,0,0));
    }
    @Test void largeScreenCanBeWatchedNearItsEdgeButNeverFromFarAway() {
        var large = screen.resize(128,100);
        assertTrue(large.withinReach(60,80,-20));
        assertFalse(large.withinReach(0,80,-49));
        assertFalse(large.withinReach(200,80,0));
    }
    @Test void compressedRemoteSceneRoundTripsIncludingNegativeOriginAndLighting() throws Exception {
        int[] states=new int[CameraLimits.CELLS],lights=states.clone(),tints=states.clone();
        for(int i=0;i<states.length;i++) { states[i]=i%73; lights[i]=(i%16)<<20; tints[i]=0x91bd59; }
        var frame = new CameraBlockFrame(-40,-64,7,states,lights,tints);
        var decoded = CameraBlockFrame.decompress(frame.compress());
        assertEquals(-40,decoded.x()); assertEquals(-64,decoded.y());
        assertArrayEquals(states,decoded.states()); assertArrayEquals(lights,decoded.lights());
        assertArrayEquals(tints,decoded.biomes());
    }
    @Test void rejectsTruncatedAndOversizedSceneStreams() {
        assertThrows(IOException.class,() -> CameraBlockFrame.decompress(new byte[]{1,2,3}));
        assertThrows(IOException.class,() -> CameraBlockFrame.decompress(new byte[CameraLimits.MAX_COMPRESSED+1]));
    }
}
