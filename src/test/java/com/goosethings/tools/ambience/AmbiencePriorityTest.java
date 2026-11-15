package com.goosethings.tools.ambience;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AmbiencePriorityTest {
    @Test void eventsAndLegacyMusicHavePriority(){assertTrue(AmbiencePriority.background("minecraft","ggd/events/meeting_other",false));assertTrue(AmbiencePriority.background("minecraft","ggd/events/victoria",false));assertTrue(AmbiencePriority.background("minecraft","ggd/flacon_pelican_time_bgm",false));assertTrue(AmbiencePriority.background("minecraft","music.overworld",true));}
    @Test void bombTicksGunshotsAndOwnAmbienceAreNotMistakenForBackgroundMusic(){assertFalse(AmbiencePriority.background("minecraft","ggd/bombtick",false));assertFalse(AmbiencePriority.background("minecraft","ggd/gunshot",false));assertFalse(AmbiencePriority.background("ggd","ambience/goosechapel/tavern",false));}
    @Test void taskNotesOnlyDuckInsteadOfStoppingTheSound(){assertTrue(AmbiencePriority.taskNote("minecraft","block.note_block.bell"));assertFalse(AmbiencePriority.taskNote("minecraft","entity.item.pickup"));}
}
