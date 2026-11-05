package com.goosethings.tools.client.outfit;
import com.goosethings.tools.outfit.OutfitSnapshot;
import com.goosethings.tools.client.hud.MeetingAlertTrueSkinCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.PlayerSkin;
import java.util.*;
/** Keeps the cape texture visible while checking frame-fraction and world-position invariance. */
final class CorpseCapeChecks {
 @SuppressWarnings("unchecked") static int run(Minecraft mc)throws Exception{
  var cacheField=MeetingAlertTrueSkinCache.class.getDeclaredField("SKINS");cacheField.setAccessible(true);
  var skins=(Map<UUID,PlayerSkin>)cacheField.get(null);
  var corpsesField=OutfitClient.class.getDeclaredField("corpses");corpsesField.setAccessible(true);
  var previous=List.copyOf(((Map<UUID,OutfitSnapshot.Corpse>)corpsesField.get(null)).values());
  UUID owner=UUID.randomUUID();var skin=mc.player.getSkin();skins.put(owner,new PlayerSkin(skin.body(),skin.body(),null,skin.model(),true));
  var stand=new ArmorStand(mc.level,mc.player.getX()+3,mc.player.getY(),mc.player.getZ());stand.setId(0x6a110053);
  var rows=new ArrayList<>(previous);rows.add(new OutfitSnapshot.Corpse(stand.getUUID(),owner,"CapeVictim"));OutfitClient.apply(new OutfitSnapshot(false,rows));
  int checks=0;
  try{
   for(float rotation:new float[]{0,90,180})for(float partial:new float[]{0,.15f,.5f,.85f,1}){
    stand.setYRot(rotation);stand.setPos(mc.player.getX()+3+rotation/90,mc.player.getY()+rotation/90,mc.player.getZ());
    var raw=mc.getEntityRenderDispatcher().extractEntity(stand,partial);
    require(raw instanceof AvatarRenderState,"Corpse keeps the full skin renderer");checks++;
    var state=(AvatarRenderState)raw;
    require(state.showCape&&state.skin.cape()!=null,"Cape remains visible rather than being removed");checks++;
    require(state.capeFlap==0&&state.capeLean==0&&state.capeLean2==0,"Static corpse cape does not twitch across frame fractions or rotations");checks++;
   }
   return checks;
  }finally{skins.remove(owner);OutfitClient.apply(new OutfitSnapshot(false,previous));}
 }
 static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
