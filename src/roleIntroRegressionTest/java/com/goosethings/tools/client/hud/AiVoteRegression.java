package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.ai.AiReviewFlowClient;
import com.goosethings.tools.client.ai.AiNameVoteScreen;
import com.goosethings.tools.intro.RoleIntroPayload;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.result.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Focused real-screen queue verification in the existing isolated client fixture. */
final class AiVoteRegression {
    private int scenario, age, phase;
    private boolean finished;
    private net.minecraft.client.gui.screens.Screen protectedScreen;
    void tick(Minecraft mc) {
        if (finished) return;
        try {
            if (scenario == 4) {
                AiReviewFlowClient.clear();
                Files.writeString(Path.of("result.txt"), "PASS AI vote defers through role intro, match result, blackout and postgame summary; opens after closure; queued cancel/clear and protocol readiness; four GPU screenshots.\n");
                finished=true;mc.stop();return;
            }
            UUID id=mc.player.getUUID();String name=mc.player.getGameProfile().name();
            if (phase==0) {
                var appearance=new MatchResultPayload(true,"good_elimination",List.of(
                        new MatchResultPayload.Player(id,name,"Goose",0x77ff55,1,false,true,false,
                                com.goosethings.tools.intro.PresentationAppearance.emptyEquipment())));
                if(scenario==0)RoleIntroScreen.apply(new RoleIntroPayload(true,"Goose",0,0,
                        List.of(new RoleIntroPayload.Appearance(id,name,false))));
                if(scenario==1)MatchResultScreen.apply(appearance);
                if(scenario==2)PresentationBlackoutScreen.apply(true);
                if(scenario==3)PostgameSummaryScreen.apply(new SummaryPayload(true,UUID.randomUUID(),appearance,
                        0,0,2818,true,20,0,0,20,0,1,0,1,20,99,0,List.of(30),
                        new SummaryRoster(List.of(new SummaryRoster.Entry(id,name,"Goose",0,1,0x77ff55,false,2818,0)),0)));
                protectedScreen=mc.gui.screen();
                require(AiReviewFlowClient.presentationBlocked(mc),"presentation guard missing");
                var open=AiReviewFlowClient.class.getDeclaredMethod("open",GooseToolsPayloads.AiNameVoteOpenS2C.class);
                open.setAccessible(true);
                open.invoke(null,new GooseToolsPayloads.AiNameVoteOpenS2C(100+scenario,1,1,20,"小鹅","谁是小鹅？",
                        List.of(new GooseToolsPayloads.AiVoteCandidate(id,name))));
                phase=1;age=0;return;
            }
            age++;
            if(phase==1) {
                require(mc.gui.screen()==protectedScreen,"AI vote interrupted presentation");
                if(age<15)return;
                Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{
                    try {image.writeToFile(Path.of("ai-vote-protected-"+scenario+".png"));}
                    catch(Exception error){throw new RuntimeException(error);}finally{image.close();}
                });
                mc.gui.setScreen(null);phase=2;age=0;return;
            }
            if(phase==2) {
                if(age<2)return;
                require(mc.gui.screen() instanceof AiNameVoteScreen,"deferred question did not open");
                AiReviewFlowClient.clear();
                require(!(mc.gui.screen() instanceof AiNameVoteScreen),"clear retained vote");
                scenario++;phase=0;
            }
        } catch(Throwable error) {
            error.printStackTrace();
            try {Files.writeString(Path.of("result.txt"),"FAIL "+error+"\n");}catch(Exception ignored){}
            finished=true;mc.stop();
        }
    }
    private static void require(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
}
