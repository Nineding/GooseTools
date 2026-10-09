package com.goosethings.tools.client.task;

import com.goosethings.tools.client.input.ProtectedInputScreen;
import com.goosethings.tools.task.*;
import com.goosethings.tools.task.profession.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Four instrument panels in one protected shell; all acceptance is server-owned. */
public final class ProfessionScreen extends Screen implements ProtectedInputScreen {
    private static final int INK=0xFFEAF2F1,MUTED=0xFFA4BAC0,GREEN=0xFF7CDAAE,RED=0xFFEF947E,AMBER=0xFFF3CF7F;
    private static final int[] COLORS={0xFF65BFFF,0xFFEBCB78,0xFFDA9DDE,0xFF7BD6A9};
    private static final Identifier KNOB=Identifier.fromNamespaceAndPath("goosetools","textures/gui/tasks/professions/dial_knob.png"),
            TERMINAL=Identifier.fromNamespaceAndPath("goosetools","textures/gui/tasks/professions/link_terminal.png");
    private final TaskPackets.Open open;
    private final TaskType type;
    private final ProfessionModel model;
    private final EditBox[] fields=new EditBox[3];
    private final List<ChoiceHit> choices=new ArrayList<>();
    private final List<DialHit> dials=new ArrayList<>();
    private final List<DropHit> drops=new ArrayList<>();
    private final List<DragHit> cards=new ArrayList<>();
    private ProfessionSnapshot state;
    private boolean ready,closing,handbook,localError;
    private int sequence,bookPage,selectedDial,dragSlot=-1,dragValue=-1;
    private int heldDial=-1,graphDial=-1;
    private long lastEvent=-1,lastDialAt;
    public ProfessionScreen(TaskPackets.Open open){
        super(Component.translatableWithFallback("task.goosetools."+TaskType.values()[open.task()].id+".title",TaskType.values()[open.task()].fallback));
        this.open=open;type=TaskType.values()[open.task()];model=ProfessionModel.create(type,open.seed());
    }
    public long sessionId(){return open.sessionId();}public TaskType taskType(){return type;}
    public ProfessionSnapshot currentState(){return state;}public ProfessionModel questionModel(){return model;}
    public boolean handbookOpen(){return handbook;}
    @Override protected void init(){
        for(int i=0;i<3;i++)if(fields[i]==null){fields[i]=new EditBox(font,27+i*190,396,174,24,common("number","Calculation"));fields[i].setMaxLength(18);}
        if(!ready){ready=true;lifecycle(TaskSession.READY);}
    }
    public void apply(ProfessionPackets.State next){
        if(next.sessionId()!=sessionId())return;ProfessionSnapshot incoming=next.state();
        if(state==null||incoming.stage()!=state.stage()){
            for(EditBox f:fields){f.setValue("");f.setFocused(false);}if(numberCount(incoming.stage())>0)fields[0].setFocused(true);
            dragSlot=dragValue=heldDial=graphDial=-1;selectedDial=0;
        }
        if(lastEvent>=0&&incoming.event()!=lastEvent){localError=false;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(incoming.stage()>state.stage()?SoundEvents.NOTE_BLOCK_PLING:SoundEvents.UI_BUTTON_CLICK,1));}
        state=incoming;lastEvent=state.event();
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        Transform t=transform();double x=t.localX(mx),y=t.localY(my);choices.clear();dials.clear();drops.clear();cards.clear();
        g.fill(0,0,width,height,0xDB0B161C);g.pose().pushMatrix();g.pose().translate((float)t.x,(float)t.y);g.pose().scale((float)t.scale,(float)t.scale);
        panel(g,0,0,700,500,0xFF253942);panel(g,8,8,684,40,0xFF12262F);
        g.text(font,getTitle(),22,16,INK,false);g.text(font,common("subtitle","Professional simulation · command trial"),22,32,MUTED,false);
        button(g,553,15,94,25,common("handbook","Handbook"),x,y,false);button(g,657,15,26,25,Component.literal("×"),x,y,false);
        for(int i=0;i<4;i++){int px=18+i*167;panel(g,px,57,163,25,state!=null&&state.stage()==i?0xFF356D64:0xFF172C35);
            center(g,domain("stage"+i,stages()[i]),px+81,65,INK);}
        panel(g,18,92,664,267,0xFF132730);
        if(state==null)center(g,common("waiting","Waiting for the server…"),350,220,INK);
        else if(state.stage()==4){
            center(g,common("complete","All four stages passed"),350,157,GREEN);
            center(g,common("result","Time %s s · Errors %s",n(state.elapsed()/1000.,1),state.errors()),350,194,INK);
            button(g,230,239,240,31,common("retry","New question"),x,y,false);button(g,230,289,240,31,common("exit","Close"),x,y,false);
        }else{
            switch(type){case TELECOM->telecom(g,x,y);case NUCLEAR->nuclear(g,x,y);case FOODSAFETY->food(g,x,y);case CIVIL->civil(g,x,y);default->{}}
            int count=numberCount(state.stage());
            if(count>0){for(int i=0;i<count;i++){g.text(font,domain("field"+state.stage()+"_"+i,fieldLabels()[state.stage()][i]),27+i*190,380,MUTED,false);fields[i].extractRenderState(g,(int)x,(int)y,dt);}}
            else wrapped(g,common("controls","Select cards or a dial. Arrows adjust; Shift uses larger steps. Enter checks."),28,383,540,MUTED);
            button(g,600,391,76,29,common(type==TaskType.NUCLEAR&&state.stage()==2?"test":"check",type==TaskType.NUCLEAR&&state.stage()==2?"Test":"Check"),x,y,false);
        }
        panel(g,18,433,664,49,0xFF0E212A);
        if(state!=null){ProfessionFeedback f=ProfessionFeedback.values()[state.feedback()];
            wrapped(g,localError?common("numeric","Enter a finite decimal number in each required field."):common("feedback."+f.key(),f.fallback),28,442,640,localError||f!=ProfessionFeedback.READY&&f!=ProfessionFeedback.TESTING&&f!=ProfessionFeedback.SUCCESS?AMBER:MUTED);}
        if(dragSlot>=0||dragValue>=0){panel(g,(int)x-45,(int)y-10,90,22,0xFF446E68);center(g,common("dragging","Place card"),(int)x,(int)y-3,INK);}
        if(handbook)book(g,x,y);
        g.pose().popMatrix();
    }
    private void telecom(GuiGraphicsExtractor g,double x,double y){TelecomModel m=(TelecomModel)model;int[] p=state.choices();double[] d=state.dials();switch(state.stage()){
        case 0->{
            header(g,domain("optical_data","Fiber %s km × 0.25 dB/km; 2 connectors × 0.5 dB; 4 splices × 0.1 dB",n(m.lengthKm,0)));
            rack(g,50,156);rack(g,267,156);line(g,96,190,267,190,COLORS[0]);g.blit(TERMINAL,150,163,221,217,0,1,0,1);
            center(g,domain("fiber_loss","Total passive loss: %s dB",n(m.lossDb,2)),190,244,INK);
            wrapped(g,domain("rx_limits","Rx sensitivity −26 dBm; reserve 3 dB; maximum −13 dBm. Power differences use dB."),44,282,277,MUTED);
            choiceRow(g,0,350,151,315,domain("tx","Transmitter / dBm"),new String[]{"−1","2","5"},x,y);
            choiceRow(g,1,350,228,315,domain("attenuator","Attenuator / dB"),new String[]{"0","3","6","9"},x,y);
        }
        case 1->{
            wrapped(g,domain("capacity_data","Bandwidth %s MHz; SNR %s dB; demand %s Mbit/s; practical cap ≤ 0.8 C. Roll-off 0.25; overhead 10%.",n(m.bandwidthMHz,0),n(m.snrDb,0),n(m.demandMbps,1)),43,113,280,INK);
            wrapped(g,domain("mcs_table","Given device thresholds: QPSK ≥ 6 dB; 16-QAM ≥ 14 dB. C = B log₂(1+SNRlinear)."),43,181,280,MUTED);
            bar(g,47,251,268,Math.min(1,m.netMbps(p)/m.demandMbps),GREEN);
            center(g,domain("throughput","Net %s Mbit/s · occupied %s MHz",n(m.netMbps(p),2),n(m.occupiedMHz(p),2)),181,273,INK);
            choiceRow(g,2,350,132,315,domain("modulation","Modulation"),new String[]{"QPSK","16-QAM"},x,y);
            choiceRow(g,3,350,205,315,domain("code_rate","Code rate"),new String[]{"1/2","3/4"},x,y);
            choiceRow(g,4,350,278,315,domain("symbol_rate","Symbol rate / Msym/s"),new String[]{"1","2","3","4"},x,y);
        }
        case 2->{
            panel(g,47,119,267,221,0xFF0C1E29);for(int i=-2;i<=2;i++){line(g,180+i*43,127,180+i*43,332,0xFF284552);line(g,55,229+i*43,306,229+i*43,0xFF284552);}
            double[][] pts=m.constellation(d);for(int i=0;i<4;i++){int tx=180+(i==0||i==3?1:-1)*58,ty=229-(i<2?1:-1)*58;
                g.outline(tx-5,ty-5,11,11,COLORS[i]);int px=Math.clamp(180+(int)(pts[i][0]*82),62,290),py=Math.clamp(229-(int)(pts[i][1]*82),140,320);circle(g,px,py,4,COLORS[i]);g.text(font,Component.literal("ABCD".substring(i,i+1)),px+7,py-3,COLORS[i],false);}
            center(g,domain("evm","EVM %s%% · stable %s / 800 ms",n(m.evm(d)*100,2),state.stable()),180,103,INK);
            dial(g,0,347,124,318,domain("phase","Phase correction / °"),-60,60,1,x,y);
            dial(g,1,347,182,318,domain("gain","Gain multiplier"),.5,1.5,.01,x,y);
            dial(g,2,347,240,318,domain("dc_i","I DC correction"),-.6,.6,.01,x,y);
            dial(g,3,347,298,318,domain("dc_q","Q DC correction"),-.6,.6,.01,x,y);
        }
        case 3->{
            header(g,domain("channels","Channels: 900 / 905 / 910 / 915 MHz. Connected stations need ≥ 10 MHz separation."));
            int[] nx={183,265,265,183,100,100},ny={144,192,280,326,280,192};
            for(int[] e:m.edges())line(g,nx[e[0]],ny[e[0]],nx[e[1]],ny[e[1]],p[5+e[0]]>=0&&p[5+e[1]]>=0&&Math.abs(p[5+e[0]]-p[5+e[1]])<2?RED:0xFF42626B);
            for(int i=0;i<6;i++){circle(g,nx[i],ny[i],18,p[5+i]<0?0xFF35505D:COLORS[p[5+i]]);center(g,Component.literal("S"+(i+1)),nx[i],ny[i]-3,0xFF14262B);drops.add(new DropHit(nx[i]-22,ny[i]-22,44,44,5+i,-1));}
            for(int c=0;c<4;c++){int px=367+c*74;button(g,px,124,68,25,Component.literal((900+5*c)+" MHz"),x,y,false);cards.add(new DragHit(px,124,68,25,-1,c));}
            for(int i=0;i<6;i++)choiceRow(g,5+i,350,160+i*31,315,Component.literal("S"+(i+1)),new String[]{"C1","C2","C3","C4"},x,y,64);
        }
        default->{}
    }}
    private void nuclear(GuiGraphicsExtractor g,double x,double y){NuclearModel m=(NuclearModel)model;int[] p=state.choices();double[] d=state.dials();switch(state.stage()){
        case 0->{
            header(g,domain("neutrons","Given neutron production %s / loss %s; identify heat flow and classify k_eff.",n(m.neutronsProduced,0),n(m.neutronsLost,0)));
            int[] parts=m.components();for(int i=0;i<3;i++){int px=35+i*216;plantPart(g,px+84,168,parts[i]);
                center(g,domain("component"+parts[i],new String[]{"Reactor","Steam generator","Cooling pool"}[parts[i]]),px+99,216,INK);
                choiceRow(g,i,px,239,204,domain("role","Thermal role"),new String[]{"Source","Exchanger","Sink"},x,y);}
            choiceRow(g,3,39,302,310,domain("fluids","Primary / secondary fluids"),new String[]{"Mixed","Separate"},x,y);
            choiceRow(g,4,368,302,291,domain("criticality","Neutron state"),new String[]{"Subcritical","Critical","Supercritical"},x,y);
        }
        case 1->{
            header(g,domain("decay_data","Given P(t) = %s / (1+t/60)^0.2 MW; evaluate t = %s min.",n(m.initialMW,0),n(m.timeMinutes,0)));
            axes(g,47,137,308,154);for(int i=0;i<60;i++)line(g,47+i*5,291-(int)(m.decayMW(i*2)/m.initialMW*130),52+i*5,291-(int)(m.decayMW(i*2+2)/m.initialMW*130),AMBER);
            center(g,domain("curve_axes","t: 0–120 min · residual heat / MW"),201,309,MUTED);
            wrapped(g,domain("cooling_data","cp = %s kJ/(kg·K); ΔT = %s K. Q = ṁ cp ΔT. Require 10% flow reserve; select the smallest suitable pump.",n(m.cp,1),n(m.deltaT,0)),389,150,263,INK);
            String[] flows=Arrays.stream(NuclearModel.PUMP_FLOW).mapToObj(v->n(v,0)+" kg/s").toArray(String[]::new);
            choiceRow(g,5,388,264,267,domain("pump","Pump rating"),flows,x,y);
        }
        case 2->{
            header(g,domain("redundancy","Each branch: 110–125% heat removal; pump rating %s kg/s. Independent buses and sinks.",p[5]<0?"?":n(NuclearModel.PUMP_FLOW[p[5]],0)));
            for(int i=0;i<2;i++){int yy=139+i*99;panel(g,37,yy,365,87,0xFF243B45);g.text(font,Component.literal(i==0?"A":"B"),45,yy+7,COLORS[i],false);
                choiceRow(g,6+i,67,yy+7,97,domain("bus","Bus"),new String[]{"1","2"},x,y);
                choiceRow(g,8+i,183,yy+7,97,domain("sink","Sink"),new String[]{"1","2"},x,y);
                choiceRow(g,10+i,299,yy+7,89,domain("valve","Valve"),new String[]{"Closed","Open"},x,y);
                line(g,69,yy+68,377,yy+68,p[10+i]==1?COLORS[i]:RED);
                dial(g,i,426,yy+10,235,domain("flow"+i,i==0?"Flow A / kg/s":"Flow B / kg/s"),0,600,5,x,y);}
            center(g,domain("test_status","T %s °C · %s / 6000 ms · mode %s",n(state.temperature(),2),state.testElapsed(),state.testElapsed()<2000?"A+B":state.testElapsed()<4000?"B":"A"),350,338,state.temperature()>=80&&state.temperature()<=95?GREEN:RED);
        }
        case 3->{
            header(g,domain("dose_data","Point source: %s µSv/h at 1 m; work ≥ %s min; budget %s µSv.",n(m.rateAt1m,0),m.requiredMinutes,n(m.doseBudget,3)));
            int route=p[12]<0?0:p[12];double[] distances=m.routeDistances(route);int[] frac={50,30,20};
            for(int i=0;i<3;i++){int yy=151+i*55;g.item(new ItemStack(Items.CLOCK),49,yy);bar(g,77,yy+4,210,distances[i]/4,COLORS[i]);g.text(font,domain("dose_segment","Segment %s: %s m · %s%% time",i+1,n(distances[i],1),frac[i]),48,yy+23,INK,false);}
            choiceRow(g,12,350,140,315,domain("route","Work route"),new String[]{"1","2","3"},x,y);
            choiceRow(g,13,350,215,315,domain("shield","Shield transmission"),new String[]{"1.0","0.5","0.2"},x,y);
            dial(g,2,350,285,315,domain("worktime","Total work / min"),1,8,.25,x,y);
        }
        default->{}
    }}
    private void food(GuiGraphicsExtractor g,double x,double y){FoodSafetyModel m=(FoodSafetyModel)model;int[] p=state.choices();double[] d=state.dials();switch(state.stage()){
        case 0->{
            header(g,domain("hazard_data","Given validated heat treatment and final metal detection; cleaning controls chemical/allergen residues."));
            int[] kinds=m.hazardKinds();for(int i=0;i<4;i++){int px=35+i%2*331,yy=128+i/2*78;panel(g,px,yy,309,68,0xFF273E46);
                g.item(new ItemStack(kinds[i]==0?Items.CHICKEN:kinds[i]==1?Items.IRON_NUGGET:kinds[i]==2?Items.MILK_BUCKET:Items.GLASS_BOTTLE),px+8,yy+7);
                g.text(font,domain("hazard"+kinds[i],new String[]{"Raw-material pathogen","Metal fragments after processing","Milk allergen at changeover","Cleaning-agent residue"}[kinds[i]]),px+33,yy+11,INK,false);
                choiceRow(g,i,px+6,yy+32,297,Component.empty(),new String[]{"GHP intake","Heat CCP","Metal CCP","Allergen GHP"},x,y,0);
                cards.add(new DragHit(px,yy,309,29,i,-1));}
            for(int i=0;i<4;i++){int px=35+i*164;button(g,px,314,150,28,domain("control"+i,new String[]{"GHP intake / rinse","CCP heat treatment","CCP metal detection","GHP verified cleaning"}[i]),x,y,false);drops.add(new DropHit(px,314,150,28,-1,i));}
        }
        case 1->{
            header(g,domain("thermal_data","Tref 70 °C; Dref %s min; z %s °C; required %s log reductions; F quality cap 3×target.",n(m.dReference,1),n(m.z,0),m.logReduction));
            axes(g,44,143,298,158);double[] ts=m.coldTemperatures(d),times=m.durations(d);double total=Arrays.stream(times).sum(),at=0;
            for(int i=0;i<6;i++){int xx=44+(int)(at/total*298),end=44+(int)((at+times[i])/total*298),yy=301-(int)((ts[i]-45)*4);
                line(g,xx,yy,end,yy,GREEN);line(g,xx,yy-16,end,yy-16,AMBER);if(i>0)line(g,xx,301-(int)((ts[i-1]-45)*4),xx,yy,GREEN);at+=times[i];}
            center(g,domain("coldpoint","Green: cold point · amber: heater +4 °C"),194,315,MUTED);
            dial(g,0,365,147,295,domain("cold_set","Cold-point plateau / °C"),65,85,.5,x,y);
            dial(g,1,365,220,295,domain("holdtime","Plateau duration / min"),.5,30,.5,x,y);
            wrapped(g,domain("thermal_samples","First four samples: 50, 60, T−6, T−2 °C for 0.5 min each. Last two: T °C for half the hold each. Integrate these rectangles; T ≤ 82 °C."),365,285,295,MUTED);
        }
        case 2->{
            for(int z=0;z<3;z++){int px=35+z*216;panel(g,px,109,204,73,0xFF223C45);center(g,domain("zone"+z,new String[]{"RAW","READY-TO-EAT","ALLERGEN"}[z]),px+102,119,COLORS[z]);drops.add(new DropHit(px,109,204,73,-1,z));}
            int[] kinds=m.toolKinds();for(int i=0;i<4;i++){int px=35+i*164;panel(g,px,206,150,65,0xFF293E47);
                center(g,domain("tool"+kinds[i],new String[]{"Raw chicken","Packed ready food","Milk ingredient","Raw-area knife"}[kinds[i]]),px+75,216,INK);
                choiceRow(g,4+i,px+4,237,142,Component.empty(),new String[]{"Raw","RTE","Allergen"},x,y,0);cards.add(new DragHit(px,206,150,28,4+i,-1));
                if(p[4+i]>=0){int zx=35+p[4+i]*216;g.text(font,Component.literal("#"+(i+1)),zx+17+i*38,151,INK,false);}}
            choiceRow(g,8,35,292,315,domain("cleaning","Changeover method"),new String[]{"Rinse","Validated clean","Heat"},x,y);
            choiceRow(g,9,365,292,315,domain("tools","Tool flow"),new String[]{"Shared","Dedicated","Unverified"},x,y);
            g.text(font,domain("clean_report","Given residue assay: rinse +, validated clean −, heat +; dedicated tools only."),35,344,MUTED,false);
        }
        case 3->{
            header(g,domain("trace_data","Lot %s failed: hold descendants. Irreversible chemical residue: reject. Thermal deviation: validated rework. Other records comply.","ABCD".substring(m.badLot,m.badLot+1)));
            for(int i=0;i<4;i++){int px=35+i*164;panel(g,px,132,150,27,i==m.badLot?0xFF773F3B:0xFF30594E);center(g,Component.literal("ABCD".substring(i,i+1)),px+75,142,INK);}
            int[] masks=m.lotMasks(),deviations=m.deviations();
            // Draw the whole network first so later edges never cross card text or controls.
            for(int i=0;i<6;i++){int px=35+i%3*216,yy=219+i/3*67;
                for(int lot=0;lot<4;lot++)if((masks[i]&(1<<lot))!=0)line(g,110+lot*164,159,px+102,yy,lot==m.badLot?0xFFB36A58:0xFF34545C);}
            for(int i=0;i<6;i++){int px=35+i%3*216,yy=219+i/3*67;
                panel(g,px,yy,204,58,0xFF263E48);StringBuilder lots=new StringBuilder();for(int k=0;k<4;k++)if((masks[i]&(1<<k))!=0)lots.append("ABCD".charAt(k));
                g.text(font,domain("batch","Batch %s · lots %s",i+1,lots.toString()),px+7,yy+6,INK,false);
                if(deviations[i]!=0)g.text(font,domain("deviation"+deviations[i],deviations[i]==1?"Thermal deviation; rework validated":"Chemical residue; irreversible"),px+7,yy+18,AMBER,false);
                choiceRow(g,10+i,px+7,yy+33,194,Component.empty(),new String[]{"Release","Hold","Reject","Rework"},x,y,0);}
        }
        default->{}
    }}
    private void civil(GuiGraphicsExtractor g,double x,double y){CivilModel m=(CivilModel)model;int[] p=state.choices();double[] d=state.dials();switch(state.stage()){
        case 0->{
            header(g,domain("survey_data","Known start and endpoint RL %s m; three equal-length legs; closure error = measured endpoint − known endpoint.",n(m.initialLevel,3)));
            double[] bs=m.backsights(),fs=m.foresights();for(int i=0;i<3;i++){int px=36+i*216;panel(g,px,143,204,135,0xFF253F48);
                line(g,px+26,170,px+26,257,AMBER);for(int j=0;j<8;j++)g.fill(px+26,171+j*10,px+36+(j%2)*6,174+j*10,INK);
                g.item(new ItemStack(Items.SPYGLASS),px+66,178);g.text(font,domain("setup","Setup %s",i+1),px+78,154,INK,false);
                g.text(font,Component.literal("BS "+n(bs[i],3)+" m"),px+70,200,INK,false);g.text(font,Component.literal("FS "+n(fs[i],3)+" m"),px+70,223,INK,false);}
            choiceRow(g,0,36,307,644,domain("correction","Closure correction"),new String[]{"No correction","−error / 3 per leg","+error / 3 per leg"},x,y);
        }
        case 1->{
            header(g,domain("beam_data","Simply supported beam: q %s kN/m, L %s m. Draw end shears and the midspan moment.",n(m.qKNm,0),n(m.lengthM,0)));
            line(g,47,163,327,163,INK);for(int i=0;i<8;i++){int xx=55+i*37;line(g,xx,131,xx,159,AMBER);line(g,xx-4,153,xx,159,AMBER);line(g,xx+4,153,xx,159,AMBER);}triangle(g,47,166);triangle(g,327,166);
            axes(g,47,205,280,51);line(g,47,230-(int)(d[1]/600*23),327,230-(int)(d[2]/600*23),COLORS[0]);
            g.text(font,domain("shear","Shear V / kN"),47,189,MUTED,false);axes(g,47,285,280,48);
            for(int i=0;i<28;i++){double t=i/28.,tt=(i+1)/28.;line(g,47+i*10,332-(int)(4*t*(1-t)*d[3]/1000*46),57+i*10,332-(int)(4*tt*(1-tt)*d[3]/1000*46),GREEN);}
            g.text(font,domain("moment","Moment M / kN·m"),47,269,MUTED,false);
            dial(g,1,369,137,291,domain("left_shear","Left shear / kN"),0,600,.5,x,y);
            dial(g,2,369,211,291,domain("right_shear","Right shear / kN"),-600,0,.5,x,y);
            dial(g,3,369,285,291,domain("mid_moment","Midspan moment / kN·m"),0,1000,.5,x,y);
        }
        case 2->{
            header(g,domain("section_data","E 200 GPa; σ ≤ 160 MPa; δ ≤ L/250 = %s mm; mass ≤ 180 kg/m. q %s kN/m, L %s m.",n(m.deflectionLimitMm(),1),n(m.qKNm,0),n(m.lengthM,0)));
            for(int i=0;i<4;i++){int px=35+i*164;panel(g,px,143,150,143,p[1]==i?0xFF345E55:0xFF263E48);int w=50+i*7;g.fill(px+75-w/2,163,px+75+w/2,171,0xFFB4C8CF);g.fill(px+71,171,px+79,211,0xFFB4C8CF);g.fill(px+75-w/2,211,px+75+w/2,219,0xFFB4C8CF);
                g.text(font,Component.literal("I "+n(CivilModel.INERTIA[i]*1e6,0)+" ×10⁻⁶ m⁴"),px+7,232,INK,false);g.text(font,Component.literal("W "+n(CivilModel.MODULUS[i]*1e6,0)+" ×10⁻⁶ m³"),px+7,247,INK,false);g.text(font,Component.literal(n(CivilModel.MASS[i],0)+" kg/m"),px+7,265,MUTED,false);
                choiceButton(g,1,i,px,296,150,27,domain("section","Section %s",i+1),x,y);}
        }
        case 3->{
            header(g,domain("concrete_data","Effective w/b 0.45; binder %s kg. Reports need strength ≥ 30 MPa and curing ≥ 7 days (given limits).",n(m.binderKg,0)));
            double[] ssd={m.sandSSD,m.stoneSSD},mc={m.sandMoisture,m.stoneMoisture},abs={m.sandAbsorption,m.stoneAbsorption};
            for(int i=0;i<2;i++){int px=36+i*222;panel(g,px,147,207,126,0xFF2B4149);g.item(new ItemStack(i==0?Items.SAND:Items.GRAVEL),px+12,161);
                g.text(font,domain("aggregate"+i,i==0?"Fine aggregate":"Coarse aggregate"),px+40,166,INK,false);
                g.text(font,Component.literal("SSD "+n(ssd[i],0)+" kg"),px+12,196,INK,false);g.text(font,Component.literal("mc "+n(mc[i]*100,1)+"% · a "+n(abs[i]*100,1)+"%"),px+12,218,MUTED,false);}
            panel(g,480,147,180,126,0xFF2B4149);g.item(new ItemStack(Items.CLAY_BALL),496,161);wrapped(g,domain("moisture_basis","mc and a use oven-dry mass. Correct wet aggregate and added water separately; absorbed water is excluded from effective water."),492,190,156,MUTED);
            double[] strength=m.reportStrength(),cure=m.reportCuring();for(int i=0;i<3;i++)choiceButton(g,2,i,36+i*216,302,204,31,domain("report","Report %s: %s MPa / %s d",i+1,n(strength[i],0),n(cure[i],0)),x,y);
        }
        default->{}
    }}
    private void header(GuiGraphicsExtractor g,Component c){wrapped(g,c,32,103,636,MUTED);}
    private void choiceRow(GuiGraphicsExtractor g,int slot,int x,int y,int width,Component label,String[] options,double mx,double my){choiceRow(g,slot,x,y,width,label,options,mx,my,18);}
    private void choiceRow(GuiGraphicsExtractor g,int slot,int x,int y,int width,Component label,String[] options,double mx,double my,int labelSpace){
        if(labelSpace>0)g.text(font,label,x,y,INK,false);int xx=x;
        if(labelSpace==64){g.text(font,label,x,y+8,INK,false);xx+=43;width-=43;labelSpace=0;}
        int w=width/options.length;for(int i=0;i<options.length;i++)choiceButton(g,slot,i,xx+i*w,y+labelSpace,w-4,23,domain("option"+slot+"_"+i,options[i]),mx,my);
    }
    private void choiceButton(GuiGraphicsExtractor g,int slot,int value,int x,int y,int w,int h,Component c,double mx,double my){
        button(g,x,y,w,h,c,mx,my,state.choices()[slot]==value);choices.add(new ChoiceHit(x,y,w,h,slot,value));
    }
    private void dial(GuiGraphicsExtractor g,int slot,int x,int y,int width,Component label,double min,double max,double step,double mx,double my){
        DialHit hit=new DialHit(x,y,width,slot,min,max,step);dials.add(hit);int pos=dials.size()-1;
        g.text(font,label,x,y,selectedDial==pos?AMBER:INK,false);g.blit(KNOB,x+width/2-35,y+11,x+width/2-11,y+35,0,1,0,1);center(g,Component.literal(n(state.dials()[slot],step>=1?0:2)),x+width/2+12,y+19,INK);
        button(g,x,y+17,26,23,Component.literal("−"),mx,my,false);button(g,x+width-26,y+17,26,23,Component.literal("+"),mx,my,false);
        bar(g,x+34,y+32,width-68,(state.dials()[slot]-min)/(max-min),GREEN);
    }
    private void book(GuiGraphicsExtractor g,double mx,double my){
        g.fill(0,49,700,500,0xEE10232B);panel(g,34,92,632,326,0xFF29434B);center(g,common("handbook","Handbook"),350,110,AMBER);
        wrapped(g,domain("book"+bookPage,books()[bookPage]),54,144,592,INK);
        center(g,Component.literal((bookPage+1)+" / 4"),350,382,MUTED);
        button(g,53,433,56,29,Component.literal("←"),mx,my,false);button(g,591,433,56,29,Component.literal("→"),mx,my,false);
        button(g,225,433,250,29,common("back","Back to panel"),mx,my,false);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){Transform t=transform();double x=t.localX(e.x()),y=t.localY(e.y());
        if(e.button()!=InputConstants.MOUSE_BUTTON_LEFT)return true;
        if(inside(x,y,657,15,26,25)){onClose();return true;}
        if(handbook){if(inside(x,y,225,433,250,29))handbook=false;else if(inside(x,y,53,433,56,29))bookPage=(bookPage+3)%4;else if(inside(x,y,591,433,56,29))bookPage=(bookPage+1)%4;return true;}
        if(inside(x,y,553,15,94,25)){handbook=true;bookPage=state==null?0:Math.min(3,state.stage());return true;}
        if(state==null)return true;
        if(state.stage()==4){if(inside(x,y,230,239,240,31))lifecycle(TaskSession.REPLAY);else if(inside(x,y,230,289,240,31))onClose();return true;}
        int count=numberCount(state.stage());for(int i=0;i<3;i++){boolean focused=i<count&&inside(x,y,27+i*190,396,174,24);fields[i].setFocused(focused);if(focused)fields[i].mouseClicked(new MouseButtonEvent(x,y,e.buttonInfo()),doubleClick);}
        if(inside(x,y,600,391,76,29)){check();return true;}
        for(ChoiceHit h:choices)if(h.contains(x,y)){send(ProfessionSession.CHOICE,h.slot,h.value,0,0);return true;}
        for(int i=0;i<dials.size();i++){DialHit h=dials.get(i);if(inside(x,y,h.x,h.y,h.width,45)){selectedDial=i;
            if(inside(x,y,h.x,h.y+17,26,23))adjust(-1,false);else if(inside(x,y,h.x+h.width-26,h.y+17,26,23))adjust(1,false);
            else if(inside(x,y,h.x+34,h.y+27,h.width-68,15)){heldDial=i;moveDial(x,false);}return true;}}
        if(type==TaskType.CIVIL&&state.stage()==1){double[] values=state.dials();
            if(Math.hypot(x-47,y-(230-values[1]/600*23))<12)graphDial=1;
            else if(Math.hypot(x-327,y-(230-values[2]/600*23))<12)graphDial=2;
            else if(Math.hypot(x-187,y-(332-values[3]/1000*46))<12)graphDial=3;
            if(graphDial>=0)return true;}
        for(DragHit h:cards)if(inside(x,y,h.x,h.y,h.w,h.h)){dragSlot=h.slot;dragValue=h.value;return true;}
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){
        if(heldDial>=0){Transform t=transform();moveDial(t.localX(e.x()),true);heldDial=-1;}
        if(graphDial>=0){Transform t=transform();moveGraph(t.localY(e.y()),true);graphDial=-1;}
        if(state!=null&&!handbook&&(dragSlot>=0||dragValue>=0)){Transform t=transform();double x=t.localX(e.x()),y=t.localY(e.y());
            for(DropHit h:drops)if(inside(x,y,h.x,h.y,h.w,h.h)){int slot=h.slot>=0?h.slot:dragSlot,value=h.value>=0?h.value:dragValue;if(slot>=0&&value>=0)send(ProfessionSession.CHOICE,slot,value,0,0);break;}}
        dragSlot=dragValue=-1;return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){Transform t=transform();
        if(heldDial>=0&&!handbook)moveDial(t.localX(e.x()),false);
        if(graphDial>=0&&!handbook)moveGraph(t.localY(e.y()),false);return true;}
    @Override public boolean charTyped(CharacterEvent e){if(state!=null&&state.stage()<4&&!handbook&&(Character.isDigit(e.codepoint())||e.codepoint()=='.'||e.codepoint()=='-'))for(EditBox field:fields)if(field.isFocused()){field.charTyped(e);break;}return true;}
    @Override public boolean keyPressed(KeyEvent e){
        if(e.key()==InputConstants.KEY_ESCAPE){if(handbook)handbook=false;else onClose();return true;}
        if(handbook){if(e.key()==InputConstants.KEY_LEFT)bookPage=(bookPage+3)%4;if(e.key()==InputConstants.KEY_RIGHT)bookPage=(bookPage+1)%4;if(e.key()==InputConstants.KEY_RETURN)handbook=false;return true;}
        if(state==null||state.stage()==4)return true;
        int count=numberCount(state.stage());if(e.key()==InputConstants.KEY_RETURN){check();return true;}
        if(e.key()==InputConstants.KEY_TAB){int focus=-1;for(int i=0;i<count;i++)if(fields[i].isFocused())focus=i;
            for(EditBox f:fields)f.setFocused(false);if(focus+1<count)fields[focus+1].setFocused(true);else if(!dials.isEmpty())selectedDial=(selectedDial+1)%dials.size();else if(count>0)fields[0].setFocused(true);return true;}
        if(e.key()==InputConstants.KEY_LEFT||e.key()==InputConstants.KEY_RIGHT){boolean typing=Arrays.stream(fields).anyMatch(EditBox::isFocused);
            if(!typing&&!dials.isEmpty()){adjust(e.key()==InputConstants.KEY_RIGHT?1:-1,e.hasShiftDown());return true;}}
        for(EditBox f:fields)if(f.isFocused()){f.keyPressed(e);break;}return true;
    }
    @Override public boolean keyReleased(KeyEvent e){return true;}
    private void adjust(int direction,boolean coarse){
        if(dials.isEmpty()||now()-lastDialAt<40)return;lastDialAt=now();DialHit h=dials.get(Math.floorMod(selectedDial,dials.size()));
        double value=Math.clamp(Math.rint((state.dials()[h.slot]+direction*h.step*(coarse?10:1))*10000)/10000,h.min,h.max);send(ProfessionSession.DIAL,h.slot,value,0,0);
    }
    private void moveDial(double x,boolean finalPoint){if(heldDial<0||heldDial>=dials.size()||!finalPoint&&now()-lastDialAt<50)return;
        lastDialAt=now();DialHit h=dials.get(heldDial);double raw=h.min+(x-h.x-34)/(h.width-68)*(h.max-h.min);
        double value=Math.clamp(Math.rint(raw/h.step)*h.step,h.min,h.max);send(ProfessionSession.DIAL,h.slot,value,0,0);}
    private void moveGraph(double y,boolean finalPoint){if(graphDial<0||!finalPoint&&now()-lastDialAt<50)return;lastDialAt=now();
        double raw=graphDial==3?(332-y)/46*1000:(230-y)/23*600;
        double value=Math.clamp(Math.rint(raw*2)/2,graphDial==2?-600:0,graphDial==3?1000:graphDial==2?0:600);send(ProfessionSession.DIAL,graphDial,value,0,0);}
    private void check(){double[] input=new double[3];try{for(int i=0;i<numberCount(state.stage());i++){input[i]=Double.parseDouble(fields[i].getValue());if(!Double.isFinite(input[i])||Math.abs(input[i])>1_000_000)throw new NumberFormatException();}
            send(ProfessionSession.CHECK,0,input[0],input[1],input[2]);}catch(NumberFormatException e){localError=true;}}
    private void send(int action,int slot,double a,double b,double c){if(state!=null&&state.stage()<4&&ClientPlayNetworking.canSend(ProfessionPackets.Input.TYPE))ClientPlayNetworking.send(new ProfessionPackets.Input(sessionId(),sequence++,state.stage(),action,slot,a,b,c));}
    private void lifecycle(int action){if(ClientPlayNetworking.canSend(TaskPackets.Action.TYPE))ClientPlayNetworking.send(new TaskPackets.Action(sessionId(),sequence++,action,-1,0,0,0));}
    @Override public void onClose(){cancel();minecraft.setScreenAndShow(null);}@Override public void removed(){cancel();}
    private void cancel(){if(!closing){closing=true;lifecycle(TaskSession.CANCEL);}}
    public void closeFromServer(){closing=true;if(minecraft!=null&&minecraft.gui.screen()==this)minecraft.setScreenAndShow(null);}
    private int numberCount(int stage){return stage>=4?0:fieldLabels()[stage].length;}
    private String[][] fieldLabels(){return switch(type){
        case TELECOM -> new String[][]{{"Received power / dBm","Receiver margin / dB"},{"Shannon C / Mbit/s","Selected net rate / Mbit/s"},{},{}};
        case NUCLEAR -> new String[][]{{"k_eff"},{"Decay heat / MW","Flow without reserve / kg/s"},{},{"Integrated dose / µSv"}};
        case FOODSAFETY -> new String[][]{{},{"Integrated Fref / min","Required Fref / min"},{},{}};
        case CIVIL -> new String[][]{{"First HI / m","Corrected first RL / m","Closure error / m"},{"Support reaction / kN","Maximum moment / kN·m"},{"Stress / MPa","Deflection / mm"},{"Wet fine aggregate / kg","Wet coarse aggregate / kg","Added water / kg"}};
        default -> throw new IllegalStateException();};}
    private String[] stages(){return switch(type){
        case TELECOM->new String[]{"1 · Optical link","2 · Modulation","3 · IQ calibration","4 · Spectrum"};
        case NUCLEAR->new String[]{"1 · Plant diagram","2 · Decay heat","3 · Redundancy","4 · Radiation"};
        case FOODSAFETY->new String[]{"1 · Hazard analysis","2 · Heat process","3 · Separation","4 · Traceability"};
        case CIVIL->new String[]{"1 · Survey","2 · Beam forces","3 · Section check","4 · Concrete"};
        default->throw new IllegalStateException();};}
    private String[] books(){return switch(type){
        case TELECOM->new String[]{
            "Optical budget: Pr[dBm] = Pt[dBm] − fiber loss[dB] − connector/splice loss[dB] − attenuator[dB]. Fiber loss = length[km] × loss[dB/km]. Margin = Pr − sensitivity, in dB. Meet sensitivity + 3 dB, and stay at or below −13 dBm to avoid receiver overload. Choose the transmitter and attenuation, then calculate the selected received power and margin. Tolerance: 1% or 0.05 dB, whichever is larger.",
            "Convert SNRdB to linear power ratio: 10^(SNRdB/10). Shannon C = B log2(1+SNRlinear) is the theoretical ceiling. Net rate = Rs × log2(M) × code rate × 0.9. Occupied bandwidth = 1.25 Rs for this given roll-off. The game equipment table requires QPSK ≥6 dB and 16-QAM ≥14 dB. Use the displayed net demand, ranging from 3 to 9 Mbit/s; occupied spectrum ≤5 MHz; net rate ≤0.8 C. MHz and Msym/s yield Mbit/s here.",
            "Colored reference pilots A–D identify each QPSK symbol. Calibrate phase, gain multiplier and I/Q DC corrections to align the solid dots with their reference squares. EVM = RMS(error vectors) / RMS(reference vectors); this constellation has unit reference RMS. The device applies phase and multiplicative gain before adding DC offsets. Select a dial, then use arrows; Shift adjusts ten steps. Keep EVM ≤3.5% for 800 ms, then check. A handbook overlay keeps the same input protection.",
            "Allocate four channel centers: 900, 905, 910 and 915 MHz. Every drawn edge means its two stations need ≥10 MHz separation, i.e. channel-index difference ≥2. Every station needs a channel. Drag a channel card onto a station or use its C1–C4 buttons. Red edges indicate current conflicts. Channel and modulation thresholds are the stated game equipment data, not universal network standards."};
        case NUCLEAR->new String[]{
            "A PWR transfers heat from the primary circuit through a steam generator to a separate secondary circuit; the fluids do not mix. Identify the reactor heat source, heat exchanger and final cooling pool. For the supplied simplified neutron balance, keff = produced / lost; below 1 is subcritical, equal to 1 critical, above 1 supercritical. The given state is already shut down. A subcritical state still needs removal of decay heat.",
            "Use the given illustrative curve P(t)=P0/(1+t/60)^0.2 MW at the requested time in minutes; it is question data, not a general reactor decay law. Heat removal Q[kW] = flow[kg/s] × cp[kJ/(kg·K)] × ΔT[K]. Multiply MW by 1000 before solving flow. Calculate heat and unreserved flow, then select the smallest rating at least 1.10 times that flow. Calculation tolerance: 1%.",
            "Configure A and B with different power buses and different heat sinks; open both valves. Each branch independently removes 110–125% of the specified heat without exceeding the selected pump rating. T changes according to Cth dT/dt = Pdecay − Qcool, with Cth=10,000 kJ/K. The 6-second test runs both branches, then loses A, then loses B, for two seconds each. Keep 80–95 °C. Changing configuration resets the test. These are explicit teaching-model parameters.",
            "External point-source approximation: dose rate at distance r equals the given 1 m rate divided by r²; multiply by the shield transmission. Work occupies three segments with 50%, 30%, 20% of total time. Integrated dose[µSv] = sum(rate[µSv/h] × segment minutes / 60). Complete at least the required work time while staying within the question dose budget. Distance law is specific to this point-source model; do not convert detector counts directly to dose."};
        case FOODSAFETY->new String[]{
            "Use the supplied process and validated control descriptions. Raw pathogens go to the heat CCP; post-process metal goes to final metal-detection CCP; milk-allergen changeover goes to verified-cleaning GHP; cleaning chemical residue goes to intake/rinse GHP. GHP establishes general hygiene conditions. CCP classification depends on the specified process and validated later controls, not merely whether a hazard exists. Drag a hazard card to a control bin or use its buttons.",
            "Dref is the time for one decimal reduction at Tref for this given product and organism. Required Fref = number of log reductions × Dref. z is the temperature rise causing a tenfold change in D. For each shown piecewise-constant product cold-point sample: F increment = minutes × 10^((T−Tref)/z); sum all six. First four durations are 0.5 min; the final two split the chosen hold. Heater temperature is cold point +4 °C and cannot replace it. Pass F ≥ target, F ≤3 target and cold plateau ≤82 °C.",
            "Keep raw chicken and raw-area knives in RAW, packed ready food in READY-TO-EAT, and milk ingredients in ALLERGEN. Select validated cleaning and dedicated tool flow. This question's cleaning validation report establishes acceptable allergen residue only for the validated method; rinsing or heating alone does not establish that result. Dedicated routes prevent recontamination after heat processing. Drag the cards to the zones or click the zone buttons.",
            "The failed raw lot affects every connected production batch, including mixtures. Apply this question's disposition plan in priority order: irreversible chemical residue means Reject; descendants of the failed raw lot mean Hold; a thermal deviation without either issue allows Rework only because a validated rework procedure is supplied; all other complete records allow Release. Inspect all links and process flags. A thermal rework option does not resolve chemical residues or unknown raw-lot failures. D/z data and limits belong to this question."};
        case CIVIL->new String[]{
            "Height of instrument HI = known RL + backsight BS. New RL = HI − foresight FS. Continue through all three equal-length legs. Closure error = observed final RL − known final RL. Apply −error/3 on each leg; the first adjusted RL is its raw RL minus error/3. Input first HI, corrected first RL and signed closure error in metres. Elevation tolerance 0.001 m; closure tolerance 0.0005 m. Choose the negative equal-length correction scheme.",
            "For this simply supported beam with uniform load q[kN/m] and span L[m], each support reaction R=qL/2 kN and maximum midspan moment M=qL²/8 kN·m. Shear starts at +R and ends at −R, crossing zero at midspan; moment is a positive parabola vanishing at both supports. Set the three diagram dials, then input R and M. Select a dial with the mouse; arrows change 0.5 units and Shift changes 5. Tolerance: 1% or 0.1 units.",
            "This question uses a uniform elastic Euler–Bernoulli beam. Convert kN·m to N·m before σ=M/W; divide Pa by 10^6 for MPa. Deflection δ=5qL^4/(384EI), with q in N/m, L in m, E in Pa and I in m^4; multiply metres by 1000 for mm. Choose a listed section; require stress ≤160 MPa, deflection ≤L/250, mass ≤180 kg/m. E/I/W and acceptance limits are supplied game data; checking strength alone is insufficient.",
            "Moisture mc and absorption a are fractions of oven-dry (OD) aggregate mass. MOD=MSSD/(1+a); wet batching mass=MOD(1+mc). Free water=MOD(mc−a)=Mwet−MSSD. Added water=binder×0.45−sum(free water). Below SSD, free water is negative and extra water is needed for absorption. Input wet fine/coarse masses and added water. Then select a report meeting the given ≥30 MPa strength and ≥7 d curing. A water/binder ratio alone does not prove concrete strength or structural acceptance."};
        default->throw new IllegalStateException();};}
    private Component common(String key,String fallback,Object... args){return Component.translatableWithFallback("task.goosetools.profession."+key,fallback,args);}
    private Component domain(String key,String fallback,Object... args){return Component.translatableWithFallback("task.goosetools."+type.id+"."+key,fallback,args);}
    private void center(GuiGraphicsExtractor g,Component c,int x,int y,int color){g.text(font,c,x-font.width(c)/2,y,color,false);}
    private void wrapped(GuiGraphicsExtractor g,Component c,int x,int y,int width,int color){for(var line:font.split(c,width)){g.text(font,line,x,y,color,false);y+=12;}}
    private void button(GuiGraphicsExtractor g,int x,int y,int w,int h,Component c,double mx,double my,boolean selected){panel(g,x,y,w,h,selected?0xFF3B7667:inside(mx,my,x,y,w,h)?0xFF456674:0xFF2C4752);center(g,c,x+w/2,y+(h-8)/2,INK);}
    private static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h,int color){g.fill(x,y,x+w,y+h,color);g.outline(x,y,w,h,0xFF48646E);}
    private static void bar(GuiGraphicsExtractor g,int x,int y,int w,double ratio,int color){g.fill(x,y,x+w,y+7,0xFF304B56);g.fill(x,y,x+(int)(w*Math.clamp(ratio,0,1)),y+7,color);}
    private static void axes(GuiGraphicsExtractor g,int x,int y,int w,int h){for(int i=0;i<5;i++)line(g,x,y+i*h/4,x+w,y+i*h/4,0xFF284653);line(g,x,y,x,y+h,MUTED);line(g,x,y+h,x+w,y+h,MUTED);}
    private static void line(GuiGraphicsExtractor g,int x,int y,int xx,int yy,int color){int steps=Math.max(Math.abs(x-xx),Math.abs(y-yy));for(int i=0;i<=steps;i++){double f=steps==0?0:i/(double)steps;int dx=(int)(x+(xx-x)*f),dy=(int)(y+(yy-y)*f);g.fill(dx,dy,dx+2,dy+2,color);}}
    private static void circle(GuiGraphicsExtractor g,int x,int y,int r,int color){for(int row=-r;row<=r;row++){int half=(int)Math.sqrt(r*r-row*row);g.fill(x-half,y+row,x+half+1,y+row+1,color);}}
    private static void rack(GuiGraphicsExtractor g,int x,int y){panel(g,x,y,49,71,0xFF354B55);for(int i=0;i<4;i++){g.fill(x+5,y+7+i*15,x+43,y+17+i*15,0xFF182C35);g.fill(x+34,y+10+i*15,x+39,y+14+i*15,GREEN);}}
    private static void plantPart(GuiGraphicsExtractor g,int x,int y,int type){if(type==0){circle(g,x,y,26,0xFF687C83);panel(g,x-19,y-22,38,44,0xFF34515C);for(int i=-2;i<=2;i++)g.fill(x+i*6,y-15,x+i*6+3,y+16,AMBER);}else if(type==1){panel(g,x-28,y-29,56,58,0xFF3B5661);for(int i=0;i<5;i++)line(g,x-20,y-19+i*9,x+20,y-19+i*9,i%2==0?COLORS[0]:AMBER);}else{panel(g,x-41,y-13,82,37,0xFF355B6A);g.fill(x-36,y-5,x+37,y+19,COLORS[0]);}}
    private static void triangle(GuiGraphicsExtractor g,int x,int y){for(int i=0;i<10;i++)g.fill(x-i,y+i,x+i+1,y+i+1,MUTED);}
    private static boolean inside(double x,double y,int xx,int yy,int w,int h){return x>=xx&&x<xx+w&&y>=yy&&y<yy+h;}
    private static String n(double value,int decimals){return String.format(Locale.ROOT,"%."+decimals+"f",value);}
    private static long now(){return System.nanoTime()/1_000_000;}
    public Transform transform(){double s=Math.max(.1,Math.min(1.6,Math.min((width-20.)/700,(height-20.)/500)));return new Transform((width-700*s)/2,(height-500*s)/2,s);}
    public record Transform(double x,double y,double scale){public double localX(double v){return(v-x)/scale;}public double localY(double v){return(v-y)/scale;}}
    private record ChoiceHit(int x,int y,int w,int h,int slot,int value){boolean contains(double px,double py){return inside(px,py,x,y,w,h);}}
    private record DialHit(int x,int y,int width,int slot,double min,double max,double step){}
    private record DragHit(int x,int y,int w,int h,int slot,int value){}
    private record DropHit(int x,int y,int w,int h,int slot,int value){}
}
