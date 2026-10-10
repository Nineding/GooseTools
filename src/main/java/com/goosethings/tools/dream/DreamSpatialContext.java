package com.goosethings.tools.dream;
import net.minecraft.commands.execution.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import java.util.List;
import java.util.function.Supplier;
/** A gameplay function lens. Entity simulation and meeting functions always use actual coordinates. */
public final class DreamSpatialContext {
 private static final ThreadLocal<Boolean> CURRENT=new ThreadLocal<>();
 private DreamSpatialContext() {}
 public static boolean enabled(){return Boolean.TRUE.equals(CURRENT.get());}
 public static Entity actor(Entity owner){return enabled()?DreamAvatarServer.actor(owner):null;}
 public static <T> T run(boolean active,Supplier<T> operation){Boolean previous=CURRENT.get();CURRENT.set(active);try{return operation.get();}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}}
 public static boolean gameplay(Identifier id){String p=id.getPath();if(id.getNamespace().equals("lobby"))return p.equals("achievement/check_cuckoo_egg_dream");if(!id.getNamespace().equals("ggd"))return false;
  return !(p.startsWith("talker/")||p.startsWith("session/")||p.startsWith("gameendevents/")||p.startsWith("role_extensions/")||p.startsWith("deadveent/")||p.startsWith("skill/dream/enter")||p.startsWith("skill/dream/wake")||p.startsWith("skill/dream/meeting")||p.startsWith("skill/dream/death/"));}
 public static <T> List<UnboundEntryAction<T>> functions(List<UnboundEntryAction<T>> entries,Identifier id){boolean active=gameplay(id);return entries.stream().<UnboundEntryAction<T>>map(a->(s,c,f)->run(active,()->{a.execute(active?origin(s):s,c,f);return null;})).toList();}
 @SuppressWarnings("unchecked")
 private static <T> T origin(T source) {
  if(!(source instanceof net.minecraft.commands.CommandSourceStack stack)
          || !(stack.getEntity() instanceof net.minecraft.server.level.ServerPlayer player))return source;
  Entity body=DreamAvatarServer.actor(player);
  if(body==null)return source;
  boolean originalOrigin=run(false,()->stack.getLevel()==player.level() && stack.getPosition().distanceToSqr(player.position())<1E-10);
  if(!originalOrigin)return source; // Keep explicit positioned/rotated command origins.
  return (T)stack.withLevel((net.minecraft.server.level.ServerLevel)body.level())
          .withPosition(body.position()).withRotation(new net.minecraft.world.phys.Vec2(body.getXRot(),body.getYRot()));
 }
 public static <T> CommandQueueEntry<T> queued(CommandQueueEntry<T> e){Boolean active=CURRENT.get();return active==null||e.action() instanceof ScopedAction<?>?e:new CommandQueueEntry<>(e.frame(),new ScopedAction<>(e.action(),active));}
 private record ScopedAction<T>(EntryAction<T> action,boolean active) implements EntryAction<T>{public void execute(ExecutionContext<T> c,Frame f){run(active,()->{action.execute(c,f);return null;});}}
}
