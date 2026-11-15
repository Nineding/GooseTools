package com.goosethings.tools.regions;
import java.util.*;
import java.util.function.Predicate;
/** Explicit geometry wins. Only regions without geometry participate in nearest-anchor fallback. */
public final class RegionResolver {
    private RegionResolver(){}
    public static RegionDefinition resolve(List<RegionDefinition> regions,double x,double y,double z,Predicate<RegionDefinition> allowAnchor){
        RegionDefinition specific=null;double smallest=Double.POSITIVE_INFINITY;
        for(var r:regions)if(!r.volumes().isEmpty()&&r.contains(x,y,z,0)){
            double size=r.volumes().stream().filter(v->v.contains(x,y,z,0)).mapToDouble(v->(v.maxX()-v.minX())*(v.maxZ()-v.minZ())*(v.maxY()-v.minY())).min().orElse(Double.POSITIVE_INFINITY);
            if(size<smallest||size==smallest&&(specific==null||r.id().compareTo(specific.id())<0)){specific=r;smallest=size;}
        }
        if(specific!=null)return specific;
        RegionDefinition best=null;double distance=Double.POSITIVE_INFINITY;
        for(var r:regions){if(!r.volumes().isEmpty()||!allowAnchor.test(r)||r.excluded().stream().anyMatch(v->v.contains(x,y,z,0)))continue;
            double dx=r.labelX()-x,dz=r.labelZ()-z,d=dx*dx+dz*dz;
            if(d<distance){best=r;distance=d;}
        }return best;
    }
}
