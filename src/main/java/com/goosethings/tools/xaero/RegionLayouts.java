package com.goosethings.tools.xaero;
import com.goosethings.tools.client.regions.RegionClient;
import com.goosethings.tools.regions.*;
import java.util.*;
final class RegionLayouts {
    static List<GgdMapElementRenderer.Layout> apply(List<GgdMapElementRenderer.Layout> base,String dimension){
        var catalog=RegionClient.catalog();if(catalog==null)return base;
        Map<String,List<GgdMapElementRenderer.Room>> rooms=new LinkedHashMap<>();Set<String> known=new HashSet<>();
        for(var layout:base){var out=new ArrayList<GgdMapElementRenderer.Room>();rooms.put(layout.id(),out);
            for(var old:layout.rooms()){
                String id=layout.id()+"/"+old.key().substring(old.key().lastIndexOf('.')+1);known.add(id);
                if(catalog.deleted().contains(id))continue;var r=catalog.find(id);
                out.add(r!=null&&(r.revision()>0||r.hiddenOnMap()||!r.volumes().isEmpty())&&r.dimension().equals(dimension)?convert(r,old):old);
            }
        }
        for(var r:catalog.regions())if(r.revision()>0&&!known.contains(r.id())&&!r.binding().startsWith("ambience:")&&r.dimension().equals(dimension))
            rooms.computeIfAbsent(r.map(),id->new ArrayList<>()).add(convert(r,null));
        List<GgdMapElementRenderer.Layout> result=new ArrayList<>();
        for(var e:rooms.entrySet()){
            var original=base.stream().filter(l->l.id().equals(e.getKey())).findFirst().orElse(null);
            double minX=original==null?Double.POSITIVE_INFINITY:original.minX(),maxX=original==null?Double.NEGATIVE_INFINITY:original.maxX(),minZ=original==null?Double.POSITIVE_INFINITY:original.minZ(),maxZ=original==null?Double.NEGATIVE_INFINITY:original.maxZ();
            for(var room:e.getValue()){minX=Math.min(minX,room.x()-1);maxX=Math.max(maxX,room.x()+1);minZ=Math.min(minZ,room.z()-1);maxZ=Math.max(maxZ,room.z()+1);for(var a:room.areas()){minX=Math.min(minX,a.minX());maxX=Math.max(maxX,a.maxX());minZ=Math.min(minZ,a.minZ());maxZ=Math.max(maxZ,a.maxZ());}}
            if(Double.isFinite(minX))result.add(new GgdMapElementRenderer.Layout(e.getKey(),minX,maxX,minZ,maxZ,List.copyOf(e.getValue())));
        }return List.copyOf(result);
    }
    private static GgdMapElementRenderer.Room convert(RegionDefinition r,GgdMapElementRenderer.Room old){
        var areas=r.volumes().stream().map(v->new GgdMapElementRenderer.Area(v.minX(),v.maxX(),v.minY(),v.maxY(),v.minZ(),v.maxZ())).toList();
        return new GgdMapElementRenderer.Room("ggd-region:"+r.id(),r.labelX(),r.labelZ(),areas,List.of(),old!=null&&old.advancedOnly(),old!=null&&old.basicOnly(),old==null?"":old.stackGroup());
    }
}
