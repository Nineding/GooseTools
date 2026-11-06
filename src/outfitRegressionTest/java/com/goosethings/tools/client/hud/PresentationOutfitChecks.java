package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.outfit.OutfitClient;
import com.goosethings.tools.client.animation.ResultDeathFade;
import com.goosethings.tools.result.MatchResultPayload;
import com.goosethings.tools.intro.RoleIntroScene;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.*;
import java.util.*;

/** Uses production GUI model class and render extraction with or without optional visual mods. */
public final class PresentationOutfitChecks {
 public static boolean highlight;
 private static MatchResultScreen previewScreen;
 public static void show(Minecraft mc){mc.gui.setScreen(previewScreen);}
 public static int visibleMode(){
  var state=new AvatarRenderState();
  MatchResultScreen.applyDeathVisibility(state,true);
  require(state.isInvisible&&state.isInvisibleToPlayer&&!((ResultDeathFade)state).goosetools$resultDeathFade(),"Visible outfits retain the existing head-and-coat death style");
  return 1;
 }
 public static int run(Minecraft mc)throws Exception {
  int count=0;
  var model=new MeetingAlertRemotePlayer(mc.level,new GameProfile(UUID.randomUUID(),"Preview"),()->mc.player.getSkin());model.setId(0x6a110042);
  model.tickCount=0;model.setPos(9000,100,9000);
  var state=(AvatarRenderState)mc.getEntityRenderDispatcher().getRenderer(model).createRenderState(model,1);
  require(state.showHat&&state.showJacket&&state.showLeftSleeve&&state.showRightSleeve&&state.showLeftPants&&state.showRightPants,"Detached GUI models retain all outer skin layers");count++;
  require(model.tickCount==(int)(mc.level.getGameTime()&0x3fffffff)&&model.tickCount>0,"Entity clock used by ETF blinking follows world time");count++;
  require(model.position().distanceTo(mc.getCameraEntity().position())<.001,"Preview skin-layer LOD uses camera-relative location");count++;
  require(!model.getUUID().equals(mc.player.getUUID()),"Preview retains separate UUID from live player");count++;
  var items=new ArrayList<ItemStack>();for(var slot:EquipmentSlot.values())items.add(slot.isArmor()?new ItemStack(Items.LEATHER_CHESTPLATE):ItemStack.EMPTY);
  var appearance=new MatchResultPayload.Player(mc.player.getUUID(),"DeadTest","Goose",0x77cc88,1,false,true,true,items);
  var screen=new MatchResultScreen(new MatchResultPayload(true,"good_elimination",List.of(appearance)));
  var factory=MatchResultScreen.class.getDeclaredMethod("model",MatchResultPayload.Player.class,int.class);factory.setAccessible(true);
  var dead=(net.minecraft.client.player.RemotePlayer)factory.invoke(screen,appearance,0);
  var deadState=(AvatarRenderState)mc.getEntityRenderDispatcher().getRenderer(dead).createRenderState(dead,1);
  MatchResultScreen.applyDeathVisibility(deadState,true);
  require(!deadState.isInvisible&&!deadState.isInvisibleToPlayer,"Hidden equipment does not erase dead result model");count++;
  require(((ResultDeathFade)deadState).goosetools$resultDeathFade(),"Hidden-outfit dead result enables the spatial opacity mask");count++;
  MatchResultScreen.applyDeathVisibility(deadState,false);
  require(!((ResultDeathFade)deadState).goosetools$resultDeathFade(),"Living result clears any previous death mask");count++;
  MatchResultScreen.applyDeathVisibility(deadState,true);
  require(deadState.headEquipment.isEmpty()&&deadState.chestEquipment.isEmpty(),"Full result corpse uses skin when debug outfits are hidden");count++;
  require(deadState.showHat&&deadState.showJacket,"Dead result retains outer skin details");count++;
  // Clothing-only explosion remains are armor stands, not player/avatar render states.
  var remains=new ArmorStand(mc.level,mc.player.getX(),mc.player.getY(),mc.player.getZ());remains.setId(0x6a110043);remains.setInvisible(true);
  remains.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.LEATHER_LEGGINGS));remains.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.LEATHER_BOOTS));
  var remainsState=mc.getEntityRenderDispatcher().extractEntity(remains,1);
  require(!(remainsState instanceof AvatarRenderState),"Bomb/Sin Eater remains never enter avatar equipment filtering");count++;
  require(remainsState instanceof HumanoidRenderState r&&!r.legsEquipment.isEmpty()&&!r.feetEquipment.isEmpty(),"Invisible explosion stand retains visible lower-body equipment");count++;
  require(OutfitClient.replace(remains,remainsState,1)==remainsState,"Explosion remains are not converted into a full undamaged body");count++;
  // Capture the actual production result screen for a visual sanity check next frame.
  // Matching default skins make both roster sizes easy to compare on the GPU capture.
  var roster=new ArrayList<MatchResultPayload.Player>();
  UUID first=new UUID(42,1);
  for(int candidate=1;roster.size()<4;candidate++){
   UUID id=new UUID(42,candidate);
   if(!net.minecraft.client.resources.DefaultPlayerSkin.get(first).equals(net.minecraft.client.resources.DefaultPlayerSkin.get(id)))continue;
   int index=roster.size();boolean deceased=index%2==1;
   roster.add(new MatchResultPayload.Player(id,deceased?"DeadTest":"AliveTest","Goose",0x77cc88,index+1,false,index>=2,deceased,items));
  }
  previewScreen=new MatchResultScreen(new MatchResultPayload(true,"good_elimination",roster));
  return count;
 }
 private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
