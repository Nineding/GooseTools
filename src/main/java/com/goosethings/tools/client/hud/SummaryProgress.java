package com.goosethings.tools.client.hud;

import com.goosethings.tools.result.SummaryPayload;
import java.util.List;

public record SummaryProgress(int level, double xp, int needed, int received, boolean complete) {
    public static SummaryProgress at(SummaryPayload p,double fraction) {
        return at(p.startLevel(),p.startXp(),p.endLevel(),p.endXp(),p.gainedXp(),p.bankGain(),p.thresholds(),fraction);
    }
    public static SummaryProgress at(int startLevel,int startXp,int endLevel,int endXp,int gainedXp,List<Integer> thresholds,double fraction) {
        return at(startLevel,startXp,endLevel,endXp,gainedXp,0,thresholds,fraction);
    }
    public static SummaryProgress at(int startLevel,int startXp,int endLevel,int endXp,int gainedXp,int bankGain,List<Integer> thresholds,double fraction) {
        double f=Math.clamp(fraction,0,1);
        if(f>=1) return new SummaryProgress(endLevel,endXp,thresholds.getLast(),gainedXp,true);
        // At the cap the saved XP bar wraps with the bank, without awarding another level.
        long path=bankGain>0?bankGain:Math.max(0,endXp-startXp);
        if(endLevel>startLevel) {
            path=-((long)startXp)+(bankGain>0?bankGain:endXp);
            for(int i=0;i<thresholds.size()-1;i++) path+=thresholds.get(i);
        }
        double value=startXp+Math.max(0,path)*f;
        int offset=0;
        while(offset<thresholds.size()-1 && value>=thresholds.get(offset)) value-=thresholds.get(offset++);
        if(bankGain>0 && offset==thresholds.size()-1 && value>thresholds.get(offset)) {
            value%=thresholds.get(offset);
            if(value==0) value=thresholds.get(offset);
        }
        return new SummaryProgress(startLevel+offset,value,thresholds.get(offset),(int)Math.floor(gainedXp*f),false);
    }
}
