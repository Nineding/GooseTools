package com.goosethings.tools.intro;
import com.goosethings.tools.result.MatchResultPolicy;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class NeutralPaletteTest {
    @Test void catalogAndVictoryUseTheApprovedPalette() {
        var palette=Map.ofEntries(Map.entry("raven",0xAD72C6),Map.entry("falcon",0x28B8CD),Map.entry("magpie",0x31568E),Map.entry("pelican",0x7BC9A2),Map.entry("vulture",0x5378A6),Map.entry("pigeon",0xEB740B),Map.entry("dodo",0xF3C13D),Map.entry("duelingdodos",0xF3C13D),Map.entry("cuckoo",0x65CDBB),Map.entry("sparrow",0xB58A62),Map.entry("seagull",0xC4E6FB),Map.entry("phoenix",0xF26B4F));
        assertEquals(12,RoleIntroCatalog.roles().stream().filter(r->r.faction()==2).count());
        for(var entry:palette.entrySet()) {
            var role=RoleIntroCatalog.findByStoredId(entry.getKey()).orElseThrow();
            assertEquals(entry.getValue().intValue(),role.color());
            assertEquals(role.color(),MatchResultPolicy.neutralColor(entry.getKey()));
            assertNotEquals(RoleIntroScene.stagePixel(RoleIntroPolicy.BIRD,.5,.9),
                    RoleIntroScene.stagePixel(RoleIntroPolicy.BIRD,role.tag(),.5,.9));
            for(int theme:new int[]{RoleIntroPolicy.GOOSE,RoleIntroPolicy.DUCK,RoleIntroPolicy.LOVER})
                assertEquals(RoleIntroScene.stagePixel(theme,.5,.9),RoleIntroScene.stagePixel(theme,role.tag(),.5,.9));
        }
    }
}
