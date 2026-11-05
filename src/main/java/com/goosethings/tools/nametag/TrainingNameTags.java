package com.goosethings.tools.nametag;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
final class TrainingNameTags {
 private static final Set<String> PRIVATE=Set.of("chamberlainWard","witchDoctorTarget","seagullWitchTarget","esperTarget","mimeMarked","mimeControlled","infected","sinEaterVictim","stalkerTarget","clownMarked","serialKillerTarget","bountyPairTarget","cupidMarked");
 static boolean training(ServerPlayer p){return p.entityTags().contains("ggdTraining");}
 static boolean same(ServerPlayer a,ServerPlayer b){return training(a)==training(b)&&(!training(a)||a.level()==b.level());}
 static Set<String> tags(ServerPlayer viewer,Set<String> tags){
  return tags(viewer.entityTags(),tags);
 }
 static Set<String> tags(Set<String> viewer,Set<String> tags){
  if(!viewer.contains("ggdTraining"))return tags;
  var result=new HashSet<>(tags);result.removeAll(PRIVATE);
  String owner=viewer.stream().filter(t->t.startsWith("training_owner_")).findFirst().orElse("");
  if(!owner.isEmpty())for(String base:PRIVATE)if(tags.contains("tr"+owner.substring(15)+"_"+base))result.add(base);
  return result;
 }
}
