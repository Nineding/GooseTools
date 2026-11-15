package com.goosethings.tools.client.regions;
import com.goosethings.tools.regions.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.network.chat.Component;
/** Read-only snapshots; no editing UI, world writes, or selection tool in GooseTools. */
public final class RegionClient {
    private static RegionCatalog catalog;private static long generation;
    private RegionClient(){}
    public static void register(){
        RegionAudioProfiles.register();
        ClientPlayNetworking.registerGlobalReceiver(RegionPackets.Catalog.TYPE,(p,c)->{try{catalog=RegionCatalog.parse(p.json());generation++;}catch(RuntimeException e){com.goosethings.tools.GooseTools.LOGGER.warn("Rejected region data",e);}});
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{catalog=null;generation++;});
    }
    public static RegionCatalog catalog(){return catalog;}
    public static long generation(){return generation;}
    public static RegionDefinition definition(String key){return catalog!=null&&key.startsWith("ggd-region:")?catalog.find(key.substring(11)):null;}
    public static Component label(String key){var r=definition(key);return r==null?Component.translatable(key):r.name().isEmpty()?Component.translatable(r.translationKey()):Component.literal(r.name());}
    public static Boolean ambience(String id,String dimension,double x,double y,double z,double grace){
        if(catalog==null)return null;String key="goosechapel/"+(id.equals("tavern")?"old_tavern":id);
        if(catalog.deleted().contains(key))return false;var r=catalog.find(key);
        return r==null||r.revision()==0||!r.dimension().equals(dimension)?null:r.contains(x,y,z,grace);
    }
}
