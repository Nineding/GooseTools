package com.goosethings.tools.spectator;
import com.goosethings.tools.intro.RoleIntroCatalog;
import com.goosethings.tools.marker.PlayerMarkerCatalog;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class SpectatorGlowPaletteTest {
 @Test void everyNeutralMatchesIntroAndItsMeetingMarker() {
  var roles=RoleIntroCatalog.roles().stream().filter(r->r.faction()==2).toList();
  assertEquals(12,roles.size());
  for(var r:roles) {
   assertEquals(r.color(),SpectatorGlowPalette.player(Set.of("neutral",r.tag()),true),r.tag());
   assertEquals(0x55ffff,SpectatorGlowPalette.player(Set.of("neutral",r.tag()),false));
   var marker=PlayerMarkerCatalog.definitions().stream().filter(m->m.translationKey().equals(r.key())).findFirst().orElseThrow();
   assertEquals(r.color(),marker.glowRgb(),r.tag());
  }
  assertEquals(0xC4E6FB,SpectatorGlowPalette.role("Seagull",0));
  assertEquals(0xAD72C6,SpectatorGlowPalette.role("raven",0));
  assertEquals(0x55ff55,SpectatorGlowPalette.player(Set.of("good","LucidDreamer"),true));
  assertEquals(0xff5555,SpectatorGlowPalette.player(Set.of("evil"),true));
  assertEquals(0xffff55,SpectatorGlowPalette.player(Set.of("neutral"),true));
 }
 @Test void onlyActualSpectatorsAndCorpseModelsQualify() {
  assertTrue(SpectatorGlowPalette.observer(Set.of("spectator","gamingGGD")));
  assertFalse(SpectatorGlowPalette.observer(Set.of("spectator","gamingGGD","endGame")));
  assertFalse(SpectatorGlowPalette.observer(Set.of("spectator")));
  assertFalse(SpectatorGlowPalette.observer(Set.of("gamingGGD","inDream")));
  for(var tags:java.util.List.of(Set.of("deadbody"),Set.of("talk","dead"),Set.of("talk","sitting_body"))) assertTrue(SpectatorGlowPalette.corpse(tags));
  for(var tags:java.util.List.of(Set.of("ggd.missingHead","talk"),Set.of("rt16.reporterChair"),Set.of("ggd.corpseRoleLabel"))) assertFalse(SpectatorGlowPalette.corpse(tags));
  assertEquals(0xaa0000,SpectatorGlowPalette.CORPSE);
 }
}
