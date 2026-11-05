package com.goosethings.tools.client.outfit;
import com.goosethings.tools.client.dream.DreamStandInClient;
import com.goosethings.tools.client.projection.ProjectionBodyClient;
import com.goosethings.tools.network.GooseToolsPayloads.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import java.util.*;
/** Production scene reconstruction and final render extraction, rather than a second implementation of the filter. */
public final class AppearanceOutfitChecks {
 public static int run(Minecraft mc)throws Exception {
  int checks=0;
  var equipment=new ArrayList<ItemStack>();for(var slot:EquipmentSlot.values())equipment.add(slot==EquipmentSlot.HEAD?new ItemStack(Items.PLAYER_HEAD):slot.isArmor()?new ItemStack(Items.LEATHER_CHESTPLATE):new ItemStack(Items.IRON_SWORD));
  var dreamApply=DreamStandInClient.class.getDeclaredMethod("applyScene",DreamSceneS2C.class);dreamApply.setAccessible(true);
  var dreamTick=DreamStandInClient.class.getDeclaredMethod("tick",Minecraft.class);dreamTick.setAccessible(true);
  var source=UUID.randomUUID();long revision=10000;
  // In lucid dreams, an unrelated source is deliberately rendered with the viewer's skin.
  // Hiding armor must preserve that selected identity, including the live Raven avatar.
  for(int kind:new int[]{DreamStandIn.MAP_BODY,DreamStandIn.LIVE_AVATAR,DreamStandIn.DREAM_CORPSE,DreamStandIn.MEETING_PROXY}){
   var fake=UUID.randomUUID();var row=new DreamStandIn(fake,source,mc.player.getUUID(),"MaskedSource",mc.level.dimension().identifier().toString(),kind,false,false,mc.player.getX()+1,mc.player.getY(),mc.player.getZ(),0,0,0,0,"STANDING",equipment);
   dreamApply.invoke(null,new DreamSceneS2C(++revision,List.of(row)));dreamTick.invoke(null,mc);
   var entity=find(mc,fake);require(entity!=null,"Dream model exists for kind "+kind);checks++;
   var rendered=(AvatarRenderState)mc.getEntityRenderDispatcher().extractEntity(entity,1);
   require(rendered.skin.equals(mc.player.getSkin()),"Dream privacy-selected skin survives hidden outfits for kind "+kind);checks++;
   require(rendered.headEquipment.isEmpty()&&rendered.chestEquipment.isEmpty()&&rendered.headItem.isEmpty()&&rendered.wornHeadType==null,"Finalized dream model cannot restore hidden head or armor "+kind);checks++;
   require(!((net.minecraft.world.entity.LivingEntity)entity).getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(),"Dream tool and skill items remain available "+kind);checks++;
  }
  dreamApply.invoke(null,new DreamSceneS2C(++revision,List.of()));
  var projectionApply=ProjectionBodyClient.class.getDeclaredMethod("applyScene",ProjectionBodiesS2C.class);projectionApply.setAccessible(true);
  var projectionTick=ProjectionBodyClient.class.getDeclaredMethod("tick",Minecraft.class);projectionTick.setAccessible(true);
  for(int kind:new int[]{ProjectionBody.ASTRAL,ProjectionBody.ESPER,ProjectionBody.SNIPER,ProjectionBody.MIME}){
   var fake=UUID.randomUUID();var row=new ProjectionBody(UUID.randomUUID(),fake,"RetainedBody",mc.level.dimension().identifier().toString(),kind,ProjectionBody.ACTIVE,false,mc.player.getX()+1,mc.player.getY(),mc.player.getZ(),0,0,0,0,"STANDING",equipment);
   projectionApply.invoke(null,new ProjectionBodiesS2C(++revision,List.of(row)));projectionTick.invoke(null,mc);
   var entity=find(mc,fake);require(entity!=null,"Projection body exists for kind "+kind);checks++;
   var rendered=(AvatarRenderState)mc.getEntityRenderDispatcher().extractEntity(entity,1);
   require(rendered.headEquipment.isEmpty()&&rendered.chestEquipment.isEmpty()&&rendered.wornHeadType==null,"Finalized projection body cannot restore hidden head or armor "+kind);checks++;
  }
  projectionApply.invoke(null,new ProjectionBodiesS2C(++revision,List.of()));
  return checks;
 }
 private static net.minecraft.world.entity.Entity find(Minecraft mc,UUID id){for(var e:mc.level.entitiesForRendering())if(e.getUUID().equals(id))return e;return null;}
 private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
