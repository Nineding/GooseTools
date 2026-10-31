package com.goosethings.tools.intro;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RoleIntroPolicyTest {
    private static RoleIntroPolicy.Player player(int seat, String... tags) {
        Set<String> all = new HashSet<>(List.of(tags)); all.add("players"); all.add("p" + seat);
        return new RoleIntroPolicy.Player(new UUID(0, seat), seat, all);
    }
    private static List<Integer> seats(List<RoleIntroPolicy.Member> roster, boolean small) {
        return roster.stream().filter(m -> m.small() == small).map(m -> (int) m.id().getLeastSignificantBits()).sorted().toList();
    }

    @Test void blindDuckMasksOtherDucksAndMimicWithoutLeakingTheHeroSeatOrder() {
        var duck = player(9, "evil", "Spy");
        var other = player(2, "evil", "Sniper");
        var mimic = player(5, "good", "Mimic");
        var goose = player(1, "good");
        var all = List.of(goose, other, mimic, duck);
        var roster = RoleIntroPolicy.roster(duck, all, true);
        assertEquals(duck.id(), roster.getFirst().id());
        assertEquals(3, roster.size());
        assertEquals(List.of(other.id(), mimic.id()), roster.stream().filter(RoleIntroPolicy.Member::anonymous)
                .map(RoleIntroPolicy.Member::id).toList());
        assertFalse(roster.getFirst().anonymous());
        assertTrue(RoleIntroPolicy.roster(duck, all, false).stream().noneMatch(RoleIntroPolicy.Member::anonymous));
        assertTrue(RoleIntroPolicy.roster(goose, all, true).stream().noneMatch(RoleIntroPolicy.Member::anonymous));
        assertTrue(RoleIntroPolicy.roster(mimic, all, true).stream().noneMatch(RoleIntroPolicy.Member::anonymous));
    }

    @Test void blindDuckLoverKeepsTheirPartnerAndMasksOnlyTheSideRoster() {
        var duck = player(1, "evil", "Lover", "lover_with_2");
        var partner = player(2, "good", "Mimic", "Lover", "lover_with_1");
        var ally = player(3, "evil");
        var mimic = player(4, "good", "Mimic");
        var all = List.of(duck, partner, ally, mimic);
        var roster = RoleIntroPolicy.roster(duck, all, true);
        assertEquals(List.of(1,2), seats(roster, false));
        assertEquals(List.of(3,4), seats(roster, true));
        assertTrue(roster.stream().filter(member -> !member.small()).noneMatch(RoleIntroPolicy.Member::anonymous));
        assertTrue(roster.stream().filter(RoleIntroPolicy.Member::small).allMatch(RoleIntroPolicy.Member::anonymous));
        assertTrue(RoleIntroPolicy.roster(partner, all, true).stream().noneMatch(RoleIntroPolicy.Member::anonymous));
        assertTrue(RoleIntroPolicy.roster(duck, all, false).stream().noneMatch(RoleIntroPolicy.Member::anonymous));
    }

    @Test void blindDuckSoloAndBirdDoNotInventAnonymousTeammates() {
        var duck = player(1, "evil"); var bird = player(2, "neutral", "Dodo");
        assertEquals(List.of(new RoleIntroPolicy.Member(duck.id(), false)), RoleIntroPolicy.roster(duck, List.of(duck, bird), true));
        assertEquals(List.of(new RoleIntroPolicy.Member(bird.id(), false)), RoleIntroPolicy.roster(bird, List.of(duck, bird), true));
    }

    @Test void eachViewerGetsExactlyTheRequestedFactionRoster() {
        var goose = player(1, "good", "Goose");
        var duck = player(2, "evil", "Spy");
        var duck2 = player(3, "evil", "Sniper");
        var mimic = player(4, "good", "Mimic");
        var bird = player(5, "neutral", "Falcon");
        var all = List.of(goose, duck, duck2, mimic, bird);
        assertEquals(List.of(1,2,3,4,5), seats(RoleIntroPolicy.roster(goose, all), false));
        assertEquals(List.of(2,3,4), seats(RoleIntroPolicy.roster(duck, all), false));
        assertEquals(List.of(5), seats(RoleIntroPolicy.roster(bird, all), false));
        assertEquals(List.of(1,2,3,4,5), seats(RoleIntroPolicy.roster(mimic, all), false));
        assertEquals(0, RoleIntroPolicy.theme(mimic.tags()));
        assertTrue(RoleIntroPolicy.roster(duck, all).stream().noneMatch(RoleIntroPolicy.Member::small));
    }

    @Test void duckLoverSeesPairAndSmallTeammatesWhileGoosePartnerDoesNot() {
        var goose = player(1, "good", "Lover", "lover_with_2");
        var duck = player(2, "evil", "Lover", "lover_with_1");
        var ally = player(3, "evil"); var mimic = player(4, "good", "Mimic");
        var bird = player(5, "neutral");
        var unrelated = player(6, "evil", "Lover", "lover_with_7");
        var unrelated2 = player(7, "good", "Lover", "lover_with_6");
        var all = List.of(goose, duck, ally, mimic, bird, unrelated, unrelated2);
        assertEquals(List.of(1,2), seats(RoleIntroPolicy.roster(duck, all), false));
        assertEquals(List.of(3,4), seats(RoleIntroPolicy.roster(duck, all), true));
        assertEquals(List.of(1,2), seats(RoleIntroPolicy.roster(goose, all), false));
        assertTrue(seats(RoleIntroPolicy.roster(goose, all), true).isEmpty());
        assertEquals(RoleIntroPolicy.LOVER, RoleIntroPolicy.theme(duck.tags()));
        assertEquals(RoleIntroPolicy.LOVER, RoleIntroPolicy.theme(goose.tags()));
    }

    @Test void mimicPartnerIsNotRepeatedInSmallTeammates() {
        var duck = player(1, "evil", "Lover", "lover_with_21");
        var mimic = player(21, "good", "Mimic", "Lover", "lover_with_1");
        var all = List.of(duck, mimic, player(2, "evil"));
        assertEquals(List.of(1,21), seats(RoleIntroPolicy.roster(duck, all), false));
        assertEquals(List.of(2), seats(RoleIntroPolicy.roster(duck, all), true));
    }

    @Test void legacyTwoLoversFallbackDoesNotGuessWhenAnExplicitEdgeExists() {
        var a = player(1,"good","Lover"); var b = player(2,"evil","Lover");
        assertEquals(List.of(1,2), seats(RoleIntroPolicy.roster(a,List.of(a,b)),false));
        var wrongEdge = player(2,"evil","Lover","lover_with_3");
        assertEquals(List.of(1), seats(RoleIntroPolicy.roster(a,List.of(a,wrongEdge)),false));
    }

    @Test void dlcAndReadyAndParticipantGatesAreAllRequired() {
        var tags = player(1,"good").tags();
        assertFalse(RoleIntroPolicy.eligible(false,tags,1));
        assertFalse(RoleIntroPolicy.eligible(true,tags,0));
        assertFalse(RoleIntroPolicy.eligible(true,tags,3));
        assertTrue(RoleIntroPolicy.eligible(true,tags,1));
        assertTrue(RoleIntroPolicy.eligible(true,tags,2));
        assertFalse(RoleIntroPolicy.eligible(true,player(1,"spectator").tags(),1));
        assertFalse(RoleIntroPolicy.eligible(true,player(1,"inTutorial").tags(),1));
        assertFalse(RoleIntroPolicy.eligible(true,Set.of("good"),1));
    }

    @Test void twentyOnePlayersRemainVisibleInSeatOrderIncludingTheHero() {
        List<RoleIntroPolicy.Player> all = new ArrayList<>();
        for (int i=1;i<=21;i++) all.add(player(i,i%2==0?"evil":"good"));
        var viewer = all.get(20); var roster = RoleIntroPolicy.roster(viewer,all);
        assertEquals(21,roster.size()); assertEquals(viewer.id(),roster.get(20).id());
        assertEquals(21, RoleIntroPolicy.seat(viewer.tags()));
        assertEquals(21,roster.stream().map(RoleIntroPolicy.Member::id).distinct().count());
    }

    @Test void duelOnlyDisclosesOtherDuelingDodosToThePair() {
        var a=player(3,"neutral","DuelingDodos","dueling"); var b=player(9,"neutral","DuelingDodos","dueling");
        var bird=player(4,"neutral","Raven"); var goose=player(1,"good"); var dodo=player(2,"neutral","Dodo");
        var all=List.of(b,bird,a,goose,dodo);
        assertEquals(List.of(3,9),seats(RoleIntroPolicy.roster(a,all),false));
        assertEquals(List.of(3,9),seats(RoleIntroPolicy.roster(b,all),false));
        assertEquals(a.id(),RoleIntroPolicy.roster(a,all).getFirst().id());
        assertEquals(List.of(4),seats(RoleIntroPolicy.roster(bird,all),false));
        assertEquals(List.of(2),seats(RoleIntroPolicy.roster(dodo,all),false));
        assertEquals(List.of(3),seats(RoleIntroPolicy.roster(a,List.of(a,bird,dodo)),false));
        assertTrue(RoleIntroAct.duel(2,"DuelingDodos",2));
        assertFalse(RoleIntroAct.duel(2,"DuelingDodos",1));
        assertFalse(RoleIntroAct.duel(2,"Dodo",2));
        assertEquals("duel",RoleIntroAct.title(2,false,"DuelingDodos",2));
        assertEquals("neutral",RoleIntroAct.title(2,false,"DuelingDodos",1));
    }

    @Test void catalogCoversExistingRolesAndKeepsEnglishFallbacks() {
        assertEquals(70,RoleIntroCatalog.roles().size());
        assertEquals("role.evil.swordman",RoleIntroCatalog.byTag("Swordman").key());
        assertEquals("role.evil.sineater",RoleIntroCatalog.byTag("SinEater").key());
        assertEquals("role.evil.snitch",RoleIntroCatalog.byTag("Snitch").key());
        assertEquals("role.neutral.seagull",RoleIntroCatalog.byTag("Seagull").key());
        assertEquals("role.good.mimic",RoleIntroCatalog.byTag("Mimic").key());
        for (var role:RoleIntroCatalog.roles()) {
            assertFalse(role.fallback().isBlank()); assertFalse(role.descriptionFallback().isBlank());
            assertTrue(role.fallback().chars().noneMatch(c -> c>=0x4e00 && c<=0x9fff));
        }
    }

    @Test void whiteRedYellowPinkStageTintsAndFadeBoundaries() {
        int white=RoleIntroScene.stagePixel(0,.5,.9), red=RoleIntroScene.stagePixel(1,.5,.9);
        int yellow=RoleIntroScene.stagePixel(2,.5,.9), pink=RoleIntroScene.stagePixel(3,.5,.9);
        assertEquals(white>>16&255,white&255);
        assertTrue((red>>16&255)>(red>>8&255)*2);
        assertTrue((yellow>>8&255)>(yellow&255)*2);
        assertTrue((pink&255)>(pink>>8&255));
        assertEquals(0,RoleIntroScene.brightness(0));
        assertEquals(1,RoleIntroScene.brightness(600_000_000L));
        assertEquals(200,RoleIntroScene.DURATION_TICKS);
        assertEquals(10_000_000_000L,RoleIntroScene.DURATION_NANOS);
        assertEquals(1,RoleIntroScene.brightness(4_000_000_000L));
        assertEquals(1,RoleIntroScene.brightness(6_400_000_000L));
        assertEquals(0,RoleIntroScene.brightness(10_000_000_000L));
        assertEquals(0,RoleIntroScene.brightness(Long.MAX_VALUE));
    }

    @Test void smallRostersCloseTheGapsWithoutShrinkingModelsOrCrowdingFullRosters() {
        float scale=61.2F;
        assertEquals(scale*1.4F,RoleIntroScene.rowStep(524.8F,2,scale));
        assertEquals(scale*1.4F,RoleIntroScene.rowStep(524.8F,3,scale));
        assertEquals(scale*1.4F,RoleIntroScene.rowStep(275.2F,2,scale));
        assertEquals(524.8F/21,RoleIntroScene.rowStep(524.8F,21,scale));
        assertNotEquals(RoleIntroScene.modelId(player(1).id()),player(1).id());
        assertNotEquals(RoleIntroScene.modelId(player(1).id()),RoleIntroScene.modelId(player(2).id()));
    }

    @Test void followingParasiteReceivesTheOpeningOnlyWhileParticipantsAreLocked() {
        assertTrue(RoleIntroPolicy.hasOpeningLock(Set.of("Parasite","parasiteInside"),true));
        assertFalse(RoleIntroPolicy.hasOpeningLock(Set.of("Parasite","parasiteInside"),false));
        assertFalse(RoleIntroPolicy.hasOpeningLock(Set.of("Parasite","parasiteFree"),true));
        assertFalse(RoleIntroPolicy.hasOpeningLock(Set.of("Goose"),true));
        assertTrue(RoleIntroPolicy.hasOpeningLock(Set.of("ggdRoleShowLocked"),false));
    }
}
