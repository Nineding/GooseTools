package com.goosethings.tools.regions;
/** Continuous instances with the same event in the same map share a single playback clock. */
public final class AmbienceClock {
    private AmbienceClock(){}
    public static String key(String map,String region,String sound,boolean continuous){return continuous?"continuous:"+map+":"+sound:region;}
    public static String key(String map,String region,RegionDefinition.Audio audio){
        String key=key(map,region,audio.sound(),audio.sharesProgress());
        // Incompatible replay settings must never steal another region's scheduling clock.
        return audio.delayed()?key+":random:"+audio.minDelaySeconds()+":"+audio.maxDelaySeconds():key;
    }
}
