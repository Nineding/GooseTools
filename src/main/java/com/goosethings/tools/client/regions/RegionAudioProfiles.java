package com.goosethings.tools.client.regions;
import com.goosethings.tools.regions.*;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** The resource pack supplies sound defaults; no audio bytes or fixed event IDs in the engine. */
public final class RegionAudioProfiles {
    private static Map<String,RegionDefinition> profiles=Map.of();
    private static Map<String,double[]> mapBounds=Map.of();
    public static boolean insideMap(String map,double x,double z){var b=mapBounds.get(map);return b!=null&&x>=b[0]&&x<=b[1]&&z>=b[2]&&z<=b[3];}
    public static Map<String,RegionDefinition> profiles(){return profiles;}
    public static RegionDefinition.Audio effective(RegionDefinition r){
        if(r.audio()!=null&&r.audio().custom())return r.audio();
        var base=profiles.get(r.id());return base!=null&&base.audio()!=null?base.audio():r.audio();
    }
    public static void register(){ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.parse("goosetools:audio_profiles"),(ResourceManagerReloadListener)manager->{
        var next=new LinkedHashMap<String,RegionDefinition>();
        for(var e:manager.listResources("regions",id->id.getNamespace().equals("mapregioneditor")&&id.getPath().endsWith(".json")).entrySet()){
            try(var in=e.getValue().open()){var data=RegionCatalog.parse(new String(in.readNBytes(RegionCatalog.MAX_JSON*3+1),StandardCharsets.UTF_8));for(var r:data.regions())next.put(r.id(),r);}
            catch(Exception ignored){}
        }profiles=Map.copyOf(next);
        var boxes=new HashMap<String,double[]>();
        var resource=manager.getResource(Identifier.parse("mapregioneditor:map_bounds.json"));
        if(resource.isEmpty())resource=manager.getResource(Identifier.parse("goosetools:layouts.json"));
        if(resource.isPresent())try(var reader=resource.get().openAsReader()){
            var root=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();var maps=root.has("maps")?root.getAsJsonArray("maps"):root.getAsJsonArray("layouts");
            for(var entry:maps){var b=entry.getAsJsonObject();boxes.put(b.get("id").getAsString(),new double[]{b.get("minX").getAsDouble(),b.get("maxX").getAsDouble(),b.get("minZ").getAsDouble(),b.get("maxZ").getAsDouble()});}
        }catch(Exception ignored){}mapBounds=Map.copyOf(boxes);
    });}
}
