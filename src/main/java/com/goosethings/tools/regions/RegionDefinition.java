package com.goosethings.tools.regions;

import java.util.List;

public record RegionDefinition(String id,String map,String dimension,String name,String translationKey,
        String binding,double labelX,double labelZ,List<RegionVolume> volumes,List<RegionVolume> excluded,long revision,Audio audio,boolean hiddenOnMap) {
    public record Audio(String sound,float volume,boolean continuous,boolean custom,String repeatMode,
            int minDelaySeconds,int maxDelaySeconds,Boolean sharedProgress){
        public Audio(String sound,float volume,boolean continuous,boolean custom){this(sound,volume,continuous,custom,"loop",0,0,null);}
        public Audio {
            if(sound==null||sound.length()>160||!sound.isEmpty()&&!sound.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||!Float.isFinite(volume)||volume<0||volume>1)throw new IllegalArgumentException("audio");
            if(repeatMode==null)repeatMode="loop";
            if(!repeatMode.equals("loop")&&!repeatMode.equals("random_delay")||minDelaySeconds<0||maxDelaySeconds<minDelaySeconds||maxDelaySeconds>3600)throw new IllegalArgumentException("audio");
        }
        public boolean delayed(){return repeatMode.equals("random_delay");}
        // Old continuous profiles shared a clock implicitly; retain that behavior on migration.
        public boolean sharesProgress(){return sharedProgress==null?continuous:sharedProgress;}
        public int nextDelayTicks(java.util.random.RandomGenerator random){return random.nextInt(minDelaySeconds*20,maxDelaySeconds*20+1);}
        public boolean samePlayback(Audio other){return other!=null&&sound.equals(other.sound)&&continuous==other.continuous&&repeatMode.equals(other.repeatMode)&&minDelaySeconds==other.minDelaySeconds&&maxDelaySeconds==other.maxDelaySeconds&&sharesProgress()==other.sharesProgress();}
    }
    public RegionDefinition(String id,String map,String dimension,String name,String key,String binding,double x,double z,List<RegionVolume> volumes,List<RegionVolume> excluded,long revision,Audio audio){this(id,map,dimension,name,key,binding,x,z,volumes,excluded,revision,audio,false);}
    public RegionDefinition(String id,String map,String dimension,String name,String key,String binding,double x,double z,List<RegionVolume> volumes,List<RegionVolume> excluded,long revision){this(id,map,dimension,name,key,binding,x,z,volumes,excluded,revision,null);}
    public RegionDefinition {
        if(id==null||!id.matches("[a-z0-9_./-]{1,96}")||map==null||!map.matches("[a-z0-9_.-]{1,48}"))throw new IllegalArgumentException("id");
        if(dimension==null||!dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("dimension");
        if(name==null||name.length()>64||name.codePoints().anyMatch(Character::isISOControl)||translationKey==null||translationKey.length()>128||name.isBlank()&&translationKey.isBlank()||binding==null||binding.length()>128)throw new IllegalArgumentException("name");
        if(!Double.isFinite(labelX)||!Double.isFinite(labelZ)||Math.abs(labelX)>30_000_000||Math.abs(labelZ)>30_000_000||revision<0)throw new IllegalArgumentException("coordinate");
        volumes=List.copyOf(volumes);excluded=List.copyOf(excluded);
        if(volumes.size()>16||excluded.size()>16)throw new IllegalArgumentException("parts");
    }
    public boolean contains(double x,double y,double z,double grace){
        return volumes.stream().anyMatch(v->v.contains(x,y,z,grace))&&excluded.stream().noneMatch(v->v.contains(x,y,z,0));
    }
    public String displayName(){return name.isEmpty()?translationKey:name;}
}
