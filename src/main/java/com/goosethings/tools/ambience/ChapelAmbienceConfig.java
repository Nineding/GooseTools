package com.goosethings.tools.ambience;

import com.google.gson.*;
import java.util.*;

/** Resource-pack-owned room volumes, independent of map-label anchors and server authority. */
public record ChapelAmbienceConfig(int mapId, Set<String> dimensions, int fadeInTicks,
        int fadeOutTicks, int priorityFadeOutTicks, double borderGrace, List<Region> regions) {
    public ChapelAmbienceConfig { dimensions=Set.copyOf(dimensions);regions=List.copyOf(regions); }
    public record Area(double minX,double maxX,double minY,double maxY,double minZ,double maxZ) {
        public boolean contains(double x,double y,double z,double horizontalGrace){
            return x>=minX-horizontalGrace&&x<maxX+horizontalGrace&&y>=minY&&y<maxY
                    &&z>=minZ-horizontalGrace&&z<maxZ+horizontalGrace;
        }
    }
    public record Region(String id,String sound,float volume,boolean continuous,List<Area> areas) {
        public Region { areas=List.copyOf(areas); }
        public boolean contains(double x,double y,double z,double grace){return areas.stream().anyMatch(a->a.contains(x,y,z,grace));}
    }
    public Region select(String previous,String dimension,double x,double y,double z){
        if(!dimensions.contains(dimension))return null;
        if(previous!=null)for(var region:regions)if(region.id().equals(previous)&&region.contains(x,y,z,borderGrace))return region;
        return regions.stream().filter(r->r.contains(x,y,z,0)).findFirst().orElse(null);
    }
    public static ChapelAmbienceConfig parse(String json){
        if(json.length()>32768)throw new IllegalArgumentException("Ambience config exceeds 32 KiB");
        JsonObject root=JsonParser.parseString(json).getAsJsonObject();
        if(root.get("schema_version").getAsInt()!=1)throw new IllegalArgumentException("Unknown ambience schema");
        int map=integer(root,"map_id",0,64),fadeIn=integer(root,"fade_in_ticks",1,100),fadeOut=integer(root,"fade_out_ticks",1,100),priority=integer(root,"priority_fade_out_ticks",1,100);
        double grace=number(root,"border_grace_blocks",0,2);
        Set<String> dimensions=new LinkedHashSet<>();for(var d:root.getAsJsonArray("dimensions")){String value=d.getAsString();if(!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Invalid dimension");dimensions.add(value);}
        if(dimensions.isEmpty()||dimensions.size()>8)throw new IllegalArgumentException("Invalid dimension count");
        List<Region> regions=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(var element:root.getAsJsonArray("regions")){
            var r=element.getAsJsonObject();String id=r.get("id").getAsString(),sound=r.get("sound").getAsString();
            if(!id.matches("[a-z0-9_]{1,40}")||!ids.add(id)||!sound.matches("ggd:ambience/goosechapel/[a-z0-9_/.-]{1,80}"))throw new IllegalArgumentException("Invalid ambience region or sound");
            float volume=(float)number(r,"volume",0,1);List<Area> areas=new ArrayList<>();
            for(var box:r.getAsJsonArray("areas")){
                var b=box.getAsJsonObject();double x1=number(b,"min_x",-30000000,30000000),x2=number(b,"max_x",-30000000,30000000),y1=number(b,"min_y",-2048,2048),y2=number(b,"max_y",-2048,2048),z1=number(b,"min_z",-30000000,30000000),z2=number(b,"max_z",-30000000,30000000);
                if(x2<=x1||y2<=y1||z2<=z1||x2-x1>512||z2-z1>512)throw new IllegalArgumentException("Invalid ambience area");
                areas.add(new Area(x1,x2,y1,y2,z1,z2));
            }
            if(areas.isEmpty()||areas.size()>16)throw new IllegalArgumentException("Invalid ambience area count");
            regions.add(new Region(id,sound,volume,r.has("continuous")&&r.get("continuous").getAsBoolean(),areas));
        }
        if(regions.isEmpty()||regions.size()>16)throw new IllegalArgumentException("Invalid ambience region count");
        return new ChapelAmbienceConfig(map,dimensions,fadeIn,fadeOut,priority,grace,regions);
    }
    private static double number(JsonObject json,String key,double min,double max){double value=json.get(key).getAsDouble();if(!Double.isFinite(value)||value<min||value>max)throw new IllegalArgumentException("Invalid "+key);return value;}
    private static int integer(JsonObject json,String key,int min,int max){double value=number(json,key,min,max);if(value!=Math.rint(value))throw new IllegalArgumentException("Invalid integer "+key);return (int)value;}
}
