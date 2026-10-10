package com.goosethings.tools.task;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class CutWiresSessionTest {
    private final TaskSession s = new TaskSession(1, TaskType.CUTWIRES, 42, 0);
    private int seq; private long now;
    private boolean input(int action, double x, double y) { now += 50; return s.apply(seq++, action, 0, x, y, now, now, 0); }
    private void ready() { assertTrue(s.apply(seq++, TaskSession.READY, -1, 0, 0, 0, 0, 0)); }
    private void cut(int wire, double x) { input(TaskSession.BEGIN,x,CutWiresLayout.y(wire)-10); input(TaskSession.END,x,CutWiresLayout.y(wire)+10); }
    @Test void allFourAndDuplicate() { ready(); cut(0,210); cut(0,210); assertEquals(1,s.progress()); for(int i=1;i<4;i++)cut(i,210); assertTrue(s.complete()); assertEquals(15,s.mask()); }
    @Test void outsideCutZoneAndMoveWithoutBegin() { ready(); input(TaskSession.END,210,125); cut(0,100); assertEquals(0,s.progress()); }
    @Test void giantJumpCannotCutAll() { ready(); input(TaskSession.BEGIN,210,90); input(TaskSession.END,210,250); assertEquals(0,s.progress()); }
    @Test void waitingAndDuplicateSequenceRejected() { assertFalse(input(TaskSession.END,210,120)); ready(); cut(0,210); assertFalse(s.apply(seq-1,TaskSession.END,0,210,165,now,now,0)); assertEquals(1,s.progress()); }
    @Test void independentPlayers() { ready(); cut(0,210); TaskSession other=new TaskSession(2,TaskType.CUTWIRES,42,0); assertEquals(0,other.progress()); assertFalse(other.complete()); }
}
