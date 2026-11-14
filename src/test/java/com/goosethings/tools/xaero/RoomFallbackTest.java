package com.goosethings.tools.xaero;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RoomFallbackTest {
    @Test void onlyAnchorOnlyRoomsCanWinFallback(){
        var bounded=new GgdMapElementRenderer.Room("bounded",0,0,List.of(new GgdMapElementRenderer.Area(10,20,0,5,10,20)),List.of(),false,false,"");
        var anchor=new GgdMapElementRenderer.Room("anchor",100,0,List.of(),List.of(),false,false,"");
        var layout=new GgdMapElementRenderer.Layout("test",-10,200,-10,200,List.of(bounded,anchor));
        assertSame(anchor,layout.currentRoom(0,1,0,layout.rooms()));
        assertSame(bounded,layout.currentRoom(12,1,12,layout.rooms()));
        assertNull(layout.currentRoom(0,1,0,List.of(bounded)));
    }
}
