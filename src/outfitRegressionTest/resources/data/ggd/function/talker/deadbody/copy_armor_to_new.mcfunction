# Keep the fixed owner on the corpse; host/one/etc. are meeting positions, not pN identities.
execute if entity @s[tag=p1] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p1
execute if entity @s[tag=p2] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p2
execute if entity @s[tag=p3] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p3
execute if entity @s[tag=p4] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p4
execute if entity @s[tag=p5] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p5
execute if entity @s[tag=p6] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p6
execute if entity @s[tag=p7] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p7
execute if entity @s[tag=p8] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p8
execute if entity @s[tag=p9] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p9
execute if entity @s[tag=p10] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p10
execute if entity @s[tag=p11] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p11
execute if entity @s[tag=p12] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p12
execute if entity @s[tag=p13] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p13
execute if entity @s[tag=p14] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p14
execute if entity @s[tag=p15] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p15
execute if entity @s[tag=p16] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p16
execute if entity @s[tag=p17] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p17
execute if entity @s[tag=p18] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p18
execute if entity @s[tag=p19] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p19
execute if entity @s[tag=p20] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p20
execute if entity @s[tag=p21] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p21
execute if entity @s[tag=p22] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p22
execute if entity @s[tag=p23] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p23
execute if entity @s[tag=p24] run tag @e[type=armor_stand,tag=newTalkDead,limit=1] add p24
# 将 cloneArmorStand 装备复制到 @e[tag=newTalkDead]
# 执行者 = 死者玩家（持 p1..p20）
execute if entity @s[tag=p1] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p1,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p1] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p1,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p1] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p1,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p1] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p1,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p2] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p2,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p2] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p2,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p2] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p2,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p2] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p2,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p3] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p3,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p3] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p3,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p3] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p3,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p3] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p3,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p4] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p4,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p4] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p4,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p4] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p4,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p4] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p4,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p5] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p5,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p5] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p5,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p5] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p5,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p5] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p5,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p6] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p6,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p6] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p6,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p6] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p6,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p6] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p6,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p7] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p7,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p7] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p7,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p7] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p7,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p7] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p7,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p8] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p8,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p8] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p8,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p8] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p8,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p8] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p8,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p9] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p9,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p9] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p9,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p9] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p9,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p9] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p9,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p10] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p10,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p10] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p10,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p10] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p10,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p10] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p10,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p11] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p11,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p11] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p11,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p11] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p11,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p11] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p11,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p12] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p12,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p12] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p12,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p12] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p12,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p12] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p12,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p13] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p13,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p13] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p13,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p13] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p13,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p13] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p13,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p14] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p14,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p14] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p14,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p14] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p14,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p14] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p14,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p15] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p15,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p15] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p15,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p15] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p15,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p15] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p15,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p16] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p16,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p17] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p17,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p18] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p18,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p19] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p19,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p20] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p20,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p21] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @e[type=armor_stand,tag=p21,tag=cloneArmorStand,limit=1] armor.head
execute if entity @s[tag=p16] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p16,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p17] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p17,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p18] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p18,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p19] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p19,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p20] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p20,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p21] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @e[type=armor_stand,tag=p21,tag=cloneArmorStand,limit=1] armor.chest
execute if entity @s[tag=p16] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p16,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p17] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p17,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p18] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p18,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p19] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p19,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p20] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p20,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p21] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @e[type=armor_stand,tag=p21,tag=cloneArmorStand,limit=1] armor.legs
execute if entity @s[tag=p16] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p16,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p17] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p17,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p18] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p18,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p19] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p19,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p20] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p20,tag=cloneArmorStand,limit=1] armor.feet
execute if entity @s[tag=p21] run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @e[type=armor_stand,tag=p21,tag=cloneArmorStand,limit=1] armor.feet

# 若玩家身上仍有装备则覆盖（clone 为空时的兜底；会议中通常已 clear 不会覆盖）
execute if items entity @s armor.head * run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.head from entity @s armor.head
execute if items entity @s armor.chest * run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.chest from entity @s armor.chest
execute if items entity @s armor.legs * run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.legs from entity @s armor.legs
execute if items entity @s armor.feet * run item replace entity @e[type=armor_stand,tag=newTalkDead,limit=1] armor.feet from entity @s armor.feet

# Meeting corpses retain their handle until deadbody/begin; vote scenes keep the original cleanup.
tag @e[type=armor_stand,tag=newTalkDead,tag=!ggd.meetingShotCorpse] remove newTalkDead
