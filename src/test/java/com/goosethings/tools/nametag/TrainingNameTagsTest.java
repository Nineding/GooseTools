package com.goosethings.tools.nametag;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class TrainingNameTagsTest {
 @Test void duplicateRolesOnlySeeTheirOwnTargetMarks(){
  var target=Set.of("players","chamberlainWard","witchDoctorTarget","tr7_chamberlainWard","tr8_witchDoctorTarget");
  var a=TrainingNameTags.tags(Set.of("ggdTraining","training_owner_7","Chamberlain"),target);
  var b=TrainingNameTags.tags(Set.of("ggdTraining","training_owner_8","WitchDoctor"),target);
  assertTrue(NameTagAttachmentPolicy.showChamberlainWard(false,Set.of("players","Chamberlain"),a,true));
  assertFalse(NameTagAttachmentPolicy.showWitchDoctorCurse(true,a));
  assertFalse(NameTagAttachmentPolicy.showChamberlainWard(false,Set.of("players","Chamberlain"),b,true));
  assertTrue(NameTagAttachmentPolicy.showWitchDoctorCurse(true,b));
  var other=TrainingNameTags.tags(Set.of("ggdTraining","training_owner_9"),target);
  assertFalse(other.contains("chamberlainWard"));assertFalse(other.contains("witchDoctorTarget"));
 }
 @Test void formalMarksRemainUnchanged(){
  var tags=Set.of("players","witchDoctorTarget");assertEquals(tags,TrainingNameTags.tags(Set.of("players"),tags));
 }
}
