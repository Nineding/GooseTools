package com.goosethings.tools.client.task;

import com.goosethings.tools.client.input.ProtectedInputScreen;
import com.goosethings.tools.task.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import java.util.Locale;

/** An instrument panel; its handbook remains inside the same protected screen. */
public final class PowerStationScreen extends Screen implements ProtectedInputScreen {
    private static final int INK=0xFFE5EFEA,MUTED=0xFF9DADB2,GREEN=0xFF73D1A0,RED=0xFFEC927D,AMBER=0xFFF0CB78;
    private final TaskPackets.Open open;
    private PowerStationSnapshot state;
    private EditBox current,apparent;
    private boolean ready,closing,handbook,localError;
    private int sequence,capacity,selectedKnob,bookPage,dragging=-1;
    private long receivedAt,lastEvent=-1;
    public PowerStationScreen(TaskPackets.Open open){super(Component.translatableWithFallback("task.goosetools.powerstation.title","Restore power station"));this.open=open;}
    public long sessionId(){return open.sessionId();}
    public PowerStationSnapshot currentState(){return state;}
    public boolean handbookOpen(){return handbook;}
    @Override protected void init(){
        if(current==null){current=new EditBox(font,280,159,180,24,text("current","Line current / A"));current.setMaxLength(12);
            apparent=new EditBox(font,280,210,180,24,text("apparent","Apparent power / kVA"));apparent.setMaxLength(12);current.setFocused(true);}
        if(!ready){ready=true;lifecycle(TaskSession.READY);}
    }
    public void apply(PowerStationPackets.State next){if(next.sessionId()!=open.sessionId())return;
        if(state!=null&&next.state().stage()!=state.stage()){current.setFocused(false);apparent.setFocused(false);dragging=-1;}
        if(lastEvent>=0&&next.state().event()!=lastEvent){localError=false;minecraft.getSoundManager().play(SimpleSoundInstance.forUI(next.state().feedback()==PowerStationSession.READY||next.state().stage()==4?SoundEvents.NOTE_BLOCK_PLING:SoundEvents.UI_BUTTON_CLICK,1));}
        state=next.state();lastEvent=state.event();receivedAt=now();
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float dt){
        Transform t=transform();double mx=t.localX(mouseX),my=t.localY(mouseY);
        g.fill(0,0,width,height,0xDC10181C);g.pose().pushMatrix();g.pose().translate((float)t.x,(float)t.y);g.pose().scale((float)t.scale,(float)t.scale);
        panel(g,0,0,600,430,0xFF26353D);g.fill(8,8,592,47,0xFF15262D);g.text(font,getTitle(),18,15,INK,false);
        g.text(font,text("simulation","Station simulation · 400 / 690 V"),18,31,MUTED,false);
        button(g,470,13,85,24,text("handbook","Handbook"),mx,my,false);button(g,565,13,24,24,Component.literal("×"),mx,my,false);
        for(int i=0;i<4;i++){int x=18+i*142;panel(g,x,56,138,23,state!=null&&state.stage()==i?0xFF356A60:0xFF1C2B32);center(g,text("stage"+i,new String[]{"1 · Calculate","2 · Compensate","3 · Synchronize","4 · Distribute"}[i]),x+69,64,INK);}
        panel(g,18,88,564,264,0xFF182830);
        if(state==null)center(g,text("waiting","Waiting for the server…"),300,195,INK);
        else if(state.stage()==4){center(g,text("complete","Supply restored"),300,145,GREEN);center(g,text("result","Time: %s s  Errors: %s",n(state.elapsed()/1000.0,1),state.errors()),300,180,INK);button(g,210,229,180,30,text("retry","New station"),mx,my,false);button(g,210,274,180,30,text("exit","Close"),mx,my,false);}
        else {
            if(state.stage()!=2)nameplate(g);
            switch(state.stage()){case 0->calculate(g,mx,my,dt);case 1->compensate(g,mx,my);case 2->synchronize(g,mx,my);case 3->distribute(g,mx,my);default->{}}
            panel(g,18,363,564,49,0xFF12232A);
            Component f=localError?text("numeric","Enter finite positive numbers in A and kVA."):feedback();int y=372;
            for(var line:font.split(f,540)){g.text(font,line,30,y,state.feedback()==0||state.feedback()>=13?GREEN:RED,false);y+=11;}
            g.text(font,text("footer","Esc: close  ·  %s s  ·  Errors: %s",n(Math.min(3_600_000,state.elapsed()+now()-receivedAt)/1000.0,1),state.errors()),18,417,MUTED,false);
        }
        if(handbook)book(g,mx,my);
        g.pose().popMatrix();
    }
    private void nameplate(GuiGraphicsExtractor g){
        panel(g,28,99,182,239,0xFF21343B);center(g,text("nameplate","Load nameplate"),119,110,AMBER);
        circle(g,119,149,20,0xFF354D52);center(g,Component.literal("G~"),119,145,INK);g.fill(118,169,121,184,GREEN);g.fill(62,184,177,188,GREEN);
        row(g,"voltage","Line voltage",state.voltage(),"V",207);row(g,"power","Active power",state.power(),"kW",235);
        row(g,"pf","Initial cosφ",state.initialPf(),"",263);g.text(font,text("reserve","Capacity reserve: 15%"),40,300,MUTED,false);
        g.text(font,text("model","Balanced sinusoidal load"),40,316,MUTED,false);
    }
    private void row(GuiGraphicsExtractor g,String key,String fallback,double value,String unit,int y){g.text(font,text(key,fallback),40,y,MUTED,false);g.text(font,Component.literal(n(value,key.equals("pf")?2:0)+" "+unit),40,y+12,INK,false);}
    private void calculate(GuiGraphicsExtractor g,double mx,double my,float dt){
        wrapped(g,text("calc_help","Calculate line current and apparent power. Select the smallest capacity meeting the 15% reserve."),226,105,337,MUTED);
        g.text(font,text("current","Line current / A"),280,145,INK,false);g.text(font,text("apparent","Apparent power / kVA"),280,196,INK,false);
        current.extractRenderState(g,(int)mx,(int)my,dt);apparent.extractRenderState(g,(int)mx,(int)my,dt);
        g.text(font,text("capacity","Rated capacity / kVA"),228,247,INK,false);
        for(int i=0;i<5;i++)button(g,228+i*67,263,61,25,Component.literal(Integer.toString(PowerStationSession.CAPACITIES[i])),mx,my,capacity==i);
        button(g,280,306,180,29,text("check","Check settings"),mx,my,false);
    }
    private void compensate(GuiGraphicsExtractor g,double mx,double my){
        wrapped(g,text("comp_help","Switch capacitor banks to reach cosφ 0.950–0.985 while keeping net reactive power inductive."),226,105,337,MUTED);
        for(int i=0;i<4;i++){int x=230+i*82;boolean active=(state.capacitors()&(1<<i))!=0;
            button(g,x,151,74,80,Component.literal(""),mx,my,active);g.fill(x+35,163,x+39,177,INK);g.fill(x+23,176,x+51,179,INK);g.fill(x+23,184,x+51,187,INK);g.fill(x+35,186,x+39,197,INK);
            center(g,Component.literal(n(state.capacitorUnit()*(1<<i),1)+" kvar"),x+37,203,INK);center(g,text(active?"on":"off",active?"ON":"OFF"),x+37,217,active?GREEN:MUTED);}
        double q=state.power()*PowerStationSession.tan(state.initialPf())-state.capacitors()*state.capacitorUnit();double pf=state.power()/Math.hypot(state.power(),q);
        g.text(font,text("net_q","Net Q: %s kvar",n(q,1)),232,246,INK,false);g.text(font,text("net_s","S: %s kVA",n(Math.hypot(state.power(),q),1)),232,265,INK,false);
        g.text(font,Component.literal("cosφ = "+n(pf,3)),416,246,GREEN,false);g.text(font,text(q>=0?"lagging":"leading",q>=0?"Inductive":"Capacitive"),416,265,q>=0?GREEN:RED,false);
        button(g,280,306,180,29,text("check","Check settings"),mx,my,false);
    }
    private double phase(){return PowerStationSession.wrap(state.phase()+(state.stage()==2?360*(state.frequency()-50)*Math.min(100,now()-receivedAt)/1000.0:0));}
    private void synchronize(GuiGraphicsExtractor g,double mx,double my){
        center(g,text("synchroscope","Synchroscope"),119,108,AMBER);circle(g,119,206,64,0xFF3C565C);circle(g,119,206,59,0xFF0E2229);
        for(int i=0;i<12;i++){double a=i*Math.PI/6;int x=(int)(119+Math.sin(a)*52),y=(int)(206-Math.cos(a)*52);g.fill(x-2,y-2,x+2,y+2,INK);}
        double p=Math.toRadians(phase());line(g,119,206,(int)(119+Math.sin(p)*48),(int)(206-Math.cos(p)*48),GREEN);circle(g,119,206,4,AMBER);
        center(g,Component.literal("Δφ = "+n(phase(),1)+"°"),119,283,INK);center(g,text("sync_hold","Window hold: %s / 400 ms",Math.min(400,state.stable())),119,308,MUTED);
        g.fill(232,103,568,155,0xFF0C1F26);
        for(int i=0;i<324;i++){double x=i/324.0*Math.PI*4;int gy=(int)(126-Math.sin(x)*18),sy=(int)(126-Math.sin(x+p)*18*state.generatorVoltage()/state.voltage());g.fill(239+i,gy,241+i,gy+2,0xFF81C6E7);g.fill(239+i,sy,241+i,sy+2,AMBER);}
        g.text(font,text("wave","Blue: bus 50 Hz  ·  Gold: generator"),235,161,MUTED,false);
        for(int k=0;k<2;k++){int x=k==0?302:488;circle(g,x,199,23,selectedKnob==k?0xFF537C72:0xFF354B53);
            center(g,text(k==0?"gen_voltage":"gen_frequency",k==0?"Excitation / V":"Speed / Hz"),x,187,INK);
            center(g,Component.literal(n(k==0?state.generatorVoltage():state.frequency(),k==0?0:2)),x,204,AMBER);
            button(g,x-56,231,45,24,Component.literal("−"),mx,my,false);button(g,x+11,231,45,24,Component.literal("+"),mx,my,false);}
        g.text(font,text("sync_limits","ΔU ≤ 2%   Δf ≤ 0.10 Hz   |Δφ| ≤ 8°"),234,266,INK,false);
        g.text(font,text("knob_keys","Select a dial; ←/→ fine, Shift coarse."),234,283,MUTED,false);
        button(g,236,306,128,29,text("sequence","Phase order: %s",state.phaseSequence()==0?"ABC":"ACB"),mx,my,state.phaseSequence()==0);
        button(g,377,306,184,29,text("breaker","Close breaker"),mx,my,state.stable()>=400);
    }
    private void distribute(GuiGraphicsExtractor g,double mx,double my){
        // Phase trays accept either card drags or the per-card A/B/C buttons.
        g.fill(28,99,210,338,0xFF21343B);center(g,text("phase_loads","Phase currents"),119,111,AMBER);
        double[] currents=state.phaseI();for(int p=0;p<3;p++){int y=132+p*58;panel(g,37,y,164,47,0xFF152C33);
            g.text(font,Component.literal("ABC".substring(p,p+1)),46,y+7,GREEN,false);g.text(font,Component.literal(n(currents[p],1)+" A"),72,y+7,INK,false);
            g.fill(46,y+26,190,y+34,0xFF3C4C52);g.fill(46,y+26,46+(int)Math.clamp(144*currents[p]/state.phaseLimit(),0,144),y+34,currents[p]>state.phaseLimit()?RED:GREEN);}
        wrapped(g,text("load_limits","All loads connected; ≤ %s A per phase, deviation ≤ 10%.",n(state.phaseLimit(),1)),38,311,160,MUTED);
        int[] as=state.assignments();double[] ps=state.loadP(),qs=state.loadQ();
        for(int i=0;i<6;i++){int x=228+i%2*170,y=103+i/2*64;panel(g,x,y,162,58,dragging==i?0xFF35584F:0xFF293E47);
            g.text(font,text("load_card","L%s: %s kW / %s kvar",i+1,n(ps[i],0),n(qs[i],1)),x+6,y+7,INK,false);
            for(int p=0;p<3;p++)button(g,x+7+p*49,y+28,43,22,Component.literal("ABC".substring(p,p+1)),mx,my,as[i]==p);}
        button(g,280,306,180,29,text("energize","Restore supply"),mx,my,false);
        if(dragging>=0){panel(g,(int)mx-35,(int)my-9,70,23,0xFF406B60);center(g,Component.literal("L"+(dragging+1)),(int)mx,(int)my,INK);}
    }
    private Component feedback(){String[] keys={"ready","wrong_current","wrong_power","wrong_capacity","under","over","wrong_sequence","wrong_voltage","wrong_frequency","wrong_phase","missing","overload","unbalanced","success","stable"};
        String[] fallbacks={"Read the instruments and adjust the current stage.","Line current is incorrect. Use line voltage and a 1% tolerance.","Apparent power is incorrect. S = P / cosφ; tolerance 1%.","Choose the smallest capacity ≥ 1.15 × S.","Not enough compensation; cosφ is below 0.950.","Too much compensation: leading Q or cosφ above 0.985.","Phase order differs from the bus. Match ABC.","Voltage difference exceeds the 2% window.","Frequency difference exceeds 0.10 Hz.","Phase must stay within ±8° for 400 ms before closing.","Every load must be connected to a phase.","A phase exceeds its current rating. Redistribute the loads.","Phase currents differ by more than 10% of their mean.","Supply restored.","Checking stable supply for 1.5 seconds…"};return text("feedback."+keys[state.feedback()],fallbacks[state.feedback()]);}
    private void book(GuiGraphicsExtractor g,double mx,double my){
        g.fill(0,48,600,430,0xEC122229);panel(g,32,89,536,268,0xFF243B43);center(g,text("handbook","Handbook"),300,104,AMBER);
        String[] f={"Balanced sine-wave three-phase load: S[kVA] = P[kW] / cosφ. I[A] = 1000 × P / (√3 × U_line × cosφ). Line voltage is √3 times phase voltage. Use at least 15% capacity reserve; select the smallest suitable rating. Input tolerance: 1%.",
            "Reactive power: Q = P × tanφ, with tanφ = √(1−cos²φ) / cosφ. Required capacitor Qc = P × (tanφ_initial − tanφ_target). Switch discrete banks, then compute net Q and cosφ = P / √(P²+Q²). This station requires inductive net Q and cosφ 0.950–0.985.",
            "Match phase sequence, voltage, frequency and phase angle before synchronizing. Phase drift: d(Δφ)/dt = 360 × Δf degrees/second. Adjust frequency to move the pointer; bring it near zero, then match 50 Hz to hold it. Game window: ΔU ≤ 2%, Δf ≤ 0.10 Hz, |Δφ| ≤ 8° for 400 ms. Select a dial and use arrows; Shift makes larger changes.",
            "For each phase, add its active and reactive loads, then subtract one-third of the selected compensation. S_phase = √(P_phase²+Q_phase²); I_phase = 1000 × S_phase / U_phase. Connect all six loads, keep each phase within its rating and within 10% of the mean current. Balanced current requires balancing both P and Q."};
        wrapped(g,text("book"+bookPage,f[bookPage]),51,135,498,INK);button(g,50,319,55,24,Component.literal("←"),mx,my,false);button(g,495,319,55,24,Component.literal("→"),mx,my,false);
        button(g,210,319,180,24,text("back","Back to panel"),mx,my,false);center(g,Component.literal((bookPage+1)+" / 4"),300,294,MUTED);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        Transform t=transform();double x=t.localX(e.x()),y=t.localY(e.y());if(e.button()!=InputConstants.MOUSE_BUTTON_LEFT)return true;
        if(inside(x,y,565,13,24,24)){onClose();return true;}
        if(handbook){if(inside(x,y,210,319,180,24))handbook=false;else if(inside(x,y,50,319,55,24))bookPage=(bookPage+3)%4;else if(inside(x,y,495,319,55,24))bookPage=(bookPage+1)%4;return true;}
        if(inside(x,y,470,13,85,24)){handbook=true;bookPage=state==null?0:Math.min(3,state.stage());return true;}
        if(state==null)return true;
        if(state.stage()==4){if(inside(x,y,210,229,180,30))lifecycle(TaskSession.REPLAY);else if(inside(x,y,210,274,180,30))onClose();return true;}
        if(state.stage()==0){current.setFocused(inside(x,y,280,159,180,24));apparent.setFocused(inside(x,y,280,210,180,24));
            if(current.isFocused())current.mouseClicked(new MouseButtonEvent(x,y,e.buttonInfo()),doubleClick);
            if(apparent.isFocused())apparent.mouseClicked(new MouseButtonEvent(x,y,e.buttonInfo()),doubleClick);
            for(int i=0;i<5;i++)if(inside(x,y,228+i*67,263,61,25))capacity=i;
            if(inside(x,y,280,306,180,29))checkCalculation();
        }else if(state.stage()==1){for(int i=0;i<4;i++)if(inside(x,y,230+i*82,151,74,80))send(PowerStationSession.CAPACITOR,i,0,0);if(inside(x,y,280,306,180,29))send(PowerStationSession.CHECK,0,0,0);}
        else if(state.stage()==2){for(int k=0;k<2;k++){int center=k==0?302:488;if(inside(x,y,center-30,176,60,50))selectedKnob=k;
                if(inside(x,y,center-56,231,45,24)){selectedKnob=k;adjust(-1,false);}if(inside(x,y,center+11,231,45,24)){selectedKnob=k;adjust(1,false);}}
            if(inside(x,y,236,306,128,29))send(PowerStationSession.SEQUENCE,1-state.phaseSequence(),0,0);
            if(inside(x,y,377,306,184,29))send(PowerStationSession.CLOSE_BREAKER,0,0,0);
        }else if(state.stage()==3){for(int i=0;i<6;i++){int cx=228+i%2*170,cy=103+i/2*64;if(inside(x,y,cx,cy,162,24))dragging=i;
                for(int p=0;p<3;p++)if(inside(x,y,cx+7+p*49,cy+28,43,22))send(PowerStationSession.ASSIGN,i,p,0);}
            if(inside(x,y,280,306,180,29))send(PowerStationSession.ENERGIZE,0,0,0);}
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging>=0&&state!=null&&!handbook){Transform t=transform();double x=t.localX(e.x()),y=t.localY(e.y());for(int p=0;p<3;p++)if(inside(x,y,37,132+p*58,164,47))send(PowerStationSession.ASSIGN,dragging,p,0);}dragging=-1;return true;}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return true;}
    @Override public boolean charTyped(CharacterEvent e){if(!handbook&&state!=null&&state.stage()==0&&(Character.isDigit(e.codepoint())||e.codepoint()=='.')){if(current.isFocused())current.charTyped(e);else if(apparent.isFocused())apparent.charTyped(e);}return true;}
    @Override public boolean keyPressed(KeyEvent e){if(e.key()==InputConstants.KEY_ESCAPE){if(handbook)handbook=false;else onClose();return true;}
        if(handbook){if(e.key()==InputConstants.KEY_LEFT)bookPage=(bookPage+3)%4;if(e.key()==InputConstants.KEY_RIGHT)bookPage=(bookPage+1)%4;if(e.key()==InputConstants.KEY_RETURN)handbook=false;return true;}
        if(state==null)return true;
        if(state.stage()==0){if(e.key()==InputConstants.KEY_TAB){boolean was=current.isFocused();current.setFocused(!was);apparent.setFocused(was);}else if(e.key()==InputConstants.KEY_RETURN)checkCalculation();else if(current.isFocused())current.keyPressed(e);else if(apparent.isFocused())apparent.keyPressed(e);}
        else if(state.stage()==2){if(e.key()==InputConstants.KEY_LEFT||e.key()==InputConstants.KEY_RIGHT)adjust(e.key()==InputConstants.KEY_RIGHT?1:-1,e.hasShiftDown());if(e.key()==InputConstants.KEY_TAB)selectedKnob=1-selectedKnob;if(e.key()==InputConstants.KEY_RETURN)send(PowerStationSession.CLOSE_BREAKER,0,0,0);}
        else if(e.key()==InputConstants.KEY_RETURN&&state.stage()<4)send(state.stage()==1?PowerStationSession.CHECK:PowerStationSession.ENERGIZE,0,0,0);
        return true;
    }
    @Override public boolean keyReleased(KeyEvent e){return true;}
    private void checkCalculation(){try{double a=Double.parseDouble(current.getValue()),b=Double.parseDouble(apparent.getValue());if(!Double.isFinite(a)||!Double.isFinite(b)||a<0||b<0||a>100_000||b>100_000)throw new NumberFormatException();send(PowerStationSession.CHECK,capacity,a,b);}catch(NumberFormatException e){localError=true;}}
    private void adjust(int direction,boolean coarse){if(selectedKnob==0)send(PowerStationSession.VOLTAGE,0,Math.clamp(state.generatorVoltage()+direction*(coarse?5:1),state.voltage()*.8,state.voltage()*1.2),0);
        else send(PowerStationSession.FREQUENCY,0,Math.clamp(Math.rint((state.frequency()+direction*(coarse?.1:.01))*100)/100,49,51),0);}
    private void send(int action,int item,double a,double b){if(ClientPlayNetworking.canSend(PowerStationPackets.Input.TYPE))ClientPlayNetworking.send(new PowerStationPackets.Input(open.sessionId(),sequence++,action,item,a,b));}
    private void lifecycle(int action){if(ClientPlayNetworking.canSend(TaskPackets.Action.TYPE))ClientPlayNetworking.send(new TaskPackets.Action(open.sessionId(),sequence++,action,-1,0,0,0));}
    @Override public void onClose(){cancel();minecraft.setScreenAndShow(null);}
    @Override public void removed(){cancel();}
    private void cancel(){if(!closing){closing=true;lifecycle(TaskSession.CANCEL);}}
    public void closeFromServer(){closing=true;if(minecraft!=null&&minecraft.gui.screen()==this)minecraft.setScreenAndShow(null);}
    private Component text(String key,String fallback,Object... args){return Component.translatableWithFallback("task.goosetools.powerstation."+key,fallback,args);}
    private void center(GuiGraphicsExtractor g,Component text,int x,int y,int ink){g.text(font,text,x-font.width(text)/2,y,ink,false);}
    private void wrapped(GuiGraphicsExtractor g,Component text,int x,int y,int width,int ink){for(var line:font.split(text,width)){g.text(font,line,x,y,ink,false);y+=12;}}
    private void button(GuiGraphicsExtractor g,int x,int y,int w,int h,Component text,double mx,double my,boolean selected){panel(g,x,y,w,h,selected?0xFF3C7664:inside(mx,my,x,y,w,h)?0xFF45606A:0xFF304751);center(g,text,x+w/2,y+(h-8)/2,INK);}
    private static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h,int color){g.fill(x,y,x+w,y+h,color);g.outline(x,y,w,h,0xFF4C636A);}
    private static void circle(GuiGraphicsExtractor g,int x,int y,int r,int color){for(int a=-r;a<=r;a++){int half=(int)Math.sqrt(r*r-a*a);g.fill(x-half,y+a,x+half+1,y+a+1,color);}}
    private static void line(GuiGraphicsExtractor g,int x,int y,int xx,int yy,int color){int steps=Math.max(Math.abs(x-xx),Math.abs(y-yy));for(int i=0;i<=steps;i++){double t=steps==0?0:i/(double)steps;int dx=(int)(x+(xx-x)*t),dy=(int)(y+(yy-y)*t);g.fill(dx-1,dy-1,dx+2,dy+2,color);}}
    private static boolean inside(double x,double y,int a,int b,int w,int h){return x>=a&&x<a+w&&y>=b&&y<b+h;}
    private static String n(double value,int decimals){return String.format(Locale.ROOT,"%."+decimals+"f",value);}
    private static long now(){return System.nanoTime()/1_000_000;}
    public Transform transform(){double s=Math.max(.1,Math.min(1.6,Math.min((width-20.0)/600,(height-20.0)/430)));return new Transform((width-600*s)/2,(height-430*s)/2,s);}
    public record Transform(double x,double y,double scale){public double localX(double x){return (x-this.x)/scale;}public double localY(double y){return (y-this.y)/scale;}}
}
