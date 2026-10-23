package com.goosethings.tools.intro;
import org.junit.jupiter.api.Test;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RoleIntroActTest {
    @Test void sixScenariosSelectTheirOwnTitleAndBackground() {
        assertEquals("friends",RoleIntroAct.title(0,false));
        assertEquals("colleagues",RoleIntroAct.title(1,false));
        assertEquals("solo",RoleIntroAct.title(1,true));
        assertEquals("together",RoleIntroAct.title(3,false));
        assertEquals("together",RoleIntroAct.title(3,true));
        assertEquals("neutral",RoleIntroAct.title(2,false));
        assertEquals("“Friends”",RoleIntroAct.fallback("friends"));
        for(int i=0;i<4;i++) assertEquals(i==2,RoleIntroAct.keepRoleArtwork(i));
        for(var role:RoleIntroCatalog.roles()) if(role.faction()==2)
            assertEquals(role.color(),RoleIntroAct.color(2,role.tag()));
    }
    @Test void allFiveLanguagesContainQuotedFriendsAndEverySceneTitle() throws Exception {
        for(String lang:List.of("zh_cn","zh_tw","zh_hk","en_us","ja_jp")) {
            try(var input=getClass().getResourceAsStream("/assets/goosetools/lang/"+lang+".json")) {
                assertNotNull(input);
                var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
                for(String key:List.of("friends","colleagues","solo","together","neutral","duel"))
                    assertFalse(json.get("intro.goosetools.act2."+key).getAsString().isBlank());
                String friends=json.get("intro.goosetools.act2.friends").getAsString();
                assertTrue(friends.startsWith("“")||friends.startsWith("「"));
                assertTrue(friends.endsWith("”")||friends.endsWith("」"));
            }
        }
    }
    @Test void userProvidedMotifsAreBundledWithTheirOriginalDimensions() throws Exception {
        for(String asset:List.of("heart","bloody_dagger")) try(var input=getClass().getResourceAsStream(
                "/assets/goosetools/textures/gui/role_intro/"+asset+".png")) {
            assertNotNull(input);var image=javax.imageio.ImageIO.read(input);
            assertEquals(asset.equals("heart")?32:25,image.getWidth());assertEquals(image.getWidth(),image.getHeight());
        }
    }
}
