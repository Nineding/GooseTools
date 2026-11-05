package com.goosethings.tools.client.outfit;
import com.mojang.authlib.properties.Property;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
/** Runs the production data-pack gates against the actual GooseThings skin service. */
final class ClassicDisguiseChecks {
 static boolean loaded(){return FabricLoader.getInstance().isModLoaded("goosethings");}
 static void prepare(ServerPlayer p)throws Exception {
  if(!loaded())return;
  var resolver=Class.forName("com.goosethings.skin.TextureResolver");
  String packed=Base64.getEncoder().encodeToString("{\"textures\":{}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
  resolver.getMethod("cache",UUID.class,String.class,List.class).invoke(null,p.getUUID(),p.getGameProfile().name(),List.of(new Property("textures",packed)));
  p.addTag("p2");p.addTag("StealP2");
 }
 static boolean overridden(ServerPlayer p)throws Exception {
  return (boolean)Class.forName("com.goosethings.skin.SkinManager").getMethod("hasOverride",UUID.class).invoke(null,p.getUUID());
 }
 static void clean(ServerPlayer p){p.removeTag("p2");p.removeTag("StealP2");}
}
