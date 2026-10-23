package com.goosethings.tools.intro;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class RoleIntroArtworkTest {
    @Test void allRealRolesAndOrdinaryDuckHaveReadableBundledArtwork() throws Exception {
        for (var role : RoleIntroCatalog.roles()) assertArtwork(role.tag());
        assertArtwork("Duck");
        assertEquals("dueling_dodo", RoleIntroArtwork.sprite("DuelingDodos").orElseThrow());
        assertEquals("witch_doctor", RoleIntroArtwork.sprite("WitchDoctor").orElseThrow());
        assertTrue(RoleIntroArtwork.sprite("unknown-role").isEmpty());
    }

    private void assertArtwork(String role) throws Exception {
        String sprite = RoleIntroArtwork.sprite(role).orElseThrow();
        try (var stream = getClass().getResourceAsStream("/assets/goosetools/textures/gui/role_intro/" + sprite + ".png")) {
            assertNotNull(stream, role);
            var image = ImageIO.read(stream);
            assertNotNull(image, role);
            assertTrue(image.getWidth() > 0 && image.getHeight() > 0, role);
        }
    }

    @Test void sourceColorsAndPartialTransparencySurviveStageLighting() {
        int red = RoleIntroArtwork.litPixel(0xFFFF3010, 0xAD72C6);
        int blue = RoleIntroArtwork.litPixel(0xFF1030FF, 0xAD72C6);
        assertTrue((red >> 16 & 255) > (red & 255));
        assertTrue((blue & 255) > (blue >> 16 & 255));
        assertTrue((red >>> 24) > 0 && (red >>> 24) < 128);
        assertEquals(0, RoleIntroArtwork.litPixel(0x00FFFFFF, 0xAD72C6));
        assertTrue((RoleIntroArtwork.litPixel(0x80FFFFFF, 0xAD72C6) >>> 24) < (red >>> 24));
    }

    @Test void allScreenAndSourceAspectRatiosFitBelowTextWithoutStretching() {
        for (int[] screen : new int[][]{{1920,1080},{2560,1080},{1024,768},{540,960}})
            for (int[] source : new int[][]{{57,57},{128,64},{64,128}}) {
                double textBottom = screen[1] * .44;
                var b = RoleIntroArtwork.bounds(screen[0],screen[1],source[0],source[1],textBottom);
                assertTrue(b.top() >= textBottom);
                assertTrue(b.top()+b.height() <= screen[1]*.7975+1);
                assertTrue(b.left() >= 0 && b.left()+b.width() <= screen[0]);
                assertEquals((double)source[0]/source[1],(double)b.width()/b.height(),.02);
                assertTrue(Math.abs(b.left()+b.width()*.5-screen[0]*.5)<=1);
            }
        assertEquals(0, RoleIntroArtwork.bounds(640,360,57,57,350).width());
    }
}
