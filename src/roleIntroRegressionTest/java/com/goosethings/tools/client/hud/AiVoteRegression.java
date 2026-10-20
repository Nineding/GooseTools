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
            if(scenario==4) {reportScenario(mc);return;}
            if (scenario == 5) {
                AiReviewFlowClient.clear();
                Files.writeString(Path.of("result.txt"), "PASS protocol 38: name votes defer through role/result/blackout/summary, accelerated countdown and scoped clear; two background reports survive the role screen and open in order during gameplay; matching previous-match settlement opens and returns to its report. Seven GPU screenshots.\n");
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
                open.invoke(null,new GooseToolsPayloads.AiNameVoteOpenS2C(100+scenario,77,1,1,20,"小鹅","谁是小鹅？",
                        List.of(new GooseToolsPayloads.AiVoteCandidate(id,name))));
                phase=1;age=0;return;
            }
            age++;
            if(phase==1) {
                require(mc.gui.screen()==protectedScreen,"AI vote interrupted presentation");
                if(age<15)return;
                screenshot(mc,"ai-vote-protected-"+scenario+".png");
                mc.gui.setScreen(null);phase=2;age=0;return;
            }
            if(phase==2) {
                if(age<2)return;
                require(mc.gui.screen() instanceof AiNameVoteScreen,"deferred question did not open");
                var clearMatch=AiReviewFlowClient.class.getDeclaredMethod("clearMatch",long.class);clearMatch.setAccessible(true);
                clearMatch.invoke(null,78L);require(mc.gui.screen() instanceof AiNameVoteScreen,"old job cleared another match's vote");
                var accelerate=AiNameVoteScreen.class.getDeclaredMethod("accelerate",long.class);accelerate.setAccessible(true);
                accelerate.invoke(mc.gui.screen(),System.currentTimeMillis()+5000);
                if(scenario==0 && age<8) return;
                if(scenario==0)screenshot(mc,"ai-vote-accelerated.png");
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
    private void reportScenario(Minecraft mc) throws Exception {
        UUID id=mc.player.getUUID();String name=mc.player.getGameProfile().name();
        if(phase==0) {
            var cache=com.goosethings.tools.client.ai.AiSummaryClient.class.getDeclaredField("CACHE");cache.setAccessible(true);
            @SuppressWarnings("unchecked") var summaries=(java.util.Map<Long,SummaryPayload>)cache.get(null);
            var appearance=new MatchResultPayload(true,"good_elimination",List.of(new MatchResultPayload.Player(id,name,"Goose",0x77ff55,1,false,true,false,com.goosethings.tools.intro.PresentationAppearance.emptyEquipment())));
            summaries.put(901L,new SummaryPayload(true,UUID.randomUUID(),appearance,1,0,2818,true,28,20,0,33,70,1,0,2,13,100,0,List.of(20,30),new SummaryRoster(List.of(new SummaryRoster.Entry(id,name,"Goose",0,1,0x77ff55,false,2818,1)),3)));
            RoleIntroScreen.apply(new RoleIntroPayload(true,"Duck",1,0,List.of(new RoleIntroPayload.Appearance(id,name,false))));
            protectedScreen=mc.gui.screen();
            var field=com.goosethings.tools.client.GooseToolsClient.class.getDeclaredField("AI_REPORT");field.setAccessible(true);Object receiver=field.get(null);
            var clear=receiver.getClass().getDeclaredMethod("clear");clear.setAccessible(true);clear.invoke(receiver);
            for(long game:new long[]{901,902}) {
                com.google.gson.JsonObject report=new com.google.gson.JsonObject();report.addProperty("game_id",game);
                var global=new com.google.gson.JsonObject();global.addProperty("title","上一局在后台完成分析");global.addProperty("summary","玩家已经进入下一局；上一局的指数和奖励独立结算。");global.add("turning_points",new com.google.gson.JsonArray());report.add("global",global);report.add("highlights",new com.google.gson.JsonArray());
                var personal=new com.google.gson.JsonObject();personal.addProperty("player_name",name);personal.addProperty("settled_index",2818);personal.addProperty("index_grade","N");personal.addProperty("score_source","ai");personal.addProperty("index_reasoning","按上一局证据评定，与当前新局身份无关。");personal.addProperty("gained_xp",33);personal.addProperty("gained_coins",70);personal.addProperty("award",1);report.add("personal",personal);
                byte[] compressed;
                try(var out=new java.io.ByteArrayOutputStream()) {try(var gzip=new java.util.zip.GZIPOutputStream(out)){gzip.write(report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}compressed=out.toByteArray();}
                var begin=receiver.getClass().getDeclaredMethod("begin",GooseToolsPayloads.AiReportStartS2C.class);begin.setAccessible(true);begin.invoke(receiver,new GooseToolsPayloads.AiReportStartS2C(game,compressed.length,1,true));
                var accept=receiver.getClass().getDeclaredMethod("accept",long.class,int.class,byte[].class);accept.setAccessible(true);accept.invoke(receiver,game,0,compressed);
            }
            phase=1;age=0;return;
        }
        age++;
        if(phase==1) {
            require(mc.gui.screen()==protectedScreen,"background report interrupted a new role intro");
            if(age<20)return;
            mc.gui.setScreen(null);phase=2;age=0;return;
        }
        if(phase==2) {
            if(age<4)return;
            require(reportId(mc.gui.screen())==901,"first background report was lost or overwritten");
            screenshot(mc,"ai-background-report.png");
            boolean pressed=false;
            for(var child:mc.gui.screen().children()) if(child instanceof net.minecraft.client.gui.components.Button button) {
                for(var field:net.minecraft.client.gui.components.Button.class.getDeclaredFields()) if(field.getType().getSimpleName().equals("OnPress")) {
                    field.setAccessible(true);Object action=field.get(button);
                    for(var method:field.getType().getMethods()) if(method.getParameterCount()==1 && method.getParameterTypes()[0].isInstance(button)) {method.invoke(action,button);pressed=true;break;}
                    if(pressed)break;
                }
                if(pressed)break;
            }
            require(pressed&&mc.gui.screen() instanceof PostgameSummaryScreen,"View settlement did not open the old frozen summary");
            phase=3;age=0;return;
        }
        if(phase==3) {
            require(mc.gui.screen() instanceof PostgameSummaryScreen,"queued report interrupted the settlement view");
            if(age<10)return;
            require(((PostgameSummaryScreen)mc.gui.screen()).payload().spotlightIndex()==2818,"settlement used the new match");
            screenshot(mc,"ai-previous-summary.png");
            mc.gui.screen().onClose();require(reportId(mc.gui.screen())==901,"settlement did not return to its report");
            mc.gui.screen().onClose();phase=4;age=0;return;
        }
        if(phase==4) {
            if(age<3)return;
            require(reportId(mc.gui.screen())==902,"second completed match did not open after the first");
            mc.gui.screen().onClose();scenario=5;phase=0;
        }
    }
    private long reportId(net.minecraft.client.gui.screens.Screen screen) throws Exception {
        require(screen instanceof com.goosethings.tools.client.ai.AiReportScreen,"expected an AI report screen");
        var field=screen.getClass().getDeclaredField("report");field.setAccessible(true);return ((com.google.gson.JsonObject)field.get(screen)).get("game_id").getAsLong();
    }
    private void screenshot(Minecraft mc,String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{
            try {image.writeToFile(Path.of(name));}catch(Exception error){throw new RuntimeException(error);}finally{image.close();}
        });
    }
    private static void require(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
}
