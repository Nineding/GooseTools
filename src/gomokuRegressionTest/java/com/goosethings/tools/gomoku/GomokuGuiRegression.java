package com.goosethings.tools.gomoku;

import com.goosethings.tools.client.gomoku.GomokuClient;
import com.goosethings.tools.boardgame.BoardPackets;
import com.goosethings.tools.boardgame.BoardGameItems;
import com.goosethings.tools.boardgame.ChessPosition;
import com.goosethings.tools.client.gomoku.GomokuScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.input.*;
import java.nio.file.*;
import java.util.*;

/** Two separate clients exercise the real handshake, packets, native screens and pointer transforms. */
public final class GomokuGuiRegression implements ClientModInitializer {
    private final String role=System.getProperty("goosetools.gomokuRole","A");
    private boolean connecting,loaded,done,language,ready;
    private int step,frames,scale=1,difficulty;
    private long since=System.nanoTime(),matchId,revision=-1;
    private int originalColor,turnAtHide;
    private final Set<String> captures=new HashSet<>();
    private String partnerStage="";
    private String pendingCapture;
    private long renderedId=-1,renderedRevision=-1;
    private int stableFrames;
    private int spectatorEntry, boardKind = 1;
    private boolean extrasDone, gamepadHidden;
    private static Path output(){return GomokuRegressionSupport.output();}
    @Override public void onInitializeClient(){if(Boolean.getBoolean("goosetools.gomokuRegression"))ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void tick(Minecraft mc){
        if(done||!mc.isGameLoadFinished())return;
        try{
            if(!language){language=true;mc.options.languageCode="zh_cn";mc.options.pauseOnLostFocus=false;mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);mc.getLanguageManager().setSelected("zh_cn");mc.reloadResourcePacks().whenComplete((v,e)->mc.execute(()->{if(e!=null)finish(mc,"FAIL language "+e);else ready=true;}));return;}
            if(!ready)return;
            mc.gui.toastManager().clear();
            if(pendingCapture!=null){saveCapture(mc,pendingCapture);pendingCapture=null;}
            if(!connecting){
                if(!Files.exists(output().resolve("server-ready.txt")))return;
                connecting=true;mc.getWindow().setWindowed(1100,800);
                ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString("127.0.0.1:25586"),new ServerData("Gomoku regression","127.0.0.1:25586",ServerData.Type.OTHER),false,null);since=System.nanoTime();return;
            }
            require((System.nanoTime()-since)/1_000_000<120_000,"timeout role="+role+" step="+step+" screen="+mc.gui.screen());
            if(role.equals("A")&&step==37&&Files.exists(output().resolve("server-pass.txt"))){finish(mc,Boolean.getBoolean("goosetools.boardRegressionOnly") ? "PASS board-games-only: lobby world use, Xiangqi/chess 3 AI levels, legal GUI moves, synchronized PvP and chat acceptance, hidden HUD and return, native spectator gamepad and meeting relocation, disconnect, admin open/close, preparation/tutorial and session cancellation; screenshots="+captures.size() : "PASS real two-client handshake, invite, board sync, five-in-row, swap rematch, hidden timeout, F9 and /gomoku, meetings and dimension, DLC ghost and vanilla spectator, revival and swallowed rejection, 3 AI levels, resignation, results, menu buttons, admin open/close, preparation/tutorial rejection and session cancellation; Xiangqi and chess 3 AI levels, two-client chess sync and chat accept, hidden HUD, native spectator hotbar meeting relocation; screenshots="+captures.size());return;}
            if(mc.player==null||!mc.player.connection.hasClientLoaded()||GomokuClient.view()==null)return;
            if(!loaded){loaded=true;if(role.equals("A")&&Boolean.getBoolean("goosetools.boardRegressionOnly")){step=16;control(6);}
            Files.writeString(output().resolve(role+"-connected.txt"),mc.player.getName().getString());}
            var v=GomokuClient.view();var p=v.position();frames++;
            if(frames%20==0)Files.writeString(output().resolve(role+"-progress.txt"),"step="+step+" group="+v.group()+" match="+p.id()+" outcome="+p.outcome()+" phase="+com.goosethings.tools.client.presence.GamePresenceClient.phaseId()+" screen="+mc.gui.screen());
            if(role.equals("B")){partner(mc,v);return;}
            if(p.id()!=renderedId||p.revision()!=renderedRevision){renderedId=p.id();renderedRevision=p.revision();stableFrames=0;}
            if(++stableFrames<4||frames<5)return; // Let the native screen render its current controls before clicking.
            switch(step){
                case 0->{if(!Files.exists(output().resolve("B-connected.txt"))||v.group()!=1)return;setStage("idle");key();next();}
                case 1->{if(!(mc.gui.screen() instanceof GomokuScreen s))return;if(frames==5){mc.options.guiScale().set(scale);mc.resizeGui();}if(frames<15)return;capture(mc,"menu-scale-"+scale);frames=0;if(++scale<=3)return;next();}
                case 2->{if(frames==5){mc.options.guiScale().set(2);mc.resizeGui();return;}if(!(mc.gui.screen() instanceof GomokuScreen s)||v.players().isEmpty())return;setStage("duel");click(s,85,110);next();}
                case 3->{if(p.id()==0)return;matchId=p.id();originalColor=p.color();capture(mc,"pvp-board");next();}
                case 4->{
                    if(p.outcome()!=0){require(p.outcome()==p.color(),"A did not win");require(p.line().length==5,"missing winning line");capture(mc,"pvp-win");Files.writeString(output().resolve("winning-board.txt"),Arrays.toString(p.board()));setStage("rematch");next();return;}
                    if(p.turn()==p.color()&&revision!=p.revision()&&mc.gui.screen() instanceof GomokuScreen s){int cell=112;while(p.board()[cell]!=0)cell++;revision=p.revision();clickCell(s,cell);}
                }
                case 5->{if(!(mc.gui.screen() instanceof GomokuScreen s)||!Files.exists(output().resolve("B-win.txt")))return;click(s,490,306);next();}
                case 6->{if(p.id()==matchId||p.id()==0)return;require(p.color()==3-originalColor,"rematch did not swap");setStage("idle");turnAtHide=p.turn();control(4);key();next();}
                case 7->{require(!(mc.gui.screen() instanceof GomokuScreen),"F9 did not hide");if(p.outcome()==0)return;require(p.outcome()==3-turnAtHide&&p.reason()==2,"hidden clock did not expire");mc.player.connection.sendCommand("gomoku");next();}
                case 8->{if(!(mc.gui.screen() instanceof GomokuScreen s))return;capture(mc,"hidden-timeout");click(s,490,344);next();}
                case 9->{if(p.id()!=0)return;control(1);next();}
                case 10->{if(v.group()!=2||v.players().isEmpty()||!(mc.gui.screen() instanceof GomokuScreen s))return;setStage("ghost-duel");click(s,85,110);next();}
                case 11->{if(p.id()==0)return;matchId=p.id();capture(mc,"meeting-play");control(2);next();}
                case 12->{if(frames<35)return;require(v.group()==2&&p.id()==matchId,"meeting dimension change cancelled game");if(!(mc.gui.screen() instanceof GomokuScreen)){key();return;}capture(mc,"meeting-end-dimension");control(3);next();}
                case 13->{if(frames<25)return;require(p.id()==matchId,"meeting end cancelled game");control(5);next();}
                case 14->{if(v.group()!=0)return;require(p.id()==0,"revival retained game");GomokuClient.action(BoardPackets.AI,1,1,BoardPackets.NONE);GomokuClient.open();control(8);next();}
                case 15->{if(frames<20)return;require(v.group()==0&&p.id()==0,"swallowed player admitted");control(6);next();}
                case 16->{if(v.group()!=1)return;key();setStage("idle");next();}
                case 17->{if(Boolean.getBoolean("goosetools.boardRegressionOnly")){step=23;frames=0;return;}if(!(mc.gui.screen() instanceof GomokuScreen s)||p.id()!=0)return;click(s,65+difficulty*72,278);click(s,520,278);next();}
                case 18->{if(p.id()==0||!(mc.gui.screen() instanceof GomokuScreen s))return;require(p.difficulty()==difficulty,"AI difficulty mismatch");if(p.board()[112]==0){clickCell(s,112);return;}if(count(p.board())<2)return;capture(mc,"ai-"+difficulty);click(s,490,312);next();}
                case 19->{if(!(mc.gui.screen() instanceof GomokuScreen s))return;click(s,440,330);next();}
                case 20->{if(p.outcome()==0||!(mc.gui.screen() instanceof GomokuScreen s))return;require(p.reason()==3,"resign confirmation failed");click(s,490,344);next();}
                case 21->{if(p.id()!=0)return;if(++difficulty<3){step=17;frames=0;return;}control(7);next();}
                case 22->{if(v.group()!=0)return;require(!(mc.gui.screen() instanceof GomokuScreen),"results did not close");control(6);next();}
                case 23->{if(v.group()!=1)return;mc.setScreenAndShow(new InventoryScreen(mc.player));next();}
                case 24->{if(frames<6)return;require(mc.gui.screen() instanceof InventoryScreen,"inventory unavailable");if(frames==6){capture(mc,"menu-entry");return;}clickEntry(mc);next();}
                case 25->{if(frames<80)return;if(!extrasDone){difficulty=0;step=40;frames=0;return;}if(!(mc.gui.screen() instanceof GomokuScreen s)||v.players().isEmpty())return;setStage("disconnect-duel");click(s,85,110);next();}
                case 26->{if(p.id()==0)return;setStage("disconnect-now");next();}
                case 27->{if(p.reason()!=4||p.outcome()==0)return;require(p.outcome()==p.color(),"disconnect did not forfeit");capture(mc,"disconnect-win");control(10);next();}
                case 28->{if(p.id()!=0||mc.gui.screen() instanceof GomokuScreen)return;control(11);next();}
                case 29->{if(!(mc.gui.screen() instanceof GomokuScreen s))return;click(s,520,278);next();}
                case 30->{if(p.id()==0)return;control(12);next();}
                case 31->{if(v.group()!=0)return;require(p.id()==0&&!(mc.gui.screen() instanceof GomokuScreen),"preparation did not cancel");GomokuClient.action(BoardPackets.AI,1,1,BoardPackets.NONE);control(13);next();}
                case 32->{if(frames<20)return;require(v.group()==0&&p.id()==0,"tutorial bypass");control(6);next();}
                case 33->{if(v.group()!=1)return;control(11);next();}
                case 34->{if(!(mc.gui.screen() instanceof GomokuScreen s))return;click(s,520,278);next();}
                case 35->{if(p.id()==0)return;control(14);next();}
                case 36->{if(p.id()!=0)return;control(99);next();}
                case 40->{if(mc.gui.screen() instanceof GomokuScreen screen)screen.onClose();control(15);next();}
                case 41->{if(frames<25)return;capture(mc,"lobby-gamepad-world");mc.options.keyUse.setDown(true);next();}
                case 42->{mc.options.keyUse.setDown(false);if(!(mc.gui.screen() instanceof GomokuScreen))return;step=50;frames=0;}
                case 50->{if(!(mc.gui.screen() instanceof GomokuScreen s)||p.id()!=0)return;click(s,100+boardKind*204,190);next();}
                case 51->{if(!(mc.gui.screen() instanceof GomokuScreen s))return;click(s,65+difficulty*72,278);click(s,520,278);next();}
                case 52->{if(p.id()==0||!(mc.gui.screen() instanceof GomokuScreen s))return;require(p.kind()==boardKind&&p.difficulty()==difficulty,"new AI kind/difficulty");capture(mc,"board-"+boardKind+"-ai-"+difficulty);revision=p.revision();clickChess(s,p,p.legal()[0]);next();}
                case 53->{if(p.revision()<revision+2||p.turn()!=p.color())return;require(p.last()>=0,"AI failed to move");key();next();}
                case 54->{if(frames<25)return;require(!(mc.gui.screen() instanceof GomokuScreen),"hidden chess reopened");if(frames==25){capture(mc,"hud-"+boardKind+"-"+difficulty);return;}mc.player.connection.sendCommand("boardgames");next();}
                case 55->{if(!(mc.gui.screen() instanceof GomokuScreen s))return;click(s,490,312);next();}
                case 56->{if(!(mc.gui.screen() instanceof GomokuScreen s))return;click(s,440,330);next();}
                case 57->{if(p.outcome()==0||!(mc.gui.screen() instanceof GomokuScreen s))return;click(s,490,344);next();}
                case 58->{if(p.id()!=0)return;if(++difficulty<3){step=50;frames=0;return;}difficulty=0;if(++boardKind<=2){step=50;frames=0;return;}boardKind=1;step=60;frames=0;}
                case 60->{if(frames<80)return;if(!(mc.gui.screen() instanceof GomokuScreen s)||v.players().isEmpty())return;click(s,100+boardKind*204,190);setStage("chess-duel-"+boardKind);click(s,85,110);next();}
                case 61->{if(p.id()==0)return;require(p.kind()==boardKind,"PvP game kind");matchId=p.id();revision=-1;next();}
                case 62->{if(p.revision()<3){if(p.turn()==p.color()&&revision!=p.revision()&&mc.gui.screen() instanceof GomokuScreen s){revision=p.revision();clickChess(s,p,p.legal()[0]);}return;}Files.writeString(output().resolve("chess-board-"+boardKind+".txt"),Arrays.toString(p.board()));capture(mc,"chess-pvp-"+boardKind);next();}
                case 63->{if(!Files.exists(output().resolve("B-chess-"+boardKind+".txt")))return;if(!(mc.gui.screen() instanceof GomokuScreen s))return;click(s,490,312);next();}
                case 64->{if(!(mc.gui.screen() instanceof GomokuScreen s))return;click(s,440,330);next();}
                case 65->{if(p.outcome()==0||!(mc.gui.screen() instanceof GomokuScreen s))return;click(s,490,344);next();}
                case 66->{if(p.id()!=0)return;if(++boardKind<=2){step=60;frames=0;return;}control(1);setStage("gamepad");next();}
                case 67->{if(v.group()!=2||!BoardGameItems.isGamepad(mc.player.getInventory().getItem(2)))return;require(!BoardGameItems.isGamepad(mc.player.getInventory().getItem(1)),"meeting gamepad duplicate");if(!Files.exists(output().resolve("B-gamepad.txt")))return;capture(mc,"meeting-gamepad");control(3);next();}
                case 68->{if(!BoardGameItems.isGamepad(mc.player.getInventory().getItem(1))||!Files.exists(output().resolve("B-gamepad.txt")))return;require(!BoardGameItems.isGamepad(mc.player.getInventory().getItem(2)),"nonmeeting gamepad duplicate");control(6);next();}
                case 69->{if(v.group()!=1)return;GomokuClient.selectedKind=0;extrasDone=true;setStage("idle");GomokuClient.open();step=25;frames=0;}

            }
        }catch(Throwable e){com.goosethings.tools.GooseTools.LOGGER.error("Gomoku GUI regression failed",e);finish(mc,"FAIL "+e);}
    }
    private void partner(Minecraft mc,BoardPackets.View v)throws Exception{
        String stage=Files.exists(output().resolve("stage.txt"))?Files.readString(output().resolve("stage.txt")):"idle";var p=v.position();
        if(!stage.equals(partnerStage)){partnerStage=stage;since=System.nanoTime();}
        if(p.id()!=matchId){matchId=p.id();revision=-1;}
        if(stage.equals("ghost-duel")&&spectatorEntry<3){
            if(spectatorEntry==0){mc.setScreenAndShow(new net.minecraft.client.gui.screens.PauseScreen(true));spectatorEntry=1;frames=0;return;}
            if(spectatorEntry==1){if(frames<6)return;if(frames==6){capture(mc,"spectator-esc-entry");return;}clickEntry(mc);spectatorEntry=2;frames=0;return;}
            if(!(mc.gui.screen() instanceof GomokuScreen)){require(frames<40,"spectator Esc button did not open");return;}spectatorEntry=3;
        }
        if(stage.equals("gamepad")&&!Files.exists(output().resolve("B-gamepad.txt"))){
            if(!gamepadHidden){gamepadHidden=true;if(mc.gui.screen() instanceof GomokuScreen screen)screen.onClose();return;}
            if(v.group()!=2||!BoardGameItems.isGamepad(mc.player.getInventory().getItem(2)))return;
            require(GomokuClient.spectatorHotbar(mc),"vanilla spectator hotbar missing");
            if(mc.gui.screen() instanceof GomokuScreen){mc.options.keyUse.setDown(false);capture(mc,"vanilla-spectator-gamepad");Files.writeString(output().resolve("B-gamepad.txt"),"PASS native spectator gamepad opens without game mode changes");return;}
            mc.player.getInventory().setSelectedSlot(2);mc.options.keyUse.setDown(true);return;
        }
        if(stage.startsWith("chess-duel-")&&p.id()!=0){
            if(p.revision()<3&&p.turn()==p.color()&&revision!=p.revision()&&mc.gui.screen() instanceof GomokuScreen s){revision=p.revision();clickChess(s,p,p.legal()[0]);}
            Path comparison=output().resolve("chess-board-"+p.kind()+".txt");
            if(Files.exists(comparison)){require(Files.readString(comparison).equals(Arrays.toString(p.board())),"chess sync mismatch");Files.writeString(output().resolve("B-chess-"+p.kind()+".txt"),"PASS identical boards");}
        }
        if(v.inviteDirection()==2&&v.position().id()==0){
            if(stage.startsWith("chess-duel-")){if(frames%10==0)mc.player.connection.sendCommand("boardgames accept "+v.invite());return;}

            if(!(mc.gui.screen() instanceof GomokuScreen)){GomokuClient.open();return;}
            if(frames%10==0)click((GomokuScreen)mc.gui.screen(),480,378);return;
        }
        if(stage.equals("duel")&&p.id()!=0){
            if(p.outcome()!=0){
                if(Files.exists(output().resolve("winning-board.txt"))){require(Files.readString(output().resolve("winning-board.txt")).equals(Arrays.toString(p.board())),"different boards");Files.writeString(output().resolve("B-win.txt"),"PASS identical winning board and losing result");}return;
            }
            if(p.turn()==p.color()&&revision!=p.revision()&&mc.gui.screen() instanceof GomokuScreen s){int cell=0;while(p.board()[cell]!=0)cell+=2;revision=p.revision();clickCell(s,cell);}
        }
        if(stage.equals("rematch")&&p.id()!=0&&p.outcome()!=0&&mc.gui.screen() instanceof GomokuScreen s){
            if(Files.exists(output().resolve("winning-board.txt"))&&!Files.exists(output().resolve("B-win.txt"))){require(Files.readString(output().resolve("winning-board.txt")).equals(Arrays.toString(p.board())),"different boards");Files.writeString(output().resolve("B-win.txt"),"PASS identical boards");}
            if(frames%10==0)click(s,490,306);
        }
        if(stage.equals("disconnect-now")){finish(mc,Boolean.getBoolean("goosetools.boardRegressionOnly") ? "PASS peer: Xiangqi/chess chat acceptance, legal GUI moves, matching boards and native spectator gamepad" : "PASS peer received invitations, accepted through GUI, alternated real moves, synchronized winning board and consented to rematch");}
    }
    private void next(){step++;frames=0;since=System.nanoTime();}
    private static void setStage(String value)throws Exception{Files.writeString(output().resolve("stage.txt"),value);}
    private static void control(int op){ClientPlayNetworking.send(new GomokuRegressionSupport.Control(op));}
    private static int count(int[] board){int n=0;for(int x:board)if(x!=0)n++;return n;}
    private static void key(){Minecraft mc=Minecraft.getInstance();if(mc.gui.screen() instanceof GomokuScreen s)s.keyPressed(new KeyEvent(InputConstants.KEY_F9,0,0));else KeyMapping.click(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_F9));}
    private static void clickChess(GomokuScreen s,BoardPackets.Position p,int move){
        int w=p.kind()==1?9:8;int size=p.board().length;
        for(int cell:new int[]{ChessPosition.from(move),ChessPosition.to(move)}){int i=p.color()==2?size-1-cell:cell;click(s,p.kind()==1?60+i%w*32:48+i%w*40,p.kind()==1?88+i/w*32:92+i/w*40);}
    }
    private static void clickCell(GomokuScreen s,int cell){click(s,GomokuScreen.GRID_X+cell%15*20,GomokuScreen.GRID_Y+cell/15*20);}
    private static void click(GomokuScreen s,double x,double y){var t=s.transform();s.mouseClicked(new MouseButtonEvent(t.x()+x*t.scale(),t.y()+y*t.scale(),new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),false);}
    private static void clickEntry(Minecraft mc){var screen=mc.gui.screen();var event=new MouseButtonEvent(screen.width-60,18,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(event,false);screen.mouseReleased(event);}
    private void capture(Minecraft mc,String name){if(captures.add(name))pendingCapture=name;}
    private void saveCapture(Minecraft mc,String name){Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(output().resolve(name+".png"));}catch(Exception e){mc.execute(()->finish(mc,"FAIL screenshot "+e));}});}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private void finish(Minecraft mc,String message){if(done)return;done=true;try{Files.writeString(output().resolve(role+"-result.txt"),message);}catch(Exception ignored){}mc.stop();}
}
