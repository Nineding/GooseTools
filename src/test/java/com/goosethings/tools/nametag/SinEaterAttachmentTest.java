package com.goosethings.tools.nametag;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class SinEaterAttachmentTest {
    @Test void warningIsSpectatorOnlyAndFollowsActualBodyState() {
        var rule=NameTagAttachmentConfig.defaults().attachments().stream().filter(x->x.id().equals("sineater_blast")).findFirst().orElseThrow();
        var target=Set.of("players","sinEaterThreatened");
        assertTrue(rule.visible(false,Set.of("spectator"),Set.of("players"),target,false));
        assertFalse(rule.visible(false,Set.of("players","evil"),target,target,true));
        assertFalse(rule.visible(false,Set.of("spectator"),target,Set.of("players"),true));
        assertFalse(rule.visible(false,Set.of("spectator"),target,Set.of("players","sinEaterThreatened","inPelican"),true));
        assertFalse(rule.visible(false,Set.of("spectator"),target,Set.of("players","sinEaterThreatened","inTalk"),true));
    }
}
