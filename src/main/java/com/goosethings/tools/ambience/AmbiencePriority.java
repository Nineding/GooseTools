package com.goosethings.tools.ambience;

/** Shared classification; no category-wide stop commands, including the bomb-tick ambient channel. */
public final class AmbiencePriority {
    private AmbiencePriority() {}
    public static boolean background(String namespace,String path,boolean musicCategory){
        return musicCategory || namespace.equals("minecraft") && (path.startsWith("ggd/events/")
                ||path.equals("ggd/flacon_pelican_time_bgm")||path.equals("ggd/duck_urgent")||path.equals("ggd/siren"));
    }
    public static boolean taskNote(String namespace,String path){return namespace.equals("minecraft")&&path.startsWith("block.note_block.");}
}
