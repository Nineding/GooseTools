package com.goosethings.tools.nametag;

import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.Method;

/** Optional authoritative pet state; personal maps and older server mods remain unshifted. */
public final class MeetingPetBridge {
    private static final Method HEADROOM=find("meetingHeadroom"),OFFSET=find("meetingLabelOffset");
    private MeetingPetBridge() {}
    private static Method find(String name){
        try{return Class.forName("com.goosethings.pet.PetManager").getMethod(name,ServerPlayer.class);}
        catch(ReflectiveOperationException unavailable){return null;}
    }
    public static int flags(ServerPlayer owner){
        if(HEADROOM!=null)try{return ((Number)HEADROOM.invoke(null,owner)).doubleValue()>0?MeetingPetLayout.flags(OFFSET==null?.12:((Number)OFFSET.invoke(null,owner)).doubleValue()):0;}
        catch(ReflectiveOperationException unavailable){return 0;}
        return 0;
    }
}
