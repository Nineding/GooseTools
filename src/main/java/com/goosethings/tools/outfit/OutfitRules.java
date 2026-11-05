package com.goosethings.tools.outfit;

import java.util.Set;

public final class OutfitRules {
    private static final String[] SEATS = {"one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen","twenty","twentyone","twentytwo","twentythree","twentyfour"};
    private OutfitRules() {}
    public static boolean enabled(int hide) { return hide != 1; }
    /** Stable pN identity is independent of the rotating meeting seat. */
    public static int playerIndex(Set<String> tags) {
        for (int i=1;i<=SEATS.length;i++) if(tags.contains("p"+i)) return i;
        return 0;
    }
    public static boolean occupantMatches(Set<String> corpseTags, Set<String> playerTags) {
        int index=playerIndex(corpseTags);
        if(index!=0)return playerTags.contains("p"+index);
        // Legacy corpses and Debugger spectators may only carry a meeting-seat tag.
        if(corpseTags.contains("host"))return playerTags.contains("host");
        for(int i=0;i<SEATS.length;i++) if(corpseTags.contains(SEATS[i])&&playerTags.contains(Integer.toString(i+1)))return true;
        return false;
    }
    public static int corpseSeat(Set<String> tags) {
        for(int i=0;i<SEATS.length;i++) if(tags.contains(SEATS[i])||tags.contains("p"+(i+1))) return i+1;
        return 0;
    }
    public static boolean meetingCorpse(Set<String> tags) {
        return tags.contains("talk") && (tags.contains("dead")||tags.contains("ggd.meetingShotCorpse")||tags.contains("sitting_body"));
    }
}
